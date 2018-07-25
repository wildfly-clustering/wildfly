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

package org.wildfly.clustering.web.infinispan.routing;

import static org.mockito.Mockito.*;

import java.util.AbstractMap;
import java.util.Arrays;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.LocalizedCacheTopology;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.topology.CacheTopology;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;
import org.wildfly.clustering.web.routing.RoutingStrategy;

/**
 * Unit test for {@link InfinispanRouteLocator}.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class InfinispanRouteLocatorTestCase {

    @Parameters
    public static Iterable<CacheMode> cacheModes() {
        return Arrays.asList(CacheMode.DIST_SYNC, CacheMode.REPL_SYNC, CacheMode.SCATTERED_SYNC, CacheMode.INVALIDATION_SYNC, CacheMode.LOCAL);
    }

    private final Address localAddress = mock(Address.class);
    private final Address ownerAddress = mock(Address.class);
    private final Address backupAddress = mock(Address.class);
    private final Node localMember = mock(Node.class);
    private final Node ownerMember = mock(Node.class);
    private final Node backupMember = mock(Node.class);
    private final AdvancedCache<String, ?> cache = mock(AdvancedCache.class);
    private final DistributionManager dist = mock(DistributionManager.class);
    private final NodeFactory<Address> factory = mock(NodeFactory.class);
    private final Registry<String, Void> registry = mock(Registry.class);
    private final Group group = mock(Group.class);
    private final KeyPartitioner partitioner = mock(KeyPartitioner.class);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public InfinispanRouteLocatorTestCase(CacheMode mode) {
        EmbeddedCacheManager manager = mock(EmbeddedCacheManager.class);
        Configuration config = new ConfigurationBuilder().clustering().cacheMode(mode).build();
        when(this.cache.getCacheManager()).thenReturn(manager);
        when(manager.getAddress()).thenReturn(this.localAddress);
        when(this.cache.getCacheConfiguration()).thenReturn(config);
        when(this.cache.getAdvancedCache()).thenReturn((AdvancedCache) this.cache);
        when(this.cache.getDistributionManager()).thenReturn(this.dist);
        ConsistentHash hash = mock(ConsistentHash.class);
        when(hash.getMembers()).thenReturn(Arrays.asList(this.localAddress, this.ownerAddress, this.backupAddress));
        when(hash.getNumSegments()).thenReturn(3);
        // Segment 0, local is not an owner
        when(hash.locatePrimaryOwnerForSegment(0)).thenReturn(this.ownerAddress);
        when(hash.locateOwnersForSegment(0)).thenReturn(Arrays.asList(this.ownerAddress, this.backupAddress));
        // Segment 1, local is primary owner
        when(hash.locatePrimaryOwnerForSegment(1)).thenReturn(this.localAddress);
        when(hash.locateOwnersForSegment(1)).thenReturn(Arrays.asList(this.localAddress, this.backupAddress));
        // Segment 2, local is a backup owner
        when(hash.locatePrimaryOwnerForSegment(2)).thenReturn(this.ownerAddress);
        when(hash.locateOwnersForSegment(2)).thenReturn(Arrays.asList(this.ownerAddress, this.localAddress, this.backupAddress));
        CacheTopology topology = new CacheTopology(1, 1, hash, null, CacheTopology.Phase.NO_REBALANCE, hash.getMembers(), null);
        LocalizedCacheTopology localizedTopology = new LocalizedCacheTopology(mode, topology, this.partitioner, manager.getAddress(), true);
        when(this.dist.getCacheTopology()).thenReturn(localizedTopology);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private RouteLocator createRouteLocator(RoutingStrategy strategy) {
        InfinispanRouteLocatorConfiguration config = mock(InfinispanRouteLocatorConfiguration.class);

        when(config.getCache()).thenReturn((AdvancedCache) this.cache);
        when(config.getMemberFactory()).thenReturn(this.factory);
        when(config.getRegistry()).thenReturn(this.registry);
        when(config.getStrategy()).thenReturn(strategy);
        when(this.registry.getGroup()).thenReturn(this.group);
        when(this.group.getLocalMember()).thenReturn(this.localMember);
        when(this.registry.getEntry(this.localMember)).thenReturn(new AbstractMap.SimpleImmutableEntry<>("local", null));
        when(this.registry.getEntry(this.ownerMember)).thenReturn(new AbstractMap.SimpleImmutableEntry<>("owner", null));
        when(this.registry.getEntry(this.backupMember)).thenReturn(new AbstractMap.SimpleImmutableEntry<>("backup", null));
        when(this.factory.createNode(this.localAddress)).thenReturn(this.localMember);
        when(this.factory.createNode(this.ownerAddress)).thenReturn(this.ownerMember);
        when(this.factory.createNode(this.backupAddress)).thenReturn(this.backupMember);

        return new InfinispanRouteLocator(config);
    }

    @SuppressWarnings("static-method")
    private void verifyLocal(RouteLocator locator) {
        String result = locator.locate("session");
        Assert.assertEquals("local", result);
    }

    private void verifyOwner(RouteLocator locator) {
        when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(0);
        String result = locator.locate("session");
        Assert.assertEquals("owner", result);

        when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(1);
        result = locator.locate("session");
        Assert.assertEquals("local", result);

        when(this.partitioner.getSegment(new Key<>("session"))).thenReturn(2);
        result = locator.locate("session");
        Assert.assertEquals("owner", result);
    }

    @Test
    public void testNone() {
        RouteLocator locator = this.createRouteLocator(RoutingStrategy.NONE);
        String result = locator.locate("session");
        Assert.assertNull(result);
    }

    @Test
    public void testLocal() {
        RouteLocator locator = this.createRouteLocator(RoutingStrategy.LOCAL);
        this.verifyLocal(locator);
    }

    @Test
    public void testOwner() {
        RouteLocator locator = this.createRouteLocator(RoutingStrategy.OWNER);
        CacheMode mode = this.cache.getCacheConfiguration().clustering().cacheMode();
        switch (mode) {
            case REPL_SYNC:
            case DIST_SYNC: {
                this.verifyOwner(locator);
                break;
            }
            default: {
                this.verifyLocal(locator);
            }
        }
    }
}