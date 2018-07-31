/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.deployment;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.web.session.SharedSessionManagerConfig;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.container.SessionManagerFactoryConfiguration;
import org.wildfly.extension.undertow.session.NonDistributableSessionManagementProvider;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

import io.undertow.server.session.InMemorySessionManager;
import io.undertow.servlet.api.SessionManagerFactory;

/**
 * @author Stuart Douglas
 */
public class SharedSessionManagerDeploymentProcessor implements DeploymentUnitProcessor, Function<SessionManagerFactoryConfiguration, SessionManagerFactory> {
    private final String defaultServerName;
    private final SessionManagementProviderFactory factory;
    private final SessionManagementProvider nonDistributableProvider;

    public SharedSessionManagerDeploymentProcessor(String defaultServerName) {
        this.defaultServerName = defaultServerName;
        Iterator<SessionManagementProviderFactory> factories = ServiceLoader.load(SessionManagementProviderFactory.class, SessionManagementProviderFactory.class.getClassLoader()).iterator();
        this.factory = factories.hasNext() ? factories.next() : null;
        this.nonDistributableProvider = new NonDistributableSessionManagementProvider(this);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        SharedSessionManagerConfig sharedConfig = deploymentUnit.getAttachment(SharedSessionManagerConfig.ATTACHMENT_KEY);
        if (sharedConfig == null) return;

        String deploymentName = (deploymentUnit.getParent() == null) ? deploymentUnit.getName() : String.join(".", deploymentUnit.getParent().getName(), deploymentUnit.getName());
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        String serverName = Optional.ofNullable(warMetaData).map(metaData -> metaData.getMergedJBossWebMetaData().getServerInstanceName())
                .orElse(Optional.ofNullable(DefaultDeploymentMappingProvider.instance().getMapping(deploymentName)).map(Map.Entry::getKey).orElse(this.defaultServerName));
        SessionConfigMetaData sessionConfig = sharedConfig.getSessionConfig();
        Integer defaultSessionTimeout = ((sessionConfig != null) && sessionConfig.getSessionTimeoutSet()) ? sessionConfig.getSessionTimeout() : null;

        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ServiceTarget target = phaseContext.getServiceTarget();
        ServiceName deploymentServiceName = deploymentUnit.getServiceName();

        ServiceName managerServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_MANAGER_SERVICE_NAME);
        ServiceName codecServiceName = deploymentServiceName.append(SharedSessionManagerConfig.SHARED_SESSION_IDENTIFIER_CODEC_SERVICE_NAME);

        SessionManagementProvider provider = (this.factory != null) ? this.factory.createSessionManagementProvider(deploymentUnit, sharedConfig.getReplicationConfig()) : this.nonDistributableProvider;
        SessionManagerFactoryConfiguration configuration = new SessionManagerFactoryConfiguration() {
            @Override
            public String getServerName() {
                return serverName;
            }

            @Override
            public String getDeploymentName() {
                return deploymentName;
            }

            @Override
            public Integer getMaxActiveSessions() {
                return sharedConfig.getMaxActiveSessions();
            }

            @Override
            public Module getModule() {
                return module;
            }

            @Override
            public Integer getDefaultSessionTimeout() {
                return defaultSessionTimeout;
            }
        };
        provider.getSessionManagerFactoryServiceConfigurator(managerServiceName, configuration).configure(support).build(target).install();
        provider.getSessionIdentifierCodecServiceConfigurator(codecServiceName, configuration).configure(support).build(target).install();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    @Override
    public SessionManagerFactory apply(SessionManagerFactoryConfiguration configuration) {
        String deploymentName = configuration.getDeploymentName();
        Integer maxActiveSessions = configuration.getMaxActiveSessions();
        InMemorySessionManager manager = (maxActiveSessions != null) ? new InMemorySessionManager(deploymentName, maxActiveSessions.intValue()) : new InMemorySessionManager(deploymentName);
        Integer defaultSessionTimeout = configuration.getDefaultSessionTimeout();
        if (defaultSessionTimeout != null) {
            manager.setDefaultSessionTimeout(defaultSessionTimeout.intValue());
        }
        return new ImmediateSessionManagerFactory(manager);
    }
}
