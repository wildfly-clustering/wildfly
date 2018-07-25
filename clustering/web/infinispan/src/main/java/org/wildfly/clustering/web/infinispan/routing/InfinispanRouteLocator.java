/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.routing.RoutingStrategy;

/**
 * Uses Infinispan's {@link org.infinispan.distribution.DistributionManager} to determine the best node (i.e. the primary lock owner) to handle a given session.
 * The {@link Address} is then converted to a route using a {@link Registry}, which maps the route identifier per node.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocator implements RouteLocator {

    private final NodeFactory<Address> factory;
    private final Registry<String, Void> registry;
    private final Cache<String, ?> cache;
    private final RoutingStrategy strategy;

    public InfinispanRouteLocator(InfinispanRouteLocatorConfiguration config) {
        this.cache = config.getCache();
        this.registry = config.getRegistry();
        this.factory = config.getMemberFactory();
        this.strategy = config.getStrategy();
    }

    @Override
    public String locate(String sessionId) {
        Configuration config = this.cache.getCacheConfiguration();
        CacheMode mode = config.clustering().cacheMode();
        switch (this.strategy) {
            case OWNER: {
                if (mode.needsStateTransfer() && !mode.isScattered()) {
                    DistributionInfo info = this.getDistributionInfo(sessionId);
                    List<String> routes = this.getRoutes(Collections.singletonList(info.primary()));
                    if (!routes.isEmpty()) {
                        return routes.get(0);
                    }
                }
                // Fall through to LOCAL logic if cache is scattered or does not share state
            }
            case LOCAL: {
                Map.Entry<String, Void> entry = this.registry.getEntry(this.registry.getGroup().getLocalMember());
                if (entry != null) {
                    return entry.getKey();
                }
                // Fall through to NONE logic if route does not exist
            }
            case NONE: {
                return null;
            }
            default: {
                throw new IllegalStateException(this.strategy.name());
            }
        }
    }

    private DistributionInfo getDistributionInfo(String sessionId) {
        return this.cache.getAdvancedCache().getDistributionManager().getCacheTopology().getDistribution(new Key<>(sessionId));
    }

    private List<String> getRoutes(List<Address> addresses) {
        List<String> routes = new ArrayList<>(addresses.size());
        for (Address address : addresses) {
            Node node = this.factory.createNode(address);
            Map.Entry<String, Void> entry = this.registry.getEntry(node);
            if (entry != null) {
                routes.add(entry.getKey());
            }
        }
        return routes;
    }
}
