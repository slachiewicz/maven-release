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

import java.util.Objects;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.shared.release.versions.ArtifactKeys;

/**
 * Coordinates and version of an artifact as resolved for a project. This stands in for the Maven 3
 * {@code org.apache.maven.artifact.Artifact}, which the Maven 4 API does not offer for plugins, extensions and
 * parents, and which is mutable there.
 */
final class ResolvedArtifact implements Comparable<ResolvedArtifact> {
    private final String groupId;

    private final String artifactId;

    private final String type;

    private final String classifier;

    private final String scope;

    private final String version;

    private final String baseVersion;

    private final boolean snapshot;

    @SuppressWarnings("checkstyle:ParameterNumber")
    ResolvedArtifact(
            String groupId,
            String artifactId,
            String type,
            String classifier,
            String scope,
            String version,
            String baseVersion,
            boolean snapshot) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.classifier = classifier == null || classifier.isEmpty() ? null : classifier;
        this.scope = scope;
        this.version = version;
        this.baseVersion = baseVersion;
        this.snapshot = snapshot;
    }

    /**
     * Creates the artifact for a coordinate that only has a declared version, such as a parent, plugin or
     * extension.
     */
    static ResolvedArtifact declared(String groupId, String artifactId, String type, String version) {
        String baseVersion = version;
        if (version != null) {
            java.util.regex.Matcher matcher = ArtifactKeys.VERSION_FILE_PATTERN.matcher(version);
            if (matcher.matches()) {
                baseVersion = matcher.group(1) + '-' + ArtifactKeys.SNAPSHOT_VERSION;
            }
        }
        return new ResolvedArtifact(
                groupId, artifactId, type, null, null, version, baseVersion, ArtifactKeys.isSnapshot(version));
    }

    static ResolvedArtifact of(Node node) {
        Dependency dependency = node.getDependency();
        if (dependency != null) {
            return new ResolvedArtifact(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getType() != null ? dependency.getType().id() : dependency.getExtension(),
                    dependency.getClassifier(),
                    dependency.getScope() != null ? dependency.getScope().id() : null,
                    String.valueOf(dependency.getVersion()),
                    String.valueOf(dependency.getBaseVersion()),
                    dependency.isSnapshot());
        }
        Artifact artifact = node.getArtifact();
        return new ResolvedArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getExtension(),
                artifact.getClassifier(),
                null,
                String.valueOf(artifact.getVersion()),
                String.valueOf(artifact.getBaseVersion()),
                artifact.isSnapshot());
    }

    String getGroupId() {
        return groupId;
    }

    String getArtifactId() {
        return artifactId;
    }

    String getType() {
        return type;
    }

    String getClassifier() {
        return classifier;
    }

    String getScope() {
        return scope;
    }

    String getVersion() {
        return version;
    }

    String getBaseVersion() {
        return baseVersion;
    }

    boolean isSnapshot() {
        return snapshot;
    }

    String versionlessKey() {
        return ArtifactKeys.versionlessKey(groupId, artifactId);
    }

    @Override
    public int compareTo(ResolvedArtifact other) {
        int result = groupId.compareTo(other.groupId);
        if (result == 0) {
            result = artifactId.compareTo(other.artifactId);
        }
        if (result == 0) {
            result = Objects.toString(type, "").compareTo(Objects.toString(other.type, ""));
        }
        if (result == 0) {
            result = Objects.toString(classifier, "").compareTo(Objects.toString(other.classifier, ""));
        }
        if (result == 0) {
            result = Objects.toString(version, "").compareTo(Objects.toString(other.version, ""));
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolvedArtifact)) {
            return false;
        }
        ResolvedArtifact that = (ResolvedArtifact) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(type, that.type)
                && Objects.equals(classifier, that.classifier)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, type, classifier, version);
    }

    /** Same format as the Maven 3 artifact: {@code group:artifact:type[:classifier]:baseVersion[:scope]}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(':').append(artifactId).append(':').append(type);
        if (classifier != null) {
            sb.append(':').append(classifier);
        }
        sb.append(':').append(baseVersion);
        if (scope != null) {
            sb.append(':').append(scope);
        }
        return sb.toString();
    }
}
