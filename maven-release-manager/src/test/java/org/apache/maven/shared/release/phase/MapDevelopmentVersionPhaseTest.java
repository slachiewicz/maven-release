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
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.stubs.TestProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapDevelopmentVersionPhaseTest extends AbstractInjectedTestCase {
    @Inject
    @Named("map-development-versions")
    private MapDevelopmentVersionsPhase mapDevelopmentVersionsPhase;

    private static Project createProject(String artifactId, String version) {
        return TestProject.of("groupId", artifactId, version);
    }

    @Test
    void testNoUpdateWorkingCopyVersions() throws Exception {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setInteractive(false);
        builder.setUpdateWorkingCopyVersions(false);

        List<Project> reactorProjects = Collections.singletonList(createProject("artifactId", "1.0"));
        mapDevelopmentVersionsPhase.execute(
                ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        assertEquals(
                "1.0", ReleaseUtils.buildReleaseDescriptor(builder).getProjectDevelopmentVersion("groupId:artifactId"));
    }
}
