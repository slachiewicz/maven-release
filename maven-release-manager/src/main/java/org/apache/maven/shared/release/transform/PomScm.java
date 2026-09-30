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
package org.apache.maven.shared.release.transform;

/**
 * Editable view of the {@code scm} element of a POM. The Maven 4 API model is immutable, so the release manager
 * edits the POM through these small views instead of through {@code org.apache.maven.api.model.Scm}.
 *
 * @since 4.0.0
 */
public interface PomScm {
    /**
     * @return the {@code connection} or {@code null} if absent
     */
    String getConnection();

    /**
     * @param connection the new value, {@code null} removes the element
     */
    void setConnection(String connection);

    /**
     * @return the {@code developerConnection} or {@code null} if absent
     */
    String getDeveloperConnection();

    /**
     * @param developerConnection the new value, {@code null} removes the element
     */
    void setDeveloperConnection(String developerConnection);

    /**
     * @return the {@code url} or {@code null} if absent
     */
    String getUrl();

    /**
     * @param url the new value, {@code null} removes the element
     */
    void setUrl(String url);

    /**
     * @return the {@code tag} or {@code null} if absent
     */
    String getTag();

    /**
     * @param tag the new value, {@code null} removes the element
     */
    void setTag(String tag);
}
