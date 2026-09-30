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
package org.apache.maven.shared.release.transform;

import org.apache.maven.api.model.Scm;

/**
 * Plain holder of SCM values, used to describe an {@code scm} element that is to be added to a POM.
 *
 * @since 4.0.0
 */
public class SimplePomScm implements PomScm {
    private String connection;

    private String developerConnection;

    private String url;

    private String tag;

    /** Creates an empty holder; unlike the Maven 3 model the tag does not default to {@code HEAD}. */
    public SimplePomScm() {}

    /**
     * Copies the values of an SCM model.
     *
     * @param scm the values to copy
     */
    public SimplePomScm(Scm scm) {
        this.connection = scm.getConnection();
        this.developerConnection = scm.getDeveloperConnection();
        this.url = scm.getUrl();
        this.tag = scm.getTag();
    }

    @Override
    public String getConnection() {
        return connection;
    }

    @Override
    public void setConnection(String connection) {
        this.connection = connection;
    }

    @Override
    public String getDeveloperConnection() {
        return developerConnection;
    }

    @Override
    public void setDeveloperConnection(String developerConnection) {
        this.developerConnection = developerConnection;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }
}
