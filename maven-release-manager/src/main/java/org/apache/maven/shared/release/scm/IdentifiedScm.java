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

import org.apache.maven.api.model.Scm;

/**
 * Unlike repositories the scm of the Project Model doesn't have an id.
 * This makes it quite hard to bind it to the credentials of a server as specified in the settings.xml.
 * <p>
 * The Maven 4 {@link Scm} is immutable, so the id is fixed at construction time and there is no setter.
 *
 * @author Robert Scholte
 * @since 2.3
 */
public class IdentifiedScm extends Scm {

    private final String id;

    /**
     * Creates an identified copy of the given values.
     *
     * @param scm the SCM values to copy
     * @param id the id of the server that holds the credentials, may be {@code null}
     */
    public IdentifiedScm(Scm scm, String id) {
        super(scm.with());
        this.id = id;
    }

    /**
     * <p>Getter for the field <code>id</code>.</p>
     *
     * @return the id
     */
    public String getId() {
        return id;
    }
}
