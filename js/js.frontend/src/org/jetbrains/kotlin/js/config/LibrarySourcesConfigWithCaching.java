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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibrarySourcesConfigWithCaching extends LibrarySourcesConfig {
    public static final List<String> JS_STDLIB =
            Arrays.asList(PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath().getAbsolutePath());

    private static List<JetFile> jsLibFiles;
    private static AnalysisResult result;

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
            libraryModule = getResult().getModuleDescriptor();
        }

        return libraryModule;
    }

    @Nullable
    @Override
    public BindingContext getLibraryContext() {
        if (libraryContext == null) {
            //TODO check errors?
            // TopDownAnalyzerFacadeForJS.checkForErrors(allLibFiles, result.getBindingContext());
            libraryContext = getResult().getBindingContext();
        }

        return libraryContext;
    }

    @Override
    public boolean isTestConfig() {
        return isUnitTestConfig;
    }

    @Override
    protected JetFile getJetFileByVirtualFile(VirtualFile file, String moduleName, PsiManager psiManager) {
        JetFile jetFile;
        try {
            String text = StringUtil.convertLineSeparators(new String(file.contentsToByteArray(false), file.getCharset()));
            jetFile = new JetPsiFactory(getProject()).createPhysicalFile(file.getName(), text);
        }
        catch (IOException e) {
            JetFile jetFileByVirtualFile = super.getJetFileByVirtualFile(file, moduleName, psiManager);
            jetFile = new JetPsiFactory(getProject()).createPhysicalFile(file.getPath(), jetFileByVirtualFile.getText());
        }

        setupPsiFile(jetFile, moduleName);
        return jetFile;
    }

    private AnalysisResult getResult() {
        if (result == null) {
            //noinspection AssignmentToStaticFieldFromInstanceMethod
            result = TopDownAnalyzerFacadeForJS.analyzeFiles(
                    generateLibFiles(),
                    createConfigWithoutLibFiles(getProject(), getModuleId(), getTarget())
            );
        }

        return result;
    }

    @NotNull
    private static Config createConfigWithoutLibFiles(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion) {
        return new Config(project, moduleId, ecmaVersion, /* generate sourcemaps = */ false, /* inlineEnabled = */ false) {
            @NotNull
            @Override
            protected List<JetFile> generateLibFiles() {
                return Collections.emptyList();
            }
            @Override
            public boolean checkLibFilesAndReportErrors(@NotNull Function1<String, Unit> report) {
                return false;
            }
        };
    }
}
