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

package com.spotify.heroic.metric;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.spotify.heroic.aggregation.AggregationSession;
import com.spotify.heroic.aggregation.Bucket;
import com.spotify.heroic.aggregation.ReducerSession;
import com.spotify.heroic.common.Series;

public class EmptyMetricCollection extends MetricCollection {
    public EmptyMetricCollection() {
        super(MetricType.POINT, ImmutableList.of());
    }

    @Override
    public void updateAggregation(AggregationSession session, Map<String, String> tags,
            Set<Series> series) {
    }

    @Override
    public void updateBucket(Bucket bucket, Map<String, String> tags) {
    }

    @Override
    public void updateReducer(ReducerSession session, Map<String, String> tags) {
    }
}
