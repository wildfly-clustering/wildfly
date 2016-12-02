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

package org.wildfly.clustering.ee.hotrod;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;

/**
 * HotRod doesn't support batching.
 * @author Paul Ferraro
 */
public enum HotRodBatcher implements Batcher<Batch> {

    INSTANCE;

    @Override
    public Batch createBatch() {
        return HotRodBatch.INSTANCE;
    }

    @Override
    public BatchContext resumeBatch(Batch batch) {
        return () -> {};
    }

    @Override
    public Batch suspendBatch() {
        return HotRodBatch.INSTANCE;
    }

    private enum HotRodBatch implements Batch {
        INSTANCE;

        @Override
        public void close() {
        }

        @Override
        public void discard() {
        }

        @Override
        public State getState() {
            return State.ACTIVE;
        }
    }
}
