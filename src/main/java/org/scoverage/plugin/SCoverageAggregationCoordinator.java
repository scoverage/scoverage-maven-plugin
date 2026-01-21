/*
 * Copyright 2014-2026 Grzegorz Slowikowski (gslowikowski at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.scoverage.plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates aggregated coverage report generation in multi-threaded Maven builds.
 * <br>
 * Ensures that:
 * <ul>
 * <li>Only one thread (the last module) performs the aggregation</li>
 * <li>Other threads skip aggregation and continue</li>
 * </ul>
 */
public class SCoverageAggregationCoordinator
{
    /**
     * Map of build session ID to aggregation session.
     * Each Maven build session gets its own coordinator.
     */
    private static final ConcurrentHashMap<String, AggregationSession> sessions = new ConcurrentHashMap<>();

    /**
     * Represents a single Maven build session's aggregation state.
     */
    private static class AggregationSession
    {
        private final AtomicBoolean aggregationClaimed;
        private final Set<String> remainingModuleIds;

        AggregationSession( Set<String> expectedModuleIds )
        {
            this.remainingModuleIds = ConcurrentHashMap.newKeySet();
            this.remainingModuleIds.addAll( expectedModuleIds );
            this.aggregationClaimed = new AtomicBoolean( false );
        }

        /**
         * Notifies that this module has completed and determines if it should perform aggregation.
         * Returns true only for the last module to complete, and only once.
         * <p>
         * Thread-safe: The Set removes completed modules.
         * When set gets empty, we know all modules have entered the report phase,
         * which means their test phases are complete and measurement files are written.
         * The AtomicBoolean ensures only one thread claims aggregation.
         *
         * @param moduleId unique identifier for the module
         * @return true if this module should perform aggregation
         */
        boolean shouldPerformAggregation( String moduleId )
        {
            remainingModuleIds.remove( moduleId );
            return remainingModuleIds.isEmpty() && aggregationClaimed.compareAndSet( false, true );
        }
    }

    /**
     * Notifies the coordinator that this module has completed testing and determines if it should perform aggregation.
     * Only the last module to complete (in a parallel build) will return true, and only once.
     *
     * @param sessionId build session identifier
     * @param moduleId unique identifier for the module (e.g., groupId:artifactId)
     * @param expectedModuleIds set of expected module IDs (auto-registers session if needed)
     * @return true if this module should perform aggregation, false otherwise
     */
    public static boolean shouldPerformAggregation( String sessionId, String moduleId, Set<String> expectedModuleIds )
    {
        AggregationSession session = sessions.computeIfAbsent( sessionId,
                id -> new AggregationSession( expectedModuleIds ) );

        return session.shouldPerformAggregation( moduleId );
    }

    /**
     * Cleans up session data after build completes.
     *
     * @param sessionId build session identifier
     */
    public static void cleanup( String sessionId )
    {
        sessions.remove( sessionId );
    }
}
