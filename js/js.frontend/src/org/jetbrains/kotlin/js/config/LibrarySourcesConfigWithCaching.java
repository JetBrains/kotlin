/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.config;

import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata;
import org.jetbrains.kotlin.utils.PathUtil;

import java.util.Collections;
import java.util.List;

public class LibrarySourcesConfigWithCaching extends LibrarySourcesConfig {
    public static final List<String> JS_STDLIB =
            Collections.singletonList(PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath().getAbsolutePath());

    private static List<KotlinJavascriptMetadata> stdlibMetadata = null;

    private final boolean isUnitTestConfig;

    public LibrarySourcesConfigWithCaching(
            @NotNull Project project,
            @NotNull String moduleId,
            @NotNull EcmaVersion ecmaVersion,
            boolean sourcemap,
            boolean inlineEnabled,
            boolean isUnitTestConfig
    ) {
        super(project, moduleId, JS_STDLIB, ecmaVersion, sourcemap, inlineEnabled);
        this.isUnitTestConfig = isUnitTestConfig;
    }

    @Override
    protected  void init(@NotNull List<JetFile> sourceFilesInLibraries, @NotNull List<KotlinJavascriptMetadata> metadata) {
        if (stdlibMetadata == null) {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            stdlibMetadata = new SmartList<KotlinJavascriptMetadata>();
            super.init(sourceFilesInLibraries, stdlibMetadata);
        }

        metadata.addAll(stdlibMetadata);
    }

    @Override
    public boolean isTestConfig() {
        return isUnitTestConfig;
    }
}
