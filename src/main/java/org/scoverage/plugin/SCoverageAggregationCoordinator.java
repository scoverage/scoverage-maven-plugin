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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.maven.execution.MavenSession;

/**
 * Coordinates aggregated coverage report generation in multi-threaded Maven builds.
 * <br>
 * Uses MavenSession request data to store state, which works across different plugin classloaders.
 * Stores only standard Java types (ConcurrentHashMap, AtomicBoolean) that are loaded by the system classloader.
 * <br>
 * Ensures that:
 * <ul>
 * <li>Only one thread (the last module) performs the aggregation</li>
 * <li>Other threads skip aggregation and continue</li>
 * </ul>
 */
public class SCoverageAggregationCoordinator
{
    private static final String REMAINING_MODULES_KEY = "scoverage-remaining-modules";
    private static final String CLAIMED_KEY = "scoverage-claimed";

    /**
     * Notifies the coordinator that this module has completed testing and determines if it should perform aggregation.
     * Only the last module to complete (in a parallel build) will return true, and only once.
     *
     * @param session Maven session
     * @param moduleId unique identifier for the module (e.g., groupId:artifactId)
     * @param expectedModuleIds set of expected module IDs (auto-registers session if needed)
     * @return true if this module should perform aggregation, false otherwise
     */
    public static boolean shouldPerformAggregation( MavenSession session, String moduleId, Set<String> expectedModuleIds )
    {
        // Get shared data map from Maven session - this is shared across all plugin classloaders
        Map<String, Object> sessionData = session.getRequest().getData();

        // Initialize remaining modules set and claimed flag (thread-safe, only once per session)
        Set<String> remainingModules = getOrCreate( sessionData, REMAINING_MODULES_KEY, () -> {
            Set<String> modules = ConcurrentHashMap.newKeySet();
            modules.addAll( expectedModuleIds );
            return modules;
        } );

        AtomicBoolean claimed = getOrCreate( sessionData, CLAIMED_KEY, () -> new AtomicBoolean( false ) );

        // Remove this module from the remaining set
        remainingModules.remove( moduleId );

        // Return true only if this is the last module and nobody else has claimed aggregation
        return remainingModules.isEmpty() && claimed.compareAndSet( false, true );
    }

    /**
     * Gets an object from the session data map, or creates it if absent using double-checked locking.
     * This ensures thread-safe initialization without unnecessary synchronization on subsequent calls.
     *
     * @param sessionData the shared session data map
     * @param key the key to look up
     * @param supplier function to create the value if absent
     * @return the value from the map (existing or newly created)
     */
    @SuppressWarnings("unchecked")
    private static <T> T getOrCreate( Map<String, Object> sessionData, String key, Supplier<T> supplier )
    {
        Object value = sessionData.get( key );
        if ( value == null )
        {
            synchronized ( sessionData )
            {
                value = sessionData.get( key );
                if ( value == null )
                {
                    value = supplier.get();
                    sessionData.put( key, value );
                }
            }
        }
        return (T) value;
    }
}
