/*
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
package org.apache.maven.shared.release;

import java.util.Map;

import org.apache.maven.api.di.Inject;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.phase.ReleasePhase;
import org.apache.maven.shared.release.policy.naming.NamingPolicy;
import org.apache.maven.shared.release.policy.version.VersionPolicy;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.strategy.Strategy;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.transform.ModelETLFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the release components are wired by the Maven 4 dependency injection container, which only knows the
 * classes listed in {@code META-INF/maven/org.apache.maven.api.di.Inject}. The list is maintained by hand.
 */
class DependencyInjectionTest extends AbstractInjectedTestCase {
    @Inject
    private ReleaseManager releaseManager;

    @Inject
    private Map<String, Strategy> strategies;

    @Inject
    private Map<String, ReleasePhase> phases;

    @Inject
    private Map<String, VersionPolicy> versionPolicies;

    @Inject
    private Map<String, NamingPolicy> namingPolicies;

    @Inject
    private Map<String, MavenExecutor> executors;

    @Inject
    private Map<String, ModelETLFactory> modelETLFactories;

    @Inject
    private Map<String, ScmTranslator> scmTranslators;

    @Inject
    private ScmManager scmManager;

    @Test
    void testReleaseManager() {
        assertNotNull(releaseManager);
    }

    /**
     * A bare {@code @Named} is the empty key of a map injection in the Maven 4 container, not {@code default} as in
     * Sisu. The release manager looks these up by {@code default}.
     */
    @Test
    void testDefaultsAreAvailableByName() {
        assertTrue(strategies.containsKey("default"), strategies.keySet().toString());
        assertTrue(
                versionPolicies.containsKey("default"), versionPolicies.keySet().toString());
        assertTrue(
                namingPolicies.containsKey("default"), namingPolicies.keySet().toString());
    }

    @Test
    void testAllPhasesOfTheDefaultStrategyExist() {
        Strategy strategy = strategies.get("default");
        for (java.util.List<String> names : java.util.Arrays.asList(
                strategy.getPreparePhases(),
                strategy.getPerformPhases(),
                strategy.getBranchPhases(),
                strategy.getRollbackPhases(),
                strategy.getUpdateVersionsPhases())) {
            for (String name : names) {
                assertTrue(phases.containsKey(name), "no phase " + name + " in " + phases.keySet());
            }
        }
    }

    @Test
    void testExecutorsAndTransformers() {
        assertTrue(executors.containsKey(
                org.apache.maven.shared.release.env.ReleaseEnvironment.DEFAULT_MAVEN_EXECUTOR_ID));
        assertTrue(executors.containsKey("invoker"));
        assertTrue(modelETLFactories.containsKey("domtrip"));
        assertTrue(scmTranslators.containsKey("git"));
        assertTrue(scmTranslators.containsKey("svn"));
    }

    @Test
    void testScmProvidersAreDiscovered() throws Exception {
        assertNotNull(scmManager.getProviderByType("git"));
        assertNotNull(scmManager.getProviderByType("svn"));
    }
}
