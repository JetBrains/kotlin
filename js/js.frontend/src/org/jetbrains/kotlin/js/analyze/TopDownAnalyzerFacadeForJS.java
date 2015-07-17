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

package org.jetbrains.kotlin.js.analyze;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.descriptors.ModuleParameters;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.frontend.js.di.DiPackage;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class TopDownAnalyzerFacadeForJS {
    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.annotation.*"),
            new ImportPath("kotlin.js.*")
    );

    public static ModuleParameters JS_MODULE_PARAMETERS = new ModuleParameters() {
        @NotNull
        @Override
        public List<ImportPath> getDefaultImports() {
            return DEFAULT_IMPORTS;
        }

        @NotNull
        @Override
        public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
            return PlatformToKotlinClassMap.EMPTY;
        }
    };

    private TopDownAnalyzerFacadeForJS() {
    }

    @NotNull
    public static JsAnalysisResult analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Config config
    ) {
        BindingTrace trace = new BindingTraceContext();

        MutableModuleContext newModuleContext = ContextPackage.ContextForNewModule(
                config.getProject(), Name.special("<" + config.getModuleId() + ">"), JS_MODULE_PARAMETERS
        );
        newModuleContext.setDependencies(computeDependencies(newModuleContext.getModule(), config));
        return analyzeFilesWithGivenTrace(files, trace, newModuleContext, config);
    }

    @NotNull
    private static List<ModuleDescriptorImpl> computeDependencies(ModuleDescriptorImpl module, @NotNull Config config) {
        List<ModuleDescriptorImpl> allDependencies = new ArrayList<ModuleDescriptorImpl>();
        allDependencies.add(module);
        allDependencies.addAll(config.getModuleDescriptors());
        allDependencies.add(KotlinBuiltIns.getInstance().getBuiltInsModule());
        return allDependencies;
    }

    @NotNull
    public static JsAnalysisResult analyzeFilesWithGivenTrace(
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull ModuleContext moduleContext,
            @NotNull Config config
    ) {
        Collection<JetFile> allFiles = Config.withJsLibAdded(files, config);

        LazyTopDownAnalyzerForTopLevel analyzerForJs = DiPackage.createTopDownAnalyzerForJs(
                moduleContext, trace,
                new FileBasedDeclarationProviderFactory(moduleContext.getStorageManager(), allFiles)
        );
        analyzerForJs.analyzeFiles(TopDownAnalysisMode.TopLevelDeclarations, files, Collections.<PackageFragmentProvider>emptyList());
        return JsAnalysisResult.success(trace, moduleContext.getModule());
    }

    public static void checkForErrors(@NotNull Collection<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }
}
