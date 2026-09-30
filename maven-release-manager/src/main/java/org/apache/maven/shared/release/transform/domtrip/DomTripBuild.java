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
import org.apache.maven.shared.release.transform.PomBuild;
import org.apache.maven.shared.release.transform.PomPlugin;

/**
 * DomTrip implementation of poms BUILD element.
 *
 * @since 3.4
 */
public class DomTripBuild implements PomBuild {
    private final Element build;
    private final Editor editor;

    public DomTripBuild(Element build, Editor editor) {
        this.build = build;
        this.editor = editor;
    }

    @Override
    public List<MavenCoordinate> getExtensions() {
        return build.childElement("extensions")
                .map(exts -> exts.childElements("extension")
                        .map(ext -> (MavenCoordinate) new DomTripMavenCoordinate(ext, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<PomPlugin> getPlugins() {
        return plugins(build);
    }

    @Override
    public List<PomPlugin> getManagedPlugins() {
        return build.childElement("pluginManagement").map(this::plugins).orElse(Collections.emptyList());
    }

    @Override
    public boolean hasPluginManagement() {
        return build.childElement("pluginManagement").isPresent();
    }

    private List<PomPlugin> plugins(Element parent) {
        return parent.childElement("plugins")
                .map(plugins -> plugins.childElements("plugin")
                        .map(plugin -> (PomPlugin) new DomTripPlugin(plugin, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
