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
package org.apache.maven.shared.release.policy.semver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.di.Named;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Maven 4 dependency injection container only knows the classes in
 * {@code META-INF/maven/org.apache.maven.api.di.Inject}. The parent POM switches the annotation processor that
 * would generate it off, so the list is maintained by hand and this test keeps it complete.
 */
class DiIndexTest {
    @Test
    void testIndexListsAllNamedClasses() throws Exception {
        Path classes = Paths.get(SemVerVersionPolicy.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

        Set<String> indexed = Files.readAllLines(
                        classes.resolve("META-INF/maven/org.apache.maven.api.di.Inject"), StandardCharsets.UTF_8)
                .stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> named = new TreeSet<>();
        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".class"))::iterator) {
                String name = classes.relativize(file).toString().replace(File.separatorChar, '.');
                name = name.substring(0, name.length() - ".class".length());
                if (!name.contains("$") && isNamed(name)) {
                    named.add(name);
                }
            }
        }
        assertEquals(named, indexed);
    }

    private boolean isNamed(String className) throws IOException {
        try {
            return Class.forName(className, false, getClass().getClassLoader()).isAnnotationPresent(Named.class);
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IOException(e);
        }
    }
}
