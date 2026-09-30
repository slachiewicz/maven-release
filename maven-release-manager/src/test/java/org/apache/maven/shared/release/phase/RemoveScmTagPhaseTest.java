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

import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.untag.UntagScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;

/**
 * Test the remove SCM tag phase.
 */
class RemoveScmTagPhaseTest extends AbstractReleaseTestCase {

    @Inject
    @Named("remove-scm-tag")
    private ReleasePhase phase;

    @Test
    void testExecuteOutput() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());
        ScmFileSet fileSet = new ScmFileSet(ReleaseUtil.getPomFile(rootProject).getParentFile());

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock(ScmProvider.class);
        Mockito.when(scmProviderMock.untag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isA(CommandParameters.class)))
                .thenReturn(new UntagScmResult("...", "...", "...", true));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        ReleaseResult actual =
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        // verify, actual contains trailing newline
        assertEquals(
                "[INFO] Removing tag with the label release-label ...",
                actual.getOutput().trim());
    }

    @Test
    void testExecuteResultCode() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());
        ScmFileSet fileSet = new ScmFileSet(ReleaseUtil.getPomFile(rootProject).getParentFile());

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock(ScmProvider.class);
        Mockito.when(scmProviderMock.untag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isA(CommandParameters.class)))
                .thenReturn(new UntagScmResult("...", "...", "...", true));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        ReleaseResult actual =
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(0, actual.getResultCode());
    }

    @Test
    @Disabled("We changed the behaviour to warning instead of error.")
    void testExecuteError() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());
        ScmFileSet fileSet = new ScmFileSet(ReleaseUtil.getPomFile(rootProject).getParentFile());

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock(ScmProvider.class);
        Mockito.when(scmProviderMock.untag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isA(CommandParameters.class)))
                .thenReturn(new UntagScmResult("command-line", "provider-message", "command-output", false));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        ReleaseScmCommandException e = assertThrows(
                ReleaseScmCommandException.class,
                () -> phase.execute(
                        ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects));

        assertEquals(
                "Unable to remove tag \nProvider message:\nprovider-message\nCommand output:\ncommand-output",
                e.getMessage());
    }

    @Test
    void testExecuteNoError() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());
        ScmFileSet fileSet = new ScmFileSet(ReleaseUtil.getPomFile(rootProject).getParentFile());

        // mock, only real matcher is the file set
        ScmProvider scmProviderMock = Mockito.mock(ScmProvider.class);
        Mockito.when(scmProviderMock.untag(
                        isA(ScmRepository.class),
                        argThat(new IsScmFileSetEquals(fileSet)),
                        isA(CommandParameters.class)))
                .thenReturn(new UntagScmResult("command-line", "provider-message", "command-output", false));
        scmManager.setScmProvider(scmProviderMock);

        // execute
        ReleaseResult actual =
                phase.execute(ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        // verify
        assertEquals(0, actual.getResultCode());
    }

    @Test
    void testSimulateOutput() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());

        // execute
        ReleaseResult actual =
                phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        // verify, actual contains newline
        assertEquals(
                "[INFO] Full run would remove tag with label: 'release-label'",
                actual.getOutput().trim());
    }

    @Test
    void testSimulateResultCode() throws Exception {

        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmReleaseLabel("release-label");
        builder.setScmSourceUrl("scm-url");
        List<Project> reactorProjects = createReactorProjects();
        Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
        builder.setWorkingDirectory(getPath(ReleaseUtil.getPomFile(rootProject).getParentFile()));
        builder.setPomFileName(ReleaseUtil.getPomFile(rootProject).getName());

        // execute
        ReleaseResult actual =
                phase.simulate(ReleaseUtils.buildReleaseDescriptor(builder), newReleaseEnvironment(), reactorProjects);

        assertEquals(0, actual.getResultCode());
    }

    private List<Project> createReactorProjects() throws Exception {
        return createReactorProjects("scm-commit/single-pom", "");
    }
}
