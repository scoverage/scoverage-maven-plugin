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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;

/**
 * Coordinates aggregated coverage report generation in multi-threaded Maven builds.
 * <br>
 * Uses RepositorySystemSession.getData() which provides build-global, thread-safe storage
 * that works across different plugin classloaders and all modules in the reactor.
 * <br>
 * Only stores standard Java types (ConcurrentHashMap, AtomicBoolean) loaded by the bootstrap classloader
 * to avoid ClassCastException across different plugin classloaders.
 * <br>
 * Ensures that:
 * <ul>
 * <li>Only one thread (the last module) performs the aggregation</li>
 * <li>Other threads skip aggregation and continue</li>
 * </ul>
 * <br>
 * Implementation Note: Uses synchronized block with SessionData.get()/set() instead of computeIfAbsent()
 * for backward compatibility with Maven 3.6.3+ (computeIfAbsent was added in Maven Resolver 1.9.x / Maven 3.9.x).
 */
public class SCoverageAggregationCoordinator
{
    private static final String REMAINING_MODULES_KEY = SCoverageAggregationCoordinator.class.getName() + ".remainingModules";
    private static final String CLAIMED_KEY = SCoverageAggregationCoordinator.class.getName() + ".claimed";

    /**
     * Notifies the coordinator that this module has completed testing and determines if it should perform aggregation.
     * Only the last module to complete (in a parallel build) will return true, and only once.
     *
     * @param repositorySession Repository system session providing build-global storage
     * @param moduleId unique identifier for the module (e.g., groupId:artifactId)
     * @param expectedModuleIds set of expected module IDs (auto-registers build if needed)
     * @return true if this module should perform aggregation, false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean shouldPerformAggregation( RepositorySystemSession repositorySession, String moduleId, Set<String> expectedModuleIds )
    {
        SessionData sessionData = repositorySession.getData();

        // Initialize data structures using backward-compatible get/set API
        // (computeIfAbsent was added in Maven Resolver 1.9.x / Maven 3.9.x)
        Set<String> remainingModules;
        AtomicBoolean claimed;

        // Use SessionData itself as synchronization lock for initialization
        // SessionData is build-global and thread-safe, so it's safe to synchronize on
        synchronized ( sessionData )
        {
            // Get or create remaining modules set
            remainingModules = (Set<String>) sessionData.get( REMAINING_MODULES_KEY );
            if ( remainingModules == null )
            {
                remainingModules = ConcurrentHashMap.newKeySet();
                remainingModules.addAll( expectedModuleIds );
                sessionData.set( REMAINING_MODULES_KEY, remainingModules );
            }

            // Get or create claimed flag
            claimed = (AtomicBoolean) sessionData.get( CLAIMED_KEY );
            if ( claimed == null )
            {
                claimed = new AtomicBoolean( false );
                sessionData.set( CLAIMED_KEY, claimed );
            }
        }

        // Remove this module from the remaining set
        remainingModules.remove( moduleId );

        // Return true only if this is the last module and nobody else has claimed aggregation
        boolean result = remainingModules.isEmpty() && claimed.compareAndSet( false, true );

        // Clean up after aggregation is claimed
        if ( result )
        {
            sessionData.set( REMAINING_MODULES_KEY, null );
            sessionData.set( CLAIMED_KEY, null );
        }

        return result;
    }
}
