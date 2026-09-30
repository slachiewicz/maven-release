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
package org.apache.maven.shared.release.stubs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.impl.DefaultDependencyCoordinates;
import org.apache.maven.impl.InternalSession;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * A {@link Project} for tests. The Maven 4 API has no project implementation outside of maven-core, so this one is
 * backed by the effective model that the model builder of maven-impl produced for a POM file.
 */
public class TestProject implements Project {
    private final Session session;

    private final Model effectiveModel;

    private final Path pomPath;

    private boolean topProject;

    private Project parent;

    public TestProject(Session session, Model effectiveModel, Path pomPath, boolean topProject) {
        this.session = session;
        this.effectiveModel = effectiveModel;
        this.pomPath = pomPath;
        this.topProject = topProject;
    }

    /**
     * A project that only knows its model, for tests of phases that read no more than that.
     *
     * @param model the model, used as the effective model
     * @return a top project without POM file
     */
    public static TestProject of(Model model) {
        return new TestProject(null, model, null, true);
    }

    /**
     * A project that only knows its model and POM file.
     *
     * @param model the model, used as the effective model
     * @param pomPath the POM file, does not have to exist
     * @return a top project
     */
    public static TestProject of(Model model, Path pomPath) {
        return new TestProject(null, model, pomPath, true);
    }

    /**
     * A project with the given coordinates and no POM file.
     *
     * @return a top project
     */
    public static TestProject of(String groupId, String artifactId, String version) {
        return of(Model.newBuilder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build());
    }

    /**
     * A project whose model is also written to a POM file below {@code target}, for phases that read the POM file.
     *
     * @param session the session, whose XML factory writes the file
     * @param model the model, used as the effective model and written as the POM
     * @return a top project
     */
    public static TestProject written(Session session, Model model) throws IOException {
        Path pom = Files.createTempDirectory(TestPaths.getTestFile("target").toPath(), "project-")
                .resolve("pom.xml");
        session.getService(ModelXmlFactory.class).write(model, pom);
        return new TestProject(session, model, pom, true);
    }

    public void setTopProject(boolean topProject) {
        this.topProject = topProject;
    }

    public void setParent(Project parent) {
        this.parent = parent;
    }

    @Override
    public String getGroupId() {
        return effectiveModel.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return effectiveModel.getArtifactId();
    }

    @Override
    public String getVersion() {
        return effectiveModel.getVersion();
    }

    @Override
    public Packaging getPackaging() {
        return session.requirePackaging(effectiveModel.getPackaging());
    }

    @Override
    public ProducedArtifact getPomArtifact() {
        return session.createProducedArtifact(getGroupId(), getArtifactId(), getVersion(), "pom");
    }

    @Override
    public List<ProducedArtifact> getArtifacts() {
        return Collections.singletonList(getPomArtifact());
    }

    @Override
    public Model getModel() {
        return effectiveModel;
    }

    @Override
    public Path getPomPath() {
        return pomPath;
    }

    @Override
    public Path getBasedir() {
        return pomPath == null ? null : pomPath.getParent();
    }

    @Override
    public List<DependencyCoordinates> getDependencies() {
        List<DependencyCoordinates> result = new ArrayList<>();
        for (Dependency dependency : effectiveModel.getDependencies()) {
            org.eclipse.aether.graph.Dependency aetherDependency = new org.eclipse.aether.graph.Dependency(
                    new DefaultArtifact(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getClassifier(),
                            dependency.getType(),
                            dependency.getVersion()),
                    dependency.getScope() == null ? "compile" : dependency.getScope());
            result.add(new DefaultDependencyCoordinates((InternalSession) session, aetherDependency));
        }
        return result;
    }

    @Override
    public List<DependencyCoordinates> getManagedDependencies() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTopProject() {
        return topProject;
    }

    @Override
    public boolean isRootProject() {
        return topProject;
    }

    @Override
    public Path getRootDirectory() {
        return getBasedir();
    }

    @Override
    public Optional<Project> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<Profile> getDeclaredProfiles() {
        return effectiveModel.getProfiles();
    }

    @Override
    public List<Profile> getEffectiveProfiles() {
        return effectiveModel.getProfiles();
    }

    @Override
    public List<Profile> getDeclaredActiveProfiles() {
        return Collections.emptyList();
    }

    @Override
    public List<Profile> getEffectiveActiveProfiles() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return getId();
    }
}
