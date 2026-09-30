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

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.di.Injector;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.apache.maven.shared.release.versions.ArtifactKeys;
import org.junit.jupiter.api.BeforeEach;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.util.DocumentBuilderFactoryConfigurer;

import static org.apache.maven.shared.release.stubs.TestPaths.getBasedir;
import static org.apache.maven.shared.release.stubs.TestPaths.getTestFile;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Base class for some release tests. Each test gets its own Maven 4 API session with a fresh local repository and a
 * dependency injection container whose components are injected into the fields of the test.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractReleaseTestCase extends AbstractInjectedTestCase {
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactoryConfigurer.builder()
            // .withDTDParsingDisabled()
            .build()
            .configure(DocumentBuilderFactory.newInstance());

    @Inject
    protected ScmManagerStub scmManager;

    @Override
    protected void customizeInjector(Injector injector) {
        injector.bindImplicit(ScmManagerStub.class);
    }

    @BeforeEach
    void setupAbstractReleaseTestCase() throws Exception {
        testSession.seed(getTestFile("src/test/remote-repository").toPath());
    }

    protected Path getWorkingDirectory(String workingDir) {
        return Paths.get(getBasedir(), "target/test-classes").resolve(Paths.get("projects", workingDir));
    }

    protected List<Project> createReactorProjects(String path, String subpath) throws Exception {
        return createReactorProjects(path, path, subpath);
    }

    protected ReleaseDescriptorBuilder createReleaseDescriptorBuilder(List<Project> reactorProjects) {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        for (Project project : reactorProjects) {
            builder.putOriginalVersion(project.getGroupId() + ':' + project.getArtifactId(), project.getVersion());
        }
        return builder;
    }

    /**
     *
     * @param sourcePath sourceDirectory to copy from
     * @param targetPath targetDirectory to copy to
     * @param executionRoot sub directory of targetPath in case the root pom.xml is not used (e.g. flat projects)
     * @return all Maven projects
     * @throws Exception if any occurs
     */
    protected List<Project> createReactorProjects(String sourcePath, String targetPath, String executionRoot)
            throws Exception {
        final Path testCaseRootFrom =
                Paths.get(getBasedir(), "src/test/resources").resolve(Paths.get("projects", sourcePath));

        final Path testCaseRootTo = getWorkingDirectory(targetPath);

        // Recopy the test resources since they are modified in some tests
        Files.walkFileTree(testCaseRootFrom, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relPath = testCaseRootFrom.relativize(file);

                if (!relPath.toFile().getName().startsWith("expected-")) {
                    Files.createDirectories(testCaseRootTo.resolve(relPath).getParent());

                    Files.copy(file, testCaseRootTo.resolve(relPath), StandardCopyOption.REPLACE_EXISTING);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        Path projectFile;
        if (executionRoot == null) {
            projectFile = testCaseRootTo.resolve("pom.xml");
        } else {
            projectFile = testCaseRootTo.resolve(Paths.get(executionRoot, "pom.xml"));
        }

        return testSession.buildReactor(projectFile);
    }

    protected static Map<String, Project> getProjectsAsMap(List<Project> reactorProjects) {
        Map<String, Project> map = new HashMap<>();
        for (Project project : reactorProjects) {
            map.put(ArtifactKeys.versionlessKey(project.getGroupId(), project.getArtifactId()), project);
        }
        return map;
    }

    protected void comparePomFiles(List<Project> reactorProjects) throws IOException {
        comparePomFiles(reactorProjects, true);
    }

    protected void comparePomFiles(List<Project> reactorProjects, boolean normalizeLineEndings) throws IOException {
        comparePomFiles(reactorProjects, "", normalizeLineEndings);
    }

    protected void comparePomFiles(List<Project> reactorProjects, String expectedFileSuffix) throws IOException {
        comparePomFiles(reactorProjects, expectedFileSuffix, true);
    }

    protected void comparePomFiles(
            List<Project> reactorProjects, String expectedFileSuffix, boolean normalizeLineEndings) throws IOException {
        for (Project project : reactorProjects) {
            comparePomFiles(project, expectedFileSuffix, normalizeLineEndings);
        }
    }

    protected void comparePomFiles(Project project, String expectedFileSuffix) throws IOException {
        comparePomFiles(project, expectedFileSuffix, true);
    }

    protected void comparePomFiles(Project project, String expectedFileSuffix, boolean normalizeLineEndings)
            throws IOException {
        File actualFile = project.getPomPath().toFile();
        File expectedFile = new File(actualFile.getParentFile(), "expected-pom" + expectedFileSuffix + ".xml");

        comparePomFiles(expectedFile, actualFile, normalizeLineEndings, false);
    }

    protected void comparePomFiles(
            File expectedFile, File actualFile, boolean normalizeLineEndings, boolean ignoreComments)
            throws IOException {
        StringBuilder sb = new StringBuilder("Check the transformed POM " + actualFile);
        sb.append(System.lineSeparator());

        final String remoteRepositoryURL = getRemoteRepositoryURL();

        DiffBuilder diffBuilder = DiffBuilder.compare(expectedFile)
                .withDocumentBuilderFactory(DOCUMENT_BUILDER_FACTORY)
                .withTest(actualFile);
        if (normalizeLineEndings) {
            diffBuilder = diffBuilder.normalizeWhitespace();
        }
        if (ignoreComments) {
            diffBuilder.ignoreComments();
        }
        // Order of elements has changed between M2 and M3, so match by name
        diffBuilder
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .checkForSimilar();

        diffBuilder.withDifferenceEvaluator(new DifferenceEvaluator() {
            @Override
            public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
                if ("${remoterepo}".equals(comparison.getControlDetails().getValue())
                        && remoteRepositoryURL.equals(
                                comparison.getTestDetails().getValue())) {
                    return ComparisonResult.EQUAL;
                } else if (outcome == ComparisonResult.DIFFERENT
                        && comparison.getType() == ComparisonType.CHILD_NODELIST_SEQUENCE) {
                    // Order of elements has changed between M2 and M3
                    return ComparisonResult.EQUAL;
                } else if (outcome == ComparisonResult.DIFFERENT
                        && comparison.getType() == ComparisonType.TEXT_VALUE
                        && "${project.build.directory}/site"
                                .equals(comparison.getTestDetails().getValue())) {
                    // M2 was target/site, M3 is ${project.build.directory}/site
                    return ComparisonResult.EQUAL;
                } else {
                    return outcome;
                }
            }
        });

        Diff diff = diffBuilder.build();

        sb.append(diff.toString());

        assertFalse(diff.hasDifferences(), sb.toString());
    }

    private String getRemoteRepositoryURL() throws IOException {
        File testFile = getTestFile("src/test/remote-repository");
        if (testFile.getAbsolutePath().equals(testFile.getCanonicalPath())) {
            return "file://"
                    + getTestFile("src/test/remote-repository")
                            .getAbsolutePath()
                            .replace('\\', '/');
        }
        return "file://"
                + getTestFile("src/test/remote-repository").getCanonicalPath().replace('\\', '/');
    }

    public static String getPath(File file) throws IOException {
        return file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
    }
}
