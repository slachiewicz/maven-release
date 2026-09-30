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
import eu.maveniverse.domtrip.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The coordinate view serves the {@code dependency}, {@code extension}, report {@code plugin} and {@code parent}
 * elements, which Maven 3 modelled with one class each.
 */
class DomTripMavenCoordinateTest {

    private DomTripMavenCoordinate createCoordinate(String xml) {
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        return new DomTripMavenCoordinate(document.root(), editor);
    }

    @Test
    void testGetArtifactIdMissing() {
        assertNull(createCoordinate("<dependency></dependency>").getArtifactId());
    }

    @Test
    void testGetArtifactIdPresent() {
        assertEquals(
                "ARTIFACTID",
                createCoordinate("<dependency><artifactId>ARTIFACTID</artifactId></dependency>")
                        .getArtifactId());
    }

    @Test
    void testGetGroupIdMissing() {
        assertNull(createCoordinate("<extension></extension>").getGroupId());
    }

    @Test
    void testGetGroupIdPresent() {
        assertEquals(
                "GROUPID",
                createCoordinate("<extension><groupId>GROUPID</groupId></extension>")
                        .getGroupId());
    }

    @Test
    void testGetVersionMissing() {
        assertNull(createCoordinate("<dependency></dependency>").getVersion());
    }

    @Test
    void testGetVersionPresent() {
        assertEquals(
                "VERSION",
                createCoordinate("<dependency><version>VERSION</version></dependency>")
                        .getVersion());
    }

    @Test
    void testGetParentVersion() {
        assertEquals(
                "1.0",
                createCoordinate("<parent><version>1.0</version></parent>").getVersion());
    }

    @Test
    void testSetVersion() {
        String xml = "<dependency><version>OLD_VERSION</version></dependency>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        new DomTripMavenCoordinate(root, editor).setVersion("NEW_VERSION");
        assertEquals("NEW_VERSION", root.childTextTrimmed("version"));
    }

    @Test
    void testSetVersionMissingIsNotAdded() {
        String xml = "<dependency></dependency>";
        Document document = Document.of(xml);
        Editor editor = new Editor(document);
        Element root = document.root();
        new DomTripMavenCoordinate(root, editor).setVersion("NEW_VERSION");
        assertNull(root.childTextTrimmed("version"));
    }

    @Test
    void testGetNameIsTheElementName() {
        assertEquals("dependency", createCoordinate("<dependency></dependency>").getName());
        assertEquals("extension", createCoordinate("<extension></extension>").getName());
        assertEquals("parent", createCoordinate("<parent></parent>").getName());
    }
}
