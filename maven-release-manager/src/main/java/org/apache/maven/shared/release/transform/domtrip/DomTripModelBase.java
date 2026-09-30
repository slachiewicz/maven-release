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
import org.apache.maven.shared.release.transform.PomModelBase;

/**
 * DomTrip shared model base logic, common to the {@code project} and the {@code profile} element.
 *
 * @since 3.4
 */
public class DomTripModelBase implements PomModelBase {
    private final Element modelBase;
    private final Editor editor;

    public DomTripModelBase(Element modelBase, Editor editor) {
        this.modelBase = modelBase;
        this.editor = editor;
    }

    @Override
    public PomBuild getBuild() {
        return modelBase
                .childElement("build")
                .map(elm -> (PomBuild) new DomTripBuild(elm, editor))
                .orElse(null);
    }

    @Override
    public List<MavenCoordinate> getDependencies() {
        return dependencies(modelBase);
    }

    @Override
    public List<MavenCoordinate> getManagedDependencies() {
        return modelBase
                .childElement("dependencyManagement")
                .map(this::dependencies)
                .orElse(Collections.emptyList());
    }

    @Override
    public boolean hasDependencyManagement() {
        return modelBase.childElement("dependencyManagement").isPresent();
    }

    @Override
    public List<MavenCoordinate> getReportPlugins() {
        return modelBase
                .childElement("reporting")
                .flatMap(reporting -> reporting.childElement("plugins"))
                .map(plugins -> plugins.childElements("plugin")
                        .map(plugin -> (MavenCoordinate) new DomTripMavenCoordinate(plugin, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public boolean hasReporting() {
        return modelBase.childElement("reporting").isPresent();
    }

    private List<MavenCoordinate> dependencies(Element parent) {
        return parent.childElement("dependencies")
                .map(deps -> deps.childElements("dependency")
                        .map(dep -> (MavenCoordinate) new DomTripMavenCoordinate(dep, editor))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
