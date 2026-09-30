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
package org.apache.maven.shared.release.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcherException;

/**
 * A shared utility to access the {@link SecDispatcher} service.
 *
 * @since TBD
 */
@Singleton
@Named
public class MavenCrypto {
    /**
     * Exception thrown when "something" of crypto operation did not succeed. All the code all over the place
     * was catching sec dispatcher and plexus cipher exceptions just to neglect it (maybe log in DEBUG), so
     * this is one single exception here.
     */
    public static class MavenCryptoException extends Exception {
        private MavenCryptoException(String message) {
            super(message);
        }

        private MavenCryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final SecDispatcher secDispatcher;

    @Inject
    public MavenCrypto(SecDispatcher secDispatcher) {
        this.secDispatcher = secDispatcher;
    }

    public String decrypt(String value) throws MavenCryptoException {
        try {
            return secDispatcher.decrypt(value);
        } catch (SecDispatcherException | IOException e) {
            throw new MavenCryptoException("decrypt failed", e);
        }
    }

    public void decryptProperties(Properties properties) throws MavenCryptoException {
        String[] keys = new String[] {"scm.password", "scm.passphrase"};

        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null) {
                properties.put(key, decrypt(value));
            }
        }
    }

    /**
     * Encrypts with the {@code master} dispatcher of the security configuration. Unlike the Maven 3 based
     * implementation this needs a Maven 4 style security configuration: the legacy dispatcher can only decrypt.
     */
    public String encryptAndDecorate(String passwd) throws MavenCryptoException {
        try {
            return secDispatcher.encrypt(
                    passwd, Collections.singletonMap(SecDispatcher.DISPATCHER_NAME_ATTR, "master"));
        } catch (SecDispatcherException | IOException e) {
            throw new MavenCryptoException("encrypt failed", e);
        }
    }

    public boolean isEncryptedString(String str) {
        return secDispatcher.isAnyEncryptedString(str);
    }
}
