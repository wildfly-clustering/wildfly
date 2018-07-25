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

package org.wildfly.extension.clustering.web.deployment;

import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementProvider;

/**
 * Parser for both jboss-all.xml distributable-web namespace parsing its standalone deployment descriptor counterpart.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentXMLReader implements XMLElementReader<MutableDistributableDeploymentConfiguration>, JBossAllXMLParser<DistributableWebDeploymentConfiguration> {

    private static final String SESSION_MANAGEMENT = "session-management";
    private static final String NAME = "name";
    private static final String INFINISPAN_SESSION_MANAGEMENT = "infinispan-session-management";
    private static final String CACHE_CONTAINER = "cache-container";
    private static final String CACHE = "cache";
    private static final String GRANULARITY = "granularity";
    private static final String ROUTING = "routing";

    private final DistributableWebDeploymentSchema schema;

    public DistributableWebDeploymentXMLReader(DistributableWebDeploymentSchema schema) {
        this.schema = schema;
    }

    @Override
    public DistributableWebDeploymentConfiguration parse(XMLExtendedStreamReader reader, DeploymentUnit unit) throws XMLStreamException {
        MutableDistributableDeploymentConfiguration configuration = new MutableDistributableDeploymentConfiguration(unit);
        this.readElement(reader, configuration);
        return configuration;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableDistributableDeploymentConfiguration configuration) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        Set<String> names = new TreeSet<>();
        if (this.schema.since(DistributableWebDeploymentSchema.VERSION_1_0)) {
            names.add(SESSION_MANAGEMENT);
            names.add(INFINISPAN_SESSION_MANAGEMENT);
        }

        if (!reader.hasNext() || reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            ParseUtils.missingOneOf(reader, names);
        }

        switch (reader.getLocalName()) {
            case SESSION_MANAGEMENT: {
                ParseUtils.requireSingleAttribute(reader, NAME);
                String name = reader.getAttributeValue(0);
                configuration.setSessionManagementName(name);
                break;
            }
            case INFINISPAN_SESSION_MANAGEMENT: {
                MutableInfinispanSessionManagementConfiguration config = new MutableInfinispanSessionManagementConfiguration(configuration);
                configuration.setSessionManagement(new InfinispanSessionManagementProvider(config));
                this.readInfinispanSessionManagement(reader, config);
                break;
            }
            default: {
                ParseUtils.unexpectedElement(reader, names);
            }
        }

        if (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            ParseUtils.unexpectedElement(reader);
        }
    }

    @SuppressWarnings("static-method")
    private void readSessionManagementAttribute(XMLExtendedStreamReader reader, int index, MutableSessionManagementConfiguration configuration) throws XMLStreamException {
        String value = reader.getAttributeValue(index);

        switch (reader.getAttributeLocalName(index)) {
            case GRANULARITY: {
                try {
                    configuration.setSessionGranularity(value);
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            case ROUTING: {
                try {
                    configuration.setRoutingStrategy(value);
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void readInfinispanSessionManagement(XMLExtendedStreamReader reader, MutableInfinispanSessionManagementConfiguration configuration) throws XMLStreamException {

        Set<String> required = new TreeSet<>();
        required.add(CACHE_CONTAINER);
        required.add(GRANULARITY);
        required.add(ROUTING);

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            required.remove(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);

            switch (reader.getAttributeLocalName(i)) {
                case CACHE_CONTAINER: {
                    configuration.setContainerName(value);
                    break;
                }
                case CACHE: {
                    configuration.setCacheName(value);
                    break;
                }
                default: {
                    this.readSessionManagementAttribute(reader, i, configuration);
                }
            }
        }

        if (!required.isEmpty()) {
            ParseUtils.requireAttributes(reader, required.toArray(new String[required.size()]));
        }

        ParseUtils.requireNoContent(reader);
    }
}
