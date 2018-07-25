/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.SessionManagementResourceDefinition.Attribute.GRANULARITY;
import static org.wildfly.extension.clustering.web.SessionManagementResourceDefinition.Attribute.ROUTING;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.routing.RoutingStrategy;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

/**
 * Abstract service configurator for session management providers.
 * @author Paul Ferraro
 */
public abstract class SessionManagementServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, DistributableSessionManagementConfiguration, Supplier<DistributableSessionManagementProvider> {

    private volatile SessionGranularity granularity;
    private volatile RoutingStrategy routingStrategy;

    SessionManagementServiceConfigurator(PathAddress address) {
        super(SessionManagementResourceDefinition.Capability.SESSION_MANAGEMENT_PROVIDER, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.granularity = ModelNodes.asEnum(GRANULARITY.resolveModelAttribute(context, model), SessionGranularity.class);
        this.routingStrategy = ModelNodes.asEnum(ROUTING.resolveModelAttribute(context, model), RoutingStrategy.class);
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<DistributableSessionManagementProvider> management = builder.provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(management, this.get()));
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.granularity.getAttributePersistenceStrategy();
    }

    @Override
    public RoutingStrategy getRoutingStrategy() {
        return this.routingStrategy;
    }
}
