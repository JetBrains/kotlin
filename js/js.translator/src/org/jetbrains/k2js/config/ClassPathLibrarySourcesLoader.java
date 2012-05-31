/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import java.util.*;

/**
 * A helper class to load the kotlin library sources to be compiled to JavaScript as part of a JavaScript build
 */
public class ClassPathLibrarySourcesLoader {
    public static final String META_INF_SERVICES_FILE = "META-INF/services/org.jetbrains.kotlin.js.librarySource";
    @NotNull private final Project project;

    public ClassPathLibrarySourcesLoader(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public List<JetFile> findSourceFiles() {
        return MetaInfServices.loadServicesFiles(META_INF_SERVICES_FILE, project);
    }
}
