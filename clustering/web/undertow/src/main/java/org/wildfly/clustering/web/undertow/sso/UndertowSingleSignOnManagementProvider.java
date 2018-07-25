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

package org.wildfly.clustering.web.undertow.sso;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.service.ActiveServiceSupplier;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.WebDefaultProviderRequirement;
import org.wildfly.clustering.web.WebProviderRequirement;
import org.wildfly.clustering.web.container.HostSingleSignOnManagementConfiguration;
import org.wildfly.clustering.web.container.HostSingleSignOnManagementProvider;
import org.wildfly.clustering.web.sso.DistributableSSOManagementProvider;
import org.wildfly.clustering.web.sso.LegacySSOManagementProviderFactory;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

/**
 * {@link SessionManagementProviderFactory} for Undertow using either the {@link DistributableSSOManagementProvider} for the given host, the default provider, or a legacy provider.
 * @author Paul Ferraro
 */
@MetaInfServices(HostSingleSignOnManagementProvider.class)
public class UndertowSingleSignOnManagementProvider implements HostSingleSignOnManagementProvider {

    final DistributableSSOManagementProvider legacyProvider;

    public UndertowSingleSignOnManagementProvider() {
        Iterator<LegacySSOManagementProviderFactory> factories = ServiceLoader.load(LegacySSOManagementProviderFactory.class, LegacySSOManagementProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacySSOManagementProviderFactory.class.getName());
        }
        this.legacyProvider = factories.next().createSSOManagementProvider();
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, HostSingleSignOnManagementConfiguration configuration) {
        return new CapabilityServiceConfigurator() {
            private volatile CapabilityServiceConfigurator configurator;

            @Override
            public ServiceName getServiceName() {
                return name;
            }

            @Override
            public ServiceConfigurator configure(OperationContext context) {
                String hostName = configuration.getHostName();
                String hostCapabilityName = RuntimeCapability.buildDynamicCapabilityName(Capabilities.CAPABILITY_HOST, configuration.getServerName(), hostName);
                ServiceName providerServiceName = null;
                if (context.hasOptionalCapability(WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.resolve(hostName), hostCapabilityName, null)) {
                    providerServiceName = WebProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context, hostName);
                } else if (context.hasOptionalCapability(WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getName(), hostCapabilityName, null)) {
                    providerServiceName = WebDefaultProviderRequirement.SSO_MANAGEMENT_PROVIDER.getServiceName(context);
                } else {
                    UndertowClusteringLogger.ROOT_LOGGER.legacySingleSignOnProviderInUse(hostName);
                }
                Supplier<DistributableSSOManagementProvider> providerSupplier = (providerServiceName != null) ? new ActiveServiceSupplier<>(context.getServiceRegistry(true), providerServiceName) : null;
                DistributableSSOManagementProvider provider = (providerSupplier != null) ? providerSupplier.get() : UndertowSingleSignOnManagementProvider.this.legacyProvider;
                this.configurator = new DistributableSingleSignOnManagerServiceConfigurator(this.getServiceName(), configuration, provider);
                return this.configurator.configure(context);
            }

            @Override
            public ServiceBuilder<?> build(ServiceTarget target) {
                return this.configurator.build(target);
            }
        };
    }
}
