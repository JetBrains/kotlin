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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Base class representing a configuration of translator.
 */
public abstract class Config {

    @NotNull
    public static Config getEmptyConfig(@NotNull Project project, @NotNull EcmaVersion ecmaVersion) {
        return new Config(project, ecmaVersion) {
            @NotNull
            @Override
            protected List<JetFile> generateLibFiles() {
                return Collections.emptyList();
            }
        };
    }

    //NOTE: used by mvn build
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public static Config getEmptyConfig(@NotNull Project project) {
        return getEmptyConfig(project, EcmaVersion.defaultVersion());
    }

    @NotNull
    public static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "/core/annotations.kt",
            "/jquery/common.kt",
            "/jquery/ui.kt",
            "/core/javautil.kt",
            "/core/javalang.kt",
            "/core/date.kt",
            "/core/core.kt",
            "/core/math.kt",
            "/core/json.kt",
            "/raphael/raphael.kt",
            "/html5/canvas.kt",
            "/html5/files.kt",
            "/html5/image.kt",
            "/stdlib/JUMaps.kt",
            "/stdlib/browser.kt",
            "/core/dom.kt"
            // TODO
            // "/core/dom/core.kt"
    );

    public static final String LIBRARIES_LOCATION = "js/js.libraries/src";

    @NotNull
    private final Project project;
    @Nullable
    private List<JetFile> libFiles = null;
    @NotNull
    private final EcmaVersion target;

    public Config(@NotNull Project project, @NotNull EcmaVersion ecmaVersion) {
        this.project = project;
        this.target = ecmaVersion;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public EcmaVersion getTarget() {
        return target;
    }

    @NotNull
    protected abstract List<JetFile> generateLibFiles();

    @NotNull
    public final List<JetFile> getLibFiles() {
        if (libFiles == null) {
            libFiles = generateLibFiles();
        }
        return libFiles;
    }
}
