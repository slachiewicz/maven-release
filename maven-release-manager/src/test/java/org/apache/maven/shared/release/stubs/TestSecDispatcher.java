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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.components.secdispatcher.Cipher;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.MasterSource;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.cipher.AESGCMNoPadding;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.MasterDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.sources.EnvMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.SystemPropertyMasterSource;

/**
 * A {@link SecDispatcher} for tests. The standalone runner of maven-impl binds the dispatchers but not the
 * {@code SecDispatcher} itself, so it is assembled here the way {@code SecDispatcherProvider} of maven-testing does.
 * The security configuration of the master dispatcher is {@code settings-security-master.xml}; legacy encrypted values are decrypted with the master of the file that {@code settings.security} names.
 */
public final class TestSecDispatcher {
    private TestSecDispatcher() {}

    public static SecDispatcher create() {
        Map<String, Cipher> ciphers = new HashMap<>();
        ciphers.put("AES/GCM/NoPadding", new AESGCMNoPadding());
        Map<String, MasterSource> sources = new HashMap<>();
        sources.put("env", new EnvMasterSource());
        sources.put("system-property", new SystemPropertyMasterSource());
        Map<String, Dispatcher> dispatchers = new HashMap<>();
        dispatchers.put("legacy", new LegacyDispatcher());
        dispatchers.put("master", new MasterDispatcher(ciphers, sources));
        return new DefaultSecDispatcher(
                dispatchers,
                TestPaths.getTestFile("target/test-classes/settings-security-master.xml")
                        .toPath());
    }
}
