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
package org.apache.maven.shared.release.transform.domtrip;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.apache.maven.shared.release.transform.PomPlugin;

/**
 * DomTrip implementation of poms PLUGIN element.
 *
 * @since 3.4
 */
public class DomTripPlugin implements PomPlugin {
    private final Element plugin;
    private final Editor editor;
    private final MavenCoordinate coordinate;

    public DomTripPlugin(Element plugin, Editor editor) {
        this.plugin = plugin;
        this.editor = editor;
        this.coordinate = new DomTripMavenCoordinate(plugin, editor);
    }

    @Override
    public String getGroupId() {
        return coordinate.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return coordinate.getArtifactId();
    }

    @Override
    public String getVersion() {
        return coordinate.getVersion();
    }

    @Override
    public void setVersion(String version) {
        coordinate.setVersion(version);
    }

    @Override
    public List<MavenCoordinate> getDependencies() {
        return plugin.childElement("dependencies")
                .map(deps -> deps.childElements("dependency")
                        .map(dep -> (MavenCoordinate) new DomTripMavenCoordinate(dep, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public String getName() {
        return "plugin";
    }
}
