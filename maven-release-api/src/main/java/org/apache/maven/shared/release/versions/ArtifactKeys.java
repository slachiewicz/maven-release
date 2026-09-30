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
package org.apache.maven.shared.release.versions;

import java.util.regex.Pattern;

/**
 * Small helpers for coordinates that Maven 3 offered as {@code org.apache.maven.artifact.ArtifactUtils}.
 * The Maven 4 API has no static equivalent: {@code VersionParser.isSnapshot} needs a service instance.
 */
public final class ArtifactKeys {
    /** Suffix that marks a snapshot version. */
    public static final String SNAPSHOT_VERSION = "SNAPSHOT";

    /** Matches the timestamped form of a snapshot version, group 1 being the base version. */
    public static final Pattern VERSION_FILE_PATTERN = Pattern.compile("^(.*)-([0-9]{8}\\.[0-9]{6})-([0-9]+)$");

    private ArtifactKeys() {}

    /**
     * Builds the {@code groupId:artifactId} key.
     *
     * @param groupId the group id
     * @param artifactId the artifact id
     * @return the versionless key
     */
    public static String versionlessKey(String groupId, String artifactId) {
        if (groupId == null) {
            throw new NullPointerException("groupId is null");
        }
        if (artifactId == null) {
            throw new NullPointerException("artifactId is null");
        }
        return groupId + ':' + artifactId;
    }

    /**
     * Tells whether the version is a snapshot, either literally or in its timestamped form.
     *
     * @param version the version, may be {@code null}
     * @return {@code true} for {@code 1.0-SNAPSHOT} and {@code 1.0-20260101.120000-1}
     */
    public static boolean isSnapshot(String version) {
        // MRELEASE-623: SNAPSHOT is case-insensitive
        return version != null
                && (version.regionMatches(
                                true,
                                version.length() - SNAPSHOT_VERSION.length(),
                                SNAPSHOT_VERSION,
                                0,
                                SNAPSHOT_VERSION.length())
                        || VERSION_FILE_PATTERN.matcher(version).matches());
    }
}
