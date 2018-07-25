/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.clustering.web;

import java.util.EnumSet;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.clustering.subsystem.ClusteringSubsystemTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;

/**
 * Unit test for distributable-web subsystem.
 * @author Paul Ferraro
 */
@RunWith(value = Parameterized.class)
public class DistributableWebSubsystemTestCase extends ClusteringSubsystemTest<DistributableWebSchema> {

    @Parameters
    public static Iterable<DistributableWebSchema> parameters() {
        return EnumSet.allOf(DistributableWebSchema.class);
    }

    public DistributableWebSubsystemTestCase(DistributableWebSchema schema) {
        super(DistributableWebExtension.SUBSYSTEM_NAME, new DistributableWebExtension(), schema, DistributableWebSchema.CURRENT, "wildfly-distributable-web-%d_%d.xml", "schema/wildfly-distributable-web_%d_%d.xsd");
    }

    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(InfinispanDefaultCacheRequirement.CONFIGURATION, "foo")
                .require(InfinispanCacheRequirement.CONFIGURATION, "foo", "bar")
                ;
    }
}