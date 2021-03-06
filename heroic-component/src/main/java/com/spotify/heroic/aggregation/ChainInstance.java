/*
 * Copyright (c) 2015 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.heroic.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.spotify.heroic.common.DateRange;
import com.spotify.heroic.common.Series;
import com.spotify.heroic.common.Statistics;
import com.spotify.heroic.metric.Event;
import com.spotify.heroic.metric.MetricGroup;
import com.spotify.heroic.metric.Point;
import com.spotify.heroic.metric.Spread;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A special aggregation method that is a chain of other aggregation methods.
 *
 * @author udoprog
 */
@Data
public class ChainInstance implements AggregationInstance {
    private static final ArrayList<AggregationInstance> EMPTY_AGGREGATORS = new ArrayList<>();

    private final List<AggregationInstance> chain;

    @JsonCreator
    public ChainInstance(@JsonProperty("chain") List<AggregationInstance> chain) {
        this.chain = checkNotEmpty(Optional.fromNullable(chain).or(EMPTY_AGGREGATORS), "chain");
    }

    static <T> List<T> checkNotEmpty(List<T> list, String what) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException(what + " must not be empty");
        }

        return list;
    }

    /**
     * The last aggregation in the chain determines the estimated number of samples.
     */
    @Override
    public long estimate(DateRange range) {
        return chain.get(chain.size() - 1).estimate(range);
    }

    /**
     * The last aggregation in the chain determines the cadence.
     */
    @Override
    public long cadence() {
        return chain.get(chain.size() - 1).cadence();
    }

    @Override
    public AggregationInstance distributed() {
        final Iterator<AggregationInstance> it = chain.iterator();

        final ImmutableList.Builder<AggregationInstance> chain = ImmutableList.builder();

        do {
            final AggregationInstance a = it.next();
            chain.add(it.hasNext() ? a : a.distributed());
        } while (it.hasNext());

        return new ChainInstance(chain.build());
    }

    @Override
    public AggregationCombiner combiner(final DateRange range) {
        return chain.get(chain.size() - 1).combiner(range);
    }

    @Override
    public ReducerSession reducer(final DateRange range) {
        return chain.get(chain.size() - 1).reducer(range);
    }

    @Override
    public AggregationTraversal session(final List<AggregationState> input, final DateRange range) {
        final Iterator<AggregationInstance> iter = chain.iterator();

        final AggregationInstance first = iter.next();
        final AggregationTraversal head = first.session(input, range);

        AggregationTraversal prev = head;

        final List<AggregationSession> tail = new ArrayList<>();

        while (iter.hasNext()) {
            final AggregationTraversal s = iter.next().session(prev.getStates(), range);
            tail.add(s.getSession());
            prev = s;
        }

        return new AggregationTraversal(prev.getStates(), new Session(head.getSession(), tail));
    }

    private static final Joiner chainJoiner = Joiner.on(" -> ");

    @Override
    public String toString() {
        return "[" + chainJoiner.join(chain.stream().map(Object::toString).iterator()) + "]";
    }

    @RequiredArgsConstructor
    private static final class Session implements AggregationSession {
        private final AggregationSession first;
        private final Iterable<AggregationSession> rest;

        @Override
        public void updatePoints(Map<String, String> group, Set<Series> series,
                List<Point> values) {
            first.updatePoints(group, series, values);
        }

        @Override
        public void updateEvents(Map<String, String> group, Set<Series> series,
                List<Event> values) {
            first.updateEvents(group, series, values);
        }

        @Override
        public void updateSpreads(Map<String, String> group, Set<Series> series,
                List<Spread> values) {
            first.updateSpreads(group, series, values);
        }

        @Override
        public void updateGroup(Map<String, String> group, Set<Series> series,
                List<MetricGroup> values) {
            first.updateGroup(group, series, values);
        }

        @Override
        public AggregationResult result() {
            final AggregationResult firstResult = first.result();
            List<AggregationData> current = firstResult.getResult();
            Statistics statistics = firstResult.getStatistics();

            for (final AggregationSession session : rest) {
                for (final AggregationData u : current) {
                    u.getMetrics().updateAggregation(session, u.getGroup(), u.getSeries());
                }

                final AggregationResult next = session.result();
                current = next.getResult();
                statistics = statistics.merge(next.getStatistics());
            }

            return new AggregationResult(current, statistics);
        }

        @Override
        public String toString() {
            if (rest.iterator().hasNext()) {
                return "[" + first + ", " + Joiner.on(", ").join(rest) + "]";
            }

            return "[" + first + "]";
        }
    }
}
