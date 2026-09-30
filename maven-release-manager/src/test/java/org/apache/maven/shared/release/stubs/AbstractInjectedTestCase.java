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
package org.apache.maven.shared.release.stubs;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.services.Prompter;
import org.apache.maven.di.Injector;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.junit.jupiter.api.BeforeEach;

import static org.apache.maven.shared.release.stubs.TestPaths.getTestFile;
import static org.mockito.Mockito.mock;

/**
 * Base class of tests that get their components injected. Each test has its own Maven 4 API session, with its own
 * local repository, and the dependency injection container of that session fills the {@code @Inject} fields of the
 * test. This replaces {@code @PlexusTest} of plexus-testing.
 */
public abstract class AbstractInjectedTestCase {
    private static int sessionCounter;

    protected ReleaseTestSession testSession;

    @BeforeEach
    void setupInjection() throws Exception {
        Path localRepository;
        synchronized (AbstractInjectedTestCase.class) {
            localRepository =
                    getTestFile("target/local-repository-" + sessionCounter++).toPath();
        }
        Files.createDirectories(localRepository);
        testSession = new ReleaseTestSession(localRepository, injector -> {
            // the standalone runner of maven-impl binds neither of them
            injector.bindInstance(SecDispatcher.class, TestSecDispatcher.create());
            injector.bindInstance(Prompter.class, mock(Prompter.class));
            customizeInjector(injector);
        });
        testSession.getInjector().injectInstance(this);
    }

    /**
     * Hook to bind more components before the container is used, like {@code customizeContainer} of plexus-testing.
     *
     * @param injector the container of the session
     */
    protected void customizeInjector(Injector injector) {}

    /**
     * @return a release environment that carries the session of the test
     */
    protected DefaultReleaseEnvironment newReleaseEnvironment() {
        return new DefaultReleaseEnvironment().setSession(testSession.getSession());
    }
}
