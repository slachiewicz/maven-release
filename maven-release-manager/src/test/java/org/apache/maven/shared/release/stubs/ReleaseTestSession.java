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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.api.services.Sources;
import org.apache.maven.di.Injector;
import org.apache.maven.impl.standalone.ApiRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * A Maven 4 API session for tests, built with the standalone runner of maven-impl. It comes with what the release
 * tests used to get from the Maven 3 project builder: a local repository seeded from
 * {@code src/test/remote-repository} (there is no transporter in standalone mode, so the artifacts are marked as
 * downloaded from {@code central}), and projects built from POM files with the model builder of maven-impl.
 */
public class ReleaseTestSession {
    private final Session session;

    private final Injector injector;

    private final Path localRepository;

    private final Map<Path, TestProject> projects = new HashMap<>();

    public ReleaseTestSession(Path localRepository, Consumer<Injector> customizer) {
        this.localRepository = localRepository;
        Injector[] holder = new Injector[1];
        Session[] sessionHolder = new Session[1];
        ProjectManager projectManager = mock(ProjectManager.class);
        lenient()
                .when(projectManager.getRemoteProjectRepositories(any(Project.class)))
                .thenAnswer(invocation -> sessionHolder[0].getRemoteRepositories());
        this.session = ApiRunner.createSession(
                injector -> {
                    holder[0] = injector;
                    injector.bindInstance(ProjectManager.class, projectManager);
                    customizer.accept(injector);
                },
                localRepository);
        sessionHolder[0] = session;
        this.injector = holder[0];
    }

    public Session getSession() {
        return session;
    }

    public Injector getInjector() {
        return injector;
    }

    public Path getLocalRepository() {
        return localRepository;
    }

