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

package org.jetbrains.k2js.test.config;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.k2js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.LibrarySourcesConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibrarySourcesConfigWithCaching extends LibrarySourcesConfig {
    public static final List<String> JS_STDLIB =
            Arrays.asList("@" + STDLIB_JS_MODULE_NAME, PathUtil.getKotlinPathsForDistDirectory().getJsLibJarPath().getAbsolutePath());

    private static List<JetFile> jsLibFiles;
    private static AnalyzeExhaust exhaust;

    private BindingContext libraryContext;
    private ModuleDescriptor libraryModule;

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

    @NotNull
    @Override
    public List<JetFile> generateLibFiles() {
        if (jsLibFiles == null) {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            jsLibFiles = super.generateLibFiles();
        }
        return jsLibFiles;
    }



    @Nullable
    @Override
    public ModuleDescriptor getLibraryModule() {
        if (libraryModule == null) {
            libraryModule = getExhaust().getModuleDescriptor();
        }

        return libraryModule;
    }

    @Nullable
    @Override
    public BindingContext getLibraryContext() {
        if (libraryContext == null) {
            //TODO check errors?
            // TopDownAnalyzerFacadeForJS.checkForErrors(allLibFiles, exhaust.getBindingContext());
            libraryContext = getExhaust().getBindingContext();
        }

        return libraryContext;
    }

    @Override
    public boolean isTestConfig() {
        return isUnitTestConfig;
    }

    private AnalyzeExhaust getExhaust() {
        if (exhaust == null) {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            exhaust = TopDownAnalyzerFacadeForJS.analyzeFiles(
                    generateLibFiles(),
                    Predicates.<PsiFile>alwaysFalse(),
                    createConfigWithoutLibFiles(getProject(), getModuleId(), getTarget())
            );
        }

        return exhaust;
    }

    @NotNull
    private static Config createConfigWithoutLibFiles(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion) {
        return new Config(project, moduleId, ecmaVersion, /* generate sourcemaps = */ false, /* inlineEnabled = */ false) {
            @NotNull
            @Override
            protected List<JetFile> generateLibFiles() {
                return Collections.emptyList();
            }
        };
    }
}
