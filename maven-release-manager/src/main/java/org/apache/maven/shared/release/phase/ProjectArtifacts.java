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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.shared.release.versions.ArtifactKeys;

/**
 * What Maven 3 kept on {@code MavenProject} as resolved artifact sets: all artifacts, the direct dependency
 * artifacts, and the artifacts of plugins, reports and extensions. The Maven 4 API only offers
 * {@link Session#collectDependencies(Project, PathScope)}, and its collection root has no plugin, report or
 * extension children, so those come from the declared versions of the effective model.
 */
final class ProjectArtifacts {
    private final Map<String, ResolvedArtifact> artifacts = new LinkedHashMap<>();

    private final List<ResolvedArtifact> dependencyArtifacts = new ArrayList<>();

    private final Map<String, ResolvedArtifact> pluginArtifacts = new LinkedHashMap<>();

    private final Map<String, ResolvedArtifact> reportArtifacts = new LinkedHashMap<>();

    private final Map<String, ResolvedArtifact> extensionArtifacts = new LinkedHashMap<>();

    private ResolvedArtifact parentArtifact;

    private ProjectArtifacts() {}

    static ProjectArtifacts of(Session session, Project project) {
        ProjectArtifacts result = new ProjectArtifacts();

        try {
            // the union of both test scopes covers compile, runtime, provided, system and test dependencies
            for (PathScope scope : new PathScope[] {PathScope.TEST_COMPILE, PathScope.TEST_RUNTIME}) {
                Node root = session.collectDependencies(project, scope);
                for (Node child : root.getChildren()) {
                    ResolvedArtifact direct = ResolvedArtifact.of(child);
                    if (!result.dependencyArtifacts.contains(direct)) {
                        result.dependencyArtifacts.add(direct);
                    }
                }
                root.stream().skip(1).forEach(node -> {
                    ResolvedArtifact artifact = ResolvedArtifact.of(node);
                    result.artifacts.putIfAbsent(artifact.versionlessKey(), artifact);
                });
            }
        } catch (DependencyResolverException e) {
            // Maven 3 built the direct dependency artifacts from the declarations, even if the dependencies could
            // not be resolved, and only left the resolved set empty
            result.dependencyArtifacts.clear();
            result.artifacts.clear();
            for (DependencyCoordinates coordinates : project.getDependencies()) {
                ResolvedArtifact declared = ResolvedArtifact.declared(
                        coordinates.getGroupId(),
                        coordinates.getArtifactId(),
                        coordinates.getType() != null ? coordinates.getType().id() : coordinates.getExtension(),
                        String.valueOf(coordinates.getVersionConstraint()));
                result.dependencyArtifacts.add(new ResolvedArtifact(
                        declared.getGroupId(),
                        declared.getArtifactId(),
                        declared.getType(),
                        coordinates.getClassifier(),
                        coordinates.getScope() != null ? coordinates.getScope().id() : null,
                        declared.getVersion(),
                        declared.getBaseVersion(),
                        declared.isSnapshot()));
            }
        }

        project.getParent()
                .ifPresent(parent -> result.parentArtifact = ResolvedArtifact.declared(
                        parent.getGroupId(), parent.getArtifactId(), "pom", parent.getVersion()));

        Model model = project.getModel();
        Build build = model.getBuild();
        if (build != null) {
            for (Plugin plugin : build.getPlugins()) {
                put(
                        result.pluginArtifacts,
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        "maven-plugin",
                        plugin.getVersion());
            }
            for (Extension extension : build.getExtensions()) {
                put(
                        result.extensionArtifacts,
                        extension.getGroupId(),
                        extension.getArtifactId(),
                        "jar",
                        extension.getVersion());
            }
        }
        Reporting reporting = model.getReporting();
        if (reporting != null) {
            for (ReportPlugin plugin : reporting.getPlugins()) {
                put(
                        result.reportArtifacts,
                        plugin.getGroupId(),
                        plugin.getArtifactId(),
                        "maven-plugin",
                        plugin.getVersion());
            }
        }
        return result;
    }

    private static void put(
            Map<String, ResolvedArtifact> map, String groupId, String artifactId, String type, String version) {
        // a plugin or extension without a version resolves to the latest release, which is never a snapshot
        if (version != null) {
            map.put(
                    ArtifactKeys.versionlessKey(groupId, artifactId),
                    ResolvedArtifact.declared(groupId, artifactId, type, version));
        }
    }

    Map<String, ResolvedArtifact> getArtifacts() {
        return artifacts;
    }

    List<ResolvedArtifact> getDependencyArtifacts() {
        return Collections.unmodifiableList(dependencyArtifacts);
    }

    ResolvedArtifact getParentArtifact() {
        return parentArtifact;
    }

    Map<String, ResolvedArtifact> getPluginArtifactMap() {
        return pluginArtifacts;
    }

    Map<String, ResolvedArtifact> getReportArtifactMap() {
        return reportArtifacts;
    }

    Map<String, ResolvedArtifact> getExtensionArtifactMap() {
        return extensionArtifacts;
    }
}