    /**
     * Copies the fixture repository into the local repository and marks the artifacts as downloaded.
     *
     * @param remoteRepository the fixture repository
     */
    public void seed(Path remoteRepository) throws IOException {
        try (Stream<Path> files = Files.walk(remoteRepository)) {
            for (Path source : (Iterable<Path>) files::iterator) {
                Path target = localRepository.resolve(
                        remoteRepository.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                    continue;
                }
                String name = source.getFileName().toString();
                if (name.equals("maven-metadata.xml")) {
                    target = target.resolveSibling("maven-metadata-central.xml");
                }
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                if (!name.startsWith("maven-metadata")) {
                    Path marker = target.resolveSibling("_remote.repositories");
                    String existing = Files.exists(marker) ? Files.readString(marker) : "#\n";
                    Files.write(
                            marker, (existing + target.getFileName() + ">central=\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    /**
     * Builds the projects of a reactor: the project of the POM and, recursively, its modules. The result is sorted
     * like the Maven 3 project sorter did, dependencies first. All reactor artifacts are installed as empty files in
     * the local repository, so that dependencies between them resolve.
     */
    public List<Project> buildReactor(Path rootPom) throws IOException, ModelBuilderException {
        List<TestProject> reactor = new ArrayList<>();
        collect(rootPom.toRealPath(), true, reactor);

        Map<String, TestProject> byId = new LinkedHashMap<>();
        for (TestProject project : reactor) {
            byId.put(key(project.getGroupId(), project.getArtifactId(), project.getVersion()), project);
        }

        List<Project> sorted = new ArrayList<>();
        java.util.Set<TestProject> visited = new java.util.HashSet<>();
        for (TestProject project : reactor) {
            visit(project, byId, visited, sorted);
        }

        for (TestProject project : reactor) {
            install(project);
        }
        return sorted;
    }

    private void collect(Path pom, boolean top, List<TestProject> reactor) throws IOException, ModelBuilderException {
        TestProject project = build(pom, top);
        if (reactor.contains(project)) {
            return;
        }
        reactor.add(project);
        List<String> modules = new ArrayList<>(project.getModel().getModules());
        modules.addAll(project.getModel().getSubprojects());
        for (String module : modules) {
            Path modulePath = project.getBasedir().resolve(module);
            Path modulePom = Files.isDirectory(modulePath) ? modulePath.resolve("pom.xml") : modulePath;
            collect(modulePom.toRealPath(), false, reactor);
        }
    }

    /**
     * Builds a single project and, through its parent element, the projects of its parents.
     */
    public TestProject build(Path pom, boolean top) throws IOException, ModelBuilderException {
        Path key = pom.toRealPath();
        TestProject cached = projects.get(key);
        if (cached != null) {
            return cached;
        }
        ModelBuilderResult result = session.getService(ModelBuilder.class)
                .newSession()
                .build(ModelBuilderRequest.builder()
                        .session(session)
                        .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                        .source(Sources.buildSource(key))
                        .build());
        TestProject project = new TestProject(session, result.getEffectiveModel(), key, top);
        projects.put(key, project);

        org.apache.maven.api.model.Parent parent = result.getEffectiveModel().getParent();
        if (parent != null) {
            Path relative = key.getParent()
                    .resolve(parent.getRelativePath() != null ? parent.getRelativePath() : "../pom.xml")
                    .normalize();
            if (Files.isDirectory(relative)) {
                relative = relative.resolve("pom.xml");
            }
            Path parentPom = null;
            if (Files.isRegularFile(relative)) {
                Model candidate = session.getService(org.apache.maven.api.services.xml.ModelXmlFactory.class)
                        .read(relative, false);
                String candidateGroupId = candidate.getGroupId() != null
                        ? candidate.getGroupId()
                        : candidate.getParent() != null ? candidate.getParent().getGroupId() : null;
                if (parent.getArtifactId().equals(candidate.getArtifactId())
                        && parent.getGroupId().equals(candidateGroupId)) {
                    parentPom = relative;
                }
            }
            if (parentPom == null) {
                Path inRepository = localRepository
                        .resolve(parent.getGroupId().replace('.', '/'))
                        .resolve(parent.getArtifactId())
                        .resolve(parent.getVersion())
                        .resolve(parent.getArtifactId() + '-' + parent.getVersion() + ".pom");
                if (Files.isRegularFile(inRepository)) {
                    parentPom = inRepository;
                }
            }
            if (parentPom != null) {
                project.setParent(build(parentPom, false));
            }
        }
        return project;
    }

    private void visit(
            TestProject project,
            Map<String, TestProject> byId,
            java.util.Set<TestProject> visited,
            List<Project> sorted) {
        if (!visited.add(project)) {
            return;
        }
        for (String id : referencedIds(project.getModel())) {
            TestProject referenced = byId.get(id);
            if (referenced != null && referenced != project) {
                visit(referenced, byId, visited, sorted);
            }
        }
        sorted.add(project);
    }

    private static List<String> referencedIds(Model model) {
        List<String> ids = new ArrayList<>();
        if (model.getParent() != null) {
            ids.add(key(
                    model.getParent().getGroupId(),
                    model.getParent().getArtifactId(),
                    model.getParent().getVersion()));
        }
        for (Dependency dependency : model.getDependencies()) {
            ids.add(key(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
        }
        if (model.getBuild() != null) {
            for (Plugin plugin : model.getBuild().getPlugins()) {
                ids.add(key(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion()));
                for (Dependency dependency : plugin.getDependencies()) {
                    ids.add(key(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
                }
            }
            model.getBuild()
                    .getExtensions()
                    .forEach(e -> ids.add(key(e.getGroupId(), e.getArtifactId(), e.getVersion())));
        }
        if (model.getReporting() != null) {
            for (ReportPlugin plugin : model.getReporting().getPlugins()) {
                ids.add(key(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion()));
            }
        }
        return ids;
    }

    private static String key(String groupId, String artifactId, String version) {
        return groupId + ':' + artifactId + ':' + version;
    }

    private void install(TestProject project) throws IOException {
        Path directory = localRepository
                .resolve(project.getGroupId().replace('.', '/'))
                .resolve(project.getArtifactId())
                .resolve(project.getVersion());
        Files.createDirectories(directory);
        String base = project.getArtifactId() + '-' + project.getVersion();
        Files.copy(project.getPomPath(), directory.resolve(base + ".pom"), StandardCopyOption.REPLACE_EXISTING);
        // the content does not matter
        try {
            Files.copy(project.getPomPath(), directory.resolve(base + ".jar"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
