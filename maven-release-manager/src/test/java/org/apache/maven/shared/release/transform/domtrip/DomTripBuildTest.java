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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomTripBuildTest {

    private DomTripBuild createBuild(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripBuild(document.root(), editor);
    }

    @Test
    void testGetExtensionsEmpty() {
        assertNotNull(createBuild("<build></build>").getExtensions());
        assertEquals(0, createBuild("<build></build>").getExtensions().size());
    }

    @Test
    void testGetExtensionsEmptyContainer() {
        assertEquals(
                0, createBuild("<build><extensions/></build>").getExtensions().size());
    }

    @Test
    void testGetExtensionsOne() {
        assertEquals(
                1,
                createBuild("<build><extensions><extension/></extensions></build>")
                        .getExtensions()
                        .size());
    }

    @Test
    void testPluginManagementMissing() {
        assertFalse(createBuild("<build></build>").hasPluginManagement());
        assertEquals(0, createBuild("<build></build>").getManagedPlugins().size());
    }

    @Test
    void testPluginManagementPresent() {
        assertTrue(createBuild("<build><pluginManagement/></build>").hasPluginManagement());
    }

    @Test
    void testGetManagedPluginsOne() {
        assertEquals(
                1,
                createBuild("<build><pluginManagement><plugins><plugin/></plugins></pluginManagement></build>")
                        .getManagedPlugins()
                        .size());
    }

    @Test
    void testPluginDependencies() {
        assertEquals(
                1,
                createBuild("<build><plugins><plugin><dependencies><dependency/></dependencies></plugin></plugins>"
                                + "</build>")
                        .getPlugins()
                        .get(0)
                        .getDependencies()
                        .size());
    }

    @Test
    void testPluginName() {
        assertEquals(
                "plugin",
                createBuild("<build><plugins><plugin/></plugins></build>")
                        .getPlugins()
                        .get(0)
                        .getName());
    }

    @Test
    void testGetPluginsEmpty() {
        assertNotNull(createBuild("<build></build>").getPlugins());
        assertEquals(0, createBuild("<build></build>").getPlugins().size());
    }

    @Test
    void testGetPluginsEmptyContainer() {
        assertEquals(0, createBuild("<build><plugins/></build>").getPlugins().size());
    }

    @Test
    void testGetPluginsOne() {
        assertEquals(
                1,
                createBuild("<build><plugins><plugin/></plugins></build>")
                        .getPlugins()
                        .size());
    }
}
