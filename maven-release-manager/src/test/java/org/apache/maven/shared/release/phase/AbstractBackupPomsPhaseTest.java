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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.stubs.TestProject;

/**
 * @author Edwin Punzalan
 */
abstract class AbstractBackupPomsPhaseTest extends AbstractInjectedTestCase {
    private final String pomFilename = "pom.xml";

    protected final String releaseBackupSuffix = ".releaseBackup";

    protected List<Project> getReactorProjects(String projectPath) throws Exception {
        List<Project> reactorProjects = new ArrayList<>();

        File pomFile = new File(projectPath, pomFilename);

        Project mainProject = createMavenProject(pomFile);

        reactorProjects.add(mainProject);

        for (String module : mainProject.getModel().getModules()) {
            File modulePom = new File(projectPath + "/" + module, pomFilename);

            Project subproject = createMavenProject(modulePom);

            reactorProjects.add(subproject);
        }

        return reactorProjects;
    }

    private Project createMavenProject(File pomFile) throws IOException {
        Model model = testSession.getSession().getService(ModelXmlFactory.class).read(pomFile.toPath());
        return TestProject.of(model, pomFile.toPath());
    }
}
