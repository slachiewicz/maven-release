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
package org.apache.maven.shared.release.exec;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.apache.maven.shared.release.stubs.TestPaths.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class InvokerMavenExecutorTest extends AbstractInjectedTestCase {

    @Inject
    private MavenCrypto mavenCrypto;

    @Inject
    private SecDispatcher secDispatcher;

    private final SettingsXmlFactory settingsXmlFactory = mock(SettingsXmlFactory.class);

    @Test
    void testEncryptSettings() throws Exception {
        InvokerMavenExecutor executor = new InvokerMavenExecutor(mavenCrypto, settingsXmlFactory);

        // prepare
        File workingDirectory = getTestFile("target/working-directory");
        workingDirectory.mkdirs();

        Settings settings = Settings.newBuilder()
                .servers(Collections.singletonList(Server.newBuilder()
                        .passphrase("server_passphrase")
                        .password("server_password")
                        .build()))
                .proxies(Collections.singletonList(
                        Proxy.newBuilder().password("proxy_password").build()))
                .build();

        DefaultReleaseEnvironment releaseEnvironment = newReleaseEnvironment();
        releaseEnvironment.setSettings(settings);
        releaseEnvironment.setMavenHome(new File(System.getProperty("injectedMavenHome")));

        InvokerMavenExecutor executorSpy = spy(executor);
        ArgumentCaptor<Settings> encryptedSettings = ArgumentCaptor.forClass(Settings.class);

        try {
            executorSpy.executeGoals(
                    workingDirectory, "validate", releaseEnvironment, false, null, null, new ReleaseResult());
        } catch (MavenExecutorException e) {
        }

        verify(settingsXmlFactory).write(encryptedSettings.capture(), isA(Path.class));

        assertNotSame(settings, encryptedSettings.getValue());

        Server encryptedServer = encryptedSettings.getValue().getServers().get(0);
        assertEquals("server_passphrase", secDispatcher.decrypt(encryptedServer.getPassphrase()));
        assertEquals("server_password", secDispatcher.decrypt(encryptedServer.getPassword()));

        Proxy encryptedProxy = encryptedSettings.getValue().getProxies().get(0);
        assertEquals("proxy_password", secDispatcher.decrypt(encryptedProxy.getPassword()));

        File settingsSecurity = new File(System.getProperty("user.home"), ".m2/settings-security.xml");
        if (settingsSecurity.exists()) {
            assertNotEquals("server_passphrase", encryptedServer.getPassphrase());
            assertNotEquals("server_password", encryptedServer.getPassword());
            assertNotEquals("proxy_password", encryptedProxy.getPassword());
        }
    }
}
