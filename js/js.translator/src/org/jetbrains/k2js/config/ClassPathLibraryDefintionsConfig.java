/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.config;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.List;

/**
 * A Config implementation which is configured with a directory to find the standard library names from
 */
public class ClassPathLibraryDefintionsConfig extends Config {
    // used by maven build
    @NotNull
    public static final String META_INF_SERVICES_FILE = "META-INF/services/org.jetbrains.kotlin.js.libraryDefinitions";

    public ClassPathLibraryDefintionsConfig(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull EcmaVersion version,
            boolean sourcemap,
            boolean inlineEnabled
    ) {
        super(project, moduleId, version, sourcemap, inlineEnabled);
    }

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        return MetaInfServices.loadServicesFiles(META_INF_SERVICES_FILE, getProject());
    }
}
