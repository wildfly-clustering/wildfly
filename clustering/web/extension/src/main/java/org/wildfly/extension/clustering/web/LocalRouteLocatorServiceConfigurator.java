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

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.web.WebDeploymentRequirement;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * Service configurator for the local routing provider.
 * @author Paul Ferraro
 */
public class LocalRouteLocatorServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, RouteLocator {

    private final String route;

    public LocalRouteLocatorServiceConfigurator(String serverName, String route) {
        super(ServiceName.parse(WebDeploymentRequirement.ROUTE_LOCATOR.resolve(serverName)));
        this.route = route;
    }

    @Override
    public String locate(String sessionId) {
        return this.route;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<RouteLocator> locator = builder.provides(this.getServiceName());
        return builder.setInstance(Service.newInstance(locator, this)).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }
}
