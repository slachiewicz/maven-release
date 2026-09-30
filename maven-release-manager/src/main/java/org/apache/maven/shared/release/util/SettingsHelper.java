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

import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;

/**
 * Lookup helpers that the Maven 3 {@code Settings} class carried itself. The Maven 4 API settings model is a
 * plain immutable value type without an id index.
 */
public final class SettingsHelper {
    private SettingsHelper() {}

    /**
     * Finds a server by id.
     *
     * @param settings the settings, must not be {@code null}
     * @param id the server id
     * @return the first server with the id or {@code null}
     */
    public static Server getServer(Settings settings, String id) {
        for (Server server : settings.getServers()) {
            if (server.getId() != null && server.getId().equals(id)) {
                return server;
            }
        }
        return null;
    }
}
