/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;

/**
 * Externalizer for {@link SessionAttributeNamesEntry}.
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class SessionAttributeNamesEntryExternalizer implements Externalizer<SessionAttributeNamesEntry> {

    @Override
    public void writeObject(ObjectOutput output, SessionAttributeNamesEntry value) throws IOException {
        IndexExternalizer.VARIABLE.writeData(output, value.getSequence().get());
        ConcurrentMap<String, Integer> names = value.getNames();
        IndexExternalizer.VARIABLE.writeData(output, names.size());
        for (Map.Entry<String, Integer> entry : names.entrySet()) {
            output.writeUTF(entry.getKey());
            IndexExternalizer.VARIABLE.writeObject(output, entry.getValue());
        }
    }

    @Override
    public SessionAttributeNamesEntry readObject(ObjectInput input) throws IOException {
        AtomicInteger sequence = new AtomicInteger(IndexExternalizer.VARIABLE.readData(input));
        int size = IndexExternalizer.VARIABLE.readData(input);
        ConcurrentMap<String, Integer> names = new ConcurrentHashMap<>(size);
        for (int i = 0; i < size; ++i) {
            names.put(input.readUTF(), IndexExternalizer.VARIABLE.readObject(input));
        }
        return new SessionAttributeNamesEntry(sequence, names);
    }

    @Override
    public Class<? extends SessionAttributeNamesEntry> getTargetClass() {
        return SessionAttributeNamesEntry.class;
    }
}
