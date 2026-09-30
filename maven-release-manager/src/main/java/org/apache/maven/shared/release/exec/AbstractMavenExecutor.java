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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.util.MavenCrypto;
import org.apache.maven.shared.release.util.MavenCrypto.MavenCryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * <p>Abstract AbstractMavenExecutor class.</p>
 */
public abstract class AbstractMavenExecutor implements MavenExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MavenCrypto mavenCrypto;

    private final SettingsXmlFactory settingsXmlFactory;

    protected AbstractMavenExecutor(MavenCrypto mavenCrypto, SettingsXmlFactory settingsXmlFactory) {
        this.mavenCrypto = requireNonNull(mavenCrypto);
        this.settingsXmlFactory = requireNonNull(settingsXmlFactory);
    }

    @Override
    public void executeGoals(
            File workingDirectory,
            String goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult result)
            throws MavenExecutorException {
        List<String> goalsList = new ArrayList<>();
        if (goals != null) {
            // accept both space and comma, so the old way still work
            // also accept line separators, so that goal lists can be spread
            // across multiple lines in the POM.
            Collections.addAll(goalsList, StringUtils.split(goals, ", \n\r\t"));
        }
        executeGoals(
                workingDirectory, goalsList, releaseEnvironment, interactive, additionalArguments, pomFileName, result);
    }

    protected abstract void executeGoals(
            File workingDirectory,
            List<String> goals,
            ReleaseEnvironment releaseEnvironment,
            boolean interactive,
            String additionalArguments,
            String pomFileName,
            ReleaseResult result)
            throws MavenExecutorException;

    /**
     * <p>Getter for the field <code>logger</code>.</p>
     *
     * @return a {@link Logger} object
     */
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * <p>encryptSettings.</p>
     *
     * @param settings a {@link org.apache.maven.api.settings.Settings} object
     * @return a copy of the settings with encrypted passwords and passphrases
     */
    protected Settings encryptSettings(Settings settings) {
        List<Server> servers = new ArrayList<>();
        for (Server server : settings.getServers()) {
            servers.add(server.with()
                    .password(encrypt(server.getPassword()))
                    .passphrase(encrypt(server.getPassphrase()))
                    .build());
        }

        List<Proxy> proxies = new ArrayList<>();
        for (Proxy proxy : settings.getProxies()) {
            proxies.add(proxy.withPassword(encrypt(proxy.getPassword())));
        }

        return settings.with().servers(servers).proxies(proxies).build();
    }

    private String encrypt(String value) {
        if (value != null && !mavenCrypto.isEncryptedString(value)) {
            try {
                return mavenCrypto.encryptAndDecorate(value);
            } catch (MavenCryptoException e) {
                // ignore
            }
        }
        return value;
    }

    /**
     * <p>getSettingsXmlFactory.</p>
     *
     * @return the factory that writes the settings handed to the forked build
     */
    protected SettingsXmlFactory getSettingsXmlFactory() {
        return settingsXmlFactory;
    }
}
