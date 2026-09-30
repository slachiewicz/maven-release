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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.DefaultSettingsXmlFactory;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.stubs.AbstractInjectedTestCase;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.apache.maven.shared.release.stubs.TestPaths.getTestFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test the forked Maven executor.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
class ForkedMavenExecutorTest extends AbstractInjectedTestCase {

    @Inject
    private MavenCrypto mavenCrypto;

    @Inject
    private SecDispatcher secDispatcher;

    private final SettingsXmlFactory settingsXmlFactory = mock(SettingsXmlFactory.class);

    @Test
    void testExecution() throws Exception {
        // prepare
        File workingDirectory = getTestFile("target/working-directory");
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(0);

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        Arg valueArgument = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(valueArgument);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(isA(String.class) /*"mvn"*/))
                .thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

        // execute
        executor.executeGoals(
                workingDirectory,
                "clean integration-test",
                newReleaseEnvironment(),
                false,
                null,
                null,
                new ReleaseResult());

        // verify
        verify(mockProcess).getInputStream();
        verify(mockProcess).getErrorStream();
        verify(mockProcess).getOutputStream();
        verify(mockProcess).waitFor();
        verify(commandLineMock).setWorkingDirectory(workingDirectory.getAbsolutePath());
        verify(commandLineMock).addEnvironment("MAVEN_TERMINATE_CMD", "on");
        verify(commandLineMock).execute();
        verify(commandLineMock, times(3)).createArg();
        verify(valueArgument).setValue("clean");
        verify(valueArgument).setValue("integration-test");
        verify(valueArgument).setValue("--batch-mode");
        verify(commandLineFactoryMock).createCommandLine(endsWith("mvn"));

