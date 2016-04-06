/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.yardstick.cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.yardstickframework.BenchmarkConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Ignite benchmark that performs putAll operations.
 */
public class IgnitePutAllPreparedBenchmark extends IgniteCacheAbstractBenchmark<Integer, Object> {

    private static final int PREPARED_MAPS_COUNT = 100;

    /** Affinity mapper. */
    private Affinity<Integer> aff;

    private final List<Map<Integer, Integer>> maps = new ArrayList<>();
    private int mapsSize;

    /** {@inheritDoc} */
    @Override public void setUp(BenchmarkConfiguration cfg) throws Exception {
        super.setUp(cfg);

        aff = ignite().affinity("atomic");

        if (!args.collocated()) {
            for (int i = 0; i < PREPARED_MAPS_COUNT; i++) {
                SortedMap<Integer, Integer> valueMap = new TreeMap<>();
                for (int j = 0; j < args.batch(); j++) {
                    int key = nextRandom(args.range());

                    valueMap.put(key, key);
                }
                maps.add(valueMap);
            }
        }
        mapsSize = maps.size();
    }

    /** {@inheritDoc} */
    @Override public boolean test(Map<Object, Object> ctx) throws Exception {
        Map<Integer, Integer> valueMap = (args.collocated() ? getCollocatedValues() : maps.get(nextRandom(mapsSize)));

        cache.putAll(valueMap);

        return true;
    }

    private SortedMap<Integer, Integer> getCollocatedValues() {
        ClusterNode node = aff.mapKeyToNode(nextRandom(args.range()));

        SortedMap<Integer, Integer> valueMap = new TreeMap<>();
        for (int i = 0; i < args.batch(); ) {
            int key = nextRandom(args.range());
            if (!aff.isPrimary(node, key)) {
                continue;
            }

            valueMap.put(key, key);
            i++;
        }
        return valueMap;
    }

    /** {@inheritDoc} */
    @Override protected IgniteCache<Integer, Object> cache() {
        return ignite().cache("atomic");
    }
}
