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

import java.util.List;
import java.util.Properties;

/**
 * Editable view of the {@code project} element of a POM. It replaces the Maven 3 {@code Model} class that the
 * DomTrip implementation used to subclass; the Maven 4 model is immutable and cannot be edited in place.
 *
 * @since 4.0.0
 */
public interface PomModel extends PomModelBase {
    @Override
    PomBuild getBuild();

    /**
     * @return the {@code parent} element or {@code null} if absent
     */
    MavenCoordinate getParent();

    /**
     * @return the profiles, never {@code null}
     */
    List<PomProfile> getProfiles();

    /**
     * @return the {@code properties} element or {@code null} if absent; writes go to the document
     */
    Properties getProperties();

    /**
     * @return the {@code scm} element or {@code null} if absent
     */
    PomScm getScm();

    /**
     * Replaces the {@code scm} element.
     *
     * @param scm the values to write, {@code null} removes the element
     */
    void setScm(PomScm scm);

    /**
     * Sets the version of the project, adding the element when the version differs from the inherited one.
     *
     * @param version the new version
     */
    void setVersion(String version);
}
