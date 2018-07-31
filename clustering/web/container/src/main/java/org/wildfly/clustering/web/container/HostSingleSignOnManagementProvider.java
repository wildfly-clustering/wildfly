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

package org.wildfly.clustering.web.container;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;

/**
 * Container-specific single sign-on management provider for a host.
 * @author Paul Ferraro
 */
public interface HostSingleSignOnManagementProvider {

    /**
     * Returns a configurator for a service providing container-specific single sign-on management for a host.
     * @param name the service name of the single sign-on management
     * @param configuration the configuration of the host's single sign-on management
     * @return a configurator for a service providing a container-specific single sign-on management
     */
    CapabilityServiceConfigurator getServiceConfigurator(ServiceName name, HostSingleSignOnManagementConfiguration configuration);
}
