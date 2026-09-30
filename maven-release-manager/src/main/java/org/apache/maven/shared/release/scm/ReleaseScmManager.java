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
package org.apache.maven.shared.release.scm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.scm.manager.AbstractScmManager;
import org.apache.maven.scm.provider.ScmProvider;

/**
 * {@link org.apache.maven.scm.manager.ScmManager} for the Maven 4 dependency injection container.
 * <p>
 * The manager of {@code maven-scm-manager-plexus} receives its providers through Sisu map injection. The SCM
 * provider components are indexed for Sisu ({@code META-INF/sisu/javax.inject.Named}), which the Maven 4
 * container does not read, so this manager reads that index itself and instantiates the providers that have a
 * public no-argument constructor.
 *
 * @since 4.0.0
 */
@Singleton
@Named
public class ReleaseScmManager extends AbstractScmManager {
    private static final String SISU_INDEX = "META-INF/sisu/javax.inject.Named";

    private static final String NAMED = "javax.inject.Named";

    @Inject
    public ReleaseScmManager() {
        this(ReleaseScmManager.class.getClassLoader());
    }

    /**
     * Creates a manager with the providers found on the given class loader.
     *
     * @param classLoader where to look for indexed SCM providers
     */
    public ReleaseScmManager(ClassLoader classLoader) {
        Map<String, ScmProvider> providers = discoverProviders(classLoader);
        if (providers.isEmpty()) {
            logger.warn("No SCM providers configured.");
        }
        setScmProviders(providers);
    }

    private Map<String, ScmProvider> discoverProviders(ClassLoader classLoader) {
        Map<String, ScmProvider> providers = new LinkedHashMap<>();
        try {
            Enumeration<URL> indexes = classLoader.getResources(SISU_INDEX);
            while (indexes.hasMoreElements()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(indexes.nextElement().openStream(), StandardCharsets.UTF_8))) {
                    String className;
                    while ((className = reader.readLine()) != null) {
                        register(providers, className.trim(), classLoader);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Cannot read the index of SCM providers: {}", e.getMessage());
        }
        return providers;
    }

    private void register(Map<String, ScmProvider> providers, String className, ClassLoader classLoader) {
        if (className.isEmpty()) {
            return;
        }
        try {
            Class<?> type = Class.forName(className, false, classLoader);
            if (!ScmProvider.class.isAssignableFrom(type)) {
                return;
            }
            String name = namedValue(type);
            if (name != null && !providers.containsKey(name)) {
                providers.put(name, (ScmProvider) type.getDeclaredConstructor().newInstance());
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.debug("Skipping SCM provider {}: {}", className, e.toString());
        }
    }

    /** Reads {@code javax.inject.Named#value()} reflectively, so that this class needs no javax.inject at all. */
    private static String namedValue(Class<?> type) throws ReflectiveOperationException {
        for (Annotation annotation : type.getAnnotations()) {
            if (NAMED.equals(annotation.annotationType().getName())) {
                String value =
                        (String) annotation.annotationType().getMethod("value").invoke(annotation);
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
