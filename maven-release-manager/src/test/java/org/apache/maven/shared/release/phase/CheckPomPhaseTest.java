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
package org.apache.maven.shared.release.phase;

import java.util.Collections;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Scm;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.stubs.TestProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the POM verification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
class CheckPomPhaseTest extends AbstractInjectedTestCase {
    @Inject
    @Named("check-poms")
    private ReleasePhase phase;

    @Test
    void testCorrectlyConfigured() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:file://localhost/tmp/repo");

        phase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(createProject("1.0-SNAPSHOT")));

        phase.simulate(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(createProject("1.0-SNAPSHOT")));

        // successful execution is verification enough
        assertTrue(true);
    }

    @Test
    void testGetUrlFromProjectConnection() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:file://localhost/tmp/repo");

        Project project = createProject("1.0-SNAPSHOT");

        phase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(project));

        assertEquals(
                "scm:svn:file://localhost/tmp/repo",
                ReleaseUtils.buildReleaseDescriptor(builder).getScmSourceUrl(),
                "Check URL");
    }

    @Test
    void testGetUrlFromProjectConnectionSimulate() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:file://localhost/tmp/repo");

        Project project = createProject("1.0-SNAPSHOT");

        phase.simulate(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(project));

        assertEquals(
                "scm:svn:file://localhost/tmp/repo",
                ReleaseUtils.buildReleaseDescriptor(builder).getScmSourceUrl(),
                "Check URL");
    }

    @Test
    void testGetUrlFromProjectDevConnection() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:https://localhost/tmp/repo");

        Project project = createProject("1.0-SNAPSHOT");

        phase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(project));

        assertEquals(
                "scm:svn:https://localhost/tmp/repo",
                ReleaseUtils.buildReleaseDescriptor(builder).getScmSourceUrl(),
                "Check URL");
    }

    @Test
    void testGetUrlFromProjectDevConnectionSimulate() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:https://localhost/tmp/repo");

        Project project = createProject("1.0-SNAPSHOT");

        phase.simulate(
                ReleaseUtils.buildReleaseDescriptor(builder),
                newReleaseEnvironment(),
                Collections.singletonList(project));

        assertEquals(
                "scm:svn:https://localhost/tmp/repo",
                ReleaseUtils.buildReleaseDescriptor(builder).getScmSourceUrl(),
                "Check URL");
    }

    @Test
    void testGetInvalidUrl() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:");

        Project project = createProject("1.0-SNAPSHOT");

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(project));

            fail("Should have thrown an exception");
        } catch (ReleaseScmRepositoryException e) {
            assertTrue(true);
        }
    }

    @Test
    void testGetInvalidProvider() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        Project project = createProject(
                "1.0-SNAPSHOT", Scm.newBuilder().connection("scm:foo:").build());

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(project));

            fail("Should have thrown an exception");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testMissingUrl() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(createProject("1.0-SNAPSHOT")));

            fail("Should have failed to execute");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(createProject("1.0-SNAPSHOT")));

            fail("Should have failed to simulate");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    @Test
    void testReleasingNonSnapshot() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl("scm:svn:file://localhost/tmp/repo");

        try {
            phase.execute(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(createProject("1.0")));

            fail("Should have failed to execute");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }

        try {
            phase.simulate(
                    ReleaseUtils.buildReleaseDescriptor(builder),
                    newReleaseEnvironment(),
                    Collections.singletonList(createProject("1.0")));

            fail("Should have failed to simulate");
        } catch (ReleaseFailureException e) {
            assertTrue(true);
        }
    }

    private static Project createProject(String version) {
        return createProject(version, null);
    }

    private static Project createProject(String version, Scm scm) {
        Model model = Model.newBuilder()
                .artifactId("artifactId")
                .groupId("groupId")
                .version(version)
                .scm(scm)
                .build();

        return TestProject.of(model);
    }
}
