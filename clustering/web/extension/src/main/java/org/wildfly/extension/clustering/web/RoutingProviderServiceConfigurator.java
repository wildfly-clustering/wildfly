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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.web.container.RoutingProvider;

/**
 * Abstract service configurator for routing providers.
 * @author Paul Ferraro
 */
public abstract class RoutingProviderServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<org.wildfly.clustering.web.routing.RoutingProvider>, RoutingProvider {

    public RoutingProviderServiceConfigurator(PathAddress address) {
        super(RoutingProviderResourceDefinition.Capability.ROUTING_PROVIDER, address);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RoutingProvider> provider = builder.provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(provider, this));
    }

    @Override
    public Collection<CapabilityServiceConfigurator> getServiceConfigurators(String serverName, String route) {
        return this.get().getServiceConfigurators(serverName, route);
    }
}