        verifyNoMoreInteractions(mockProcess, commandLineFactoryMock, commandLineMock, valueArgument);
    }

    @Test
    void testExecutionWithCustomPomFile() throws Exception {
        File workingDirectory = getTestFile("target/working-directory");
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(0);

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        Arg argMock = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(argMock);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(isA(String.class) /* "mvn" */))
                .thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

        // execute
        executor.executeGoals(
                workingDirectory,
                "clean integration-test",
                newReleaseEnvironment(),
                false,
                null,
                "my-pom.xml",
                new ReleaseResult());
        // verify
        verify(mockProcess).getInputStream();
        verify(mockProcess).getErrorStream();
        verify(mockProcess).getOutputStream();
        verify(mockProcess).waitFor();
        verify(commandLineMock).setWorkingDirectory(workingDirectory.getAbsolutePath());
        verify(commandLineMock).addEnvironment("MAVEN_TERMINATE_CMD", "on");
        verify(commandLineMock).execute();
        verify(commandLineMock, times(5)).createArg();
        verify(argMock).setValue("clean");
        verify(argMock).setValue("integration-test");
        verify(argMock).setValue("-f");
        verify(argMock).setValue("my-pom.xml");
        verify(argMock).setValue("--batch-mode");
        verify(commandLineFactoryMock).createCommandLine(endsWith("mvn"));

        verifyNoMoreInteractions(mockProcess, commandLineMock, argMock, commandLineFactoryMock);
    }

    @Test
    void testExecutionWithArguments() throws Exception {
        File workingDirectory = getTestFile("target/working-directory");
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(0);

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        Arg argMock = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(argMock);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(endsWith("mvn"))).thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

        // execute
        String arguments = "-DperformRelease=true -Dmaven.test.skip=true";
        executor.executeGoals(
                workingDirectory,
                "clean integration-test",
                newReleaseEnvironment(),
                false,
                arguments,
                null,
                new ReleaseResult());

        // verify
        verify(mockProcess).getInputStream();
        verify(mockProcess).getErrorStream();
        verify(mockProcess).getOutputStream();
        verify(mockProcess).waitFor();
        verify(commandLineMock).setWorkingDirectory(workingDirectory.getAbsolutePath());
        verify(commandLineMock).addEnvironment("MAVEN_TERMINATE_CMD", "on");
        verify(commandLineMock).execute();
        verify(commandLineMock, times(4)).createArg();
        verify(argMock).setValue("clean");
        verify(argMock).setValue("integration-test");
        verify(argMock).setValue("--batch-mode");
        verify(argMock).setLine("-DperformRelease=true -Dmaven.test.skip=true");
        verify(commandLineFactoryMock).createCommandLine(endsWith("mvn"));

        verifyNoMoreInteractions(mockProcess, commandLineMock, argMock, commandLineFactoryMock);
    }

    @Test
    void testExecutionWithNonZeroExitCode() throws Exception {
        // prepare
        File workingDirectory = getTestFile("target/working-directory");
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(1);
        when(mockProcess.exitValue()).thenReturn(1); // why was this here in the original test?

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        Arg argMock = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(argMock);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(endsWith("mvn"))).thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

        // execute
        try {
            executor.executeGoals(
                    workingDirectory,
                    "clean integration-test",
                    newReleaseEnvironment(),
                    false,
                    null,
                    null,
                    new ReleaseResult());

            fail("Should have thrown an exception");
        } catch (MavenExecutorException e) {
            assertEquals(1, e.getExitCode(), "Check exit code");
        }

        // verify
        verify(mockProcess).getInputStream();
        verify(mockProcess).getErrorStream();
        verify(mockProcess).getOutputStream();
        verify(mockProcess).waitFor();
        //        verify( mockProcess ).exitValue();
        verify(commandLineMock).setWorkingDirectory(workingDirectory.getAbsolutePath());
        verify(commandLineMock).addEnvironment("MAVEN_TERMINATE_CMD", "on");
        verify(commandLineMock).execute();
        verify(commandLineMock, times(3)).createArg();
        verify(argMock).setValue("clean");
        verify(argMock).setValue("integration-test");
        verify(argMock).setValue("--batch-mode");
        verify(commandLineFactoryMock).createCommandLine(endsWith("mvn"));

        verifyNoMoreInteractions(mockProcess, commandLineMock, argMock, commandLineFactoryMock);
    }

    @Test
    void testExecutionWithCommandLineException() throws Exception {
        // prepare
        File workingDirectory = getTestFile("target/working-directory");

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenThrow(new CommandLineException("..."));

        Arg argMock = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(argMock);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(endsWith("mvn"))).thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

        // execute
        try {
            executor.executeGoals(
                    workingDirectory,
                    "clean integration-test",
                    newReleaseEnvironment(),
                    false,
                    null,
                    null,
                    new ReleaseResult());

            fail("Should have thrown an exception");
        } catch (MavenExecutorException e) {
            assertEquals(CommandLineException.class, e.getCause().getClass(), "Check cause");
        }

        // verify
        verify(commandLineMock).setWorkingDirectory(workingDirectory.getAbsolutePath());
        verify(commandLineMock).addEnvironment("MAVEN_TERMINATE_CMD", "on");
        verify(commandLineMock).execute();
        verify(commandLineMock, times(3)).createArg();
        verify(argMock).setValue("clean");
        verify(argMock).setValue("integration-test");
        verify(argMock).setValue("--batch-mode");
        verify(commandLineFactoryMock).createCommandLine(endsWith("mvn"));

        verifyNoMoreInteractions(commandLineMock, argMock, commandLineFactoryMock);
    }

    /**
     * The settings file is written in UTF-8 whatever the platform default charset is; the surefire configuration
     * runs this module with a non-UTF-8 {@code file.encoding}.
     */
    @Test
    void testSettingsFileIsWrittenInUtf8() throws Exception {
        String nonAscii = "p\u00e4ssw\u00f6rd-\u20ac-\u4e2d";

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(0);

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        // the settings file is deleted when the execution ends, so read it while the argument is being set
        AtomicReference<byte[]> written = new AtomicReference<>();
        Arg argMock = mock(Arg.class);
        doAnswer(invocation -> {
                    written.set(
                            Files.readAllBytes(invocation.<File>getArgument(0).toPath()));
                    return null;
                })
                .when(argMock)
                .setFile(any(File.class));
        when(commandLineMock.createArg()).thenReturn(argMock);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(isA(String.class))).thenReturn(commandLineMock);

        ForkedMavenExecutor executor =
                new ForkedMavenExecutor(mavenCrypto, new DefaultSettingsXmlFactory(), commandLineFactoryMock);

        DefaultReleaseEnvironment releaseEnvironment = newReleaseEnvironment();
        releaseEnvironment.setSettings(
                Settings.newBuilder().localRepository(nonAscii).build());

        executor.executeGoals(
                getTestFile("target/working-directory"),
                "validate",
                releaseEnvironment,
                false,
                null,
                null,
                new ReleaseResult());

        String xml = new String(written.get(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("<localRepository>" + nonAscii + "</localRepository>"), xml);
    }

    @Test
    void testEncryptSettings() throws Exception {
        // prepare
        File workingDirectory = getTestFile("target/working-directory");
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getErrorStream()).thenReturn(mock(InputStream.class));
        when(mockProcess.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(mockProcess.waitFor()).thenReturn(0);

        Commandline commandLineMock = mock(Commandline.class);
        when(commandLineMock.execute()).thenReturn(mockProcess);

        Arg valueArgument = mock(Arg.class);
        when(commandLineMock.createArg()).thenReturn(valueArgument);

        CommandLineFactory commandLineFactoryMock = mock(CommandLineFactory.class);
        when(commandLineFactoryMock.createCommandLine(isA(String.class) /* "mvn" */))
                .thenReturn(commandLineMock);

        ForkedMavenExecutor executor = new ForkedMavenExecutor(mavenCrypto, settingsXmlFactory, commandLineFactoryMock);

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

        AbstractMavenExecutor executorSpy = spy(executor);
        ArgumentCaptor<Settings> encryptedSettings = ArgumentCaptor.forClass(Settings.class);

        executorSpy.executeGoals(
                workingDirectory, "validate", releaseEnvironment, false, null, null, new ReleaseResult());

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
