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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContextImpl;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForTopDownAnalyzerForJs;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class TopDownAnalyzerFacadeForJS {
    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.js.*")
    );

    private TopDownAnalyzerFacadeForJS() {
    }

    //TODO: refactor
    @NotNull
    public static JsAnalysisResult analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Config config
    ) {
        BindingTrace trace = new BindingTraceContext();

        ModuleDescriptorImpl module = createJsModule("<" + config.getModuleId() + ">");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());

        for(ModuleDescriptorImpl moduleDescriptor : config.getModuleDescriptors()) {
            module.addDependencyOnModule(moduleDescriptor);
        }

        module.seal();

        return analyzeFilesWithGivenTrace(files, trace, module, config);
    }

    @NotNull
    public static JsAnalysisResult analyzeFilesWithGivenTrace(
            @NotNull Collection<JetFile> files,
            @NotNull BindingTrace trace,
            @NotNull ModuleDescriptorImpl module,
            @NotNull Config config
    ) {
        Project project = config.getProject();

        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(), globalContext.getExceptionTracker(), false, false);

        Collection<JetFile> allFiles = Config.withJsLibAdded(files, config);

        InjectorForTopDownAnalyzerForJs injector = new InjectorForTopDownAnalyzerForJs(
                project, topDownAnalysisParameters, trace, module,
                new FileBasedDeclarationProviderFactory(topDownAnalysisParameters.getStorageManager(), allFiles));
        try {
            injector.getLazyTopDownAnalyzerForTopLevel().analyzeFiles(topDownAnalysisParameters, files,
                                                           Collections.<PackageFragmentProvider>emptyList());
            return JsAnalysisResult.success(trace, module);
        }
        finally {
            injector.destroy();
        }
    }

    public static void checkForErrors(@NotNull Collection<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJsModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, PlatformToKotlinClassMap.EMPTY);
    }
}
