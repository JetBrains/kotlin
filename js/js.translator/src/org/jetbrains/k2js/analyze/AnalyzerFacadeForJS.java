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

package org.jetbrains.k2js.analyze;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacadeForEverything;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJs;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.config.Config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeForJS {

    private AnalyzerFacadeForJS() {
    }

    @NotNull
    public static BindingContext analyzeFilesAndCheckErrors(@NotNull List<JetFile> files,
            @NotNull Config config) {
        BindingContext bindingContext = analyzeFiles(files, Predicates.<PsiFile>alwaysTrue(), config).getBindingContext();
        checkForErrors(Config.withJsLibAdded(files, config), bindingContext);
        return bindingContext;
    }


    //NOTE: web demo related method
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public static BindingContext analyzeFiles(@NotNull Collection<JetFile> files, @NotNull Config config) {
        return analyzeFiles(files, Predicates.<PsiFile>alwaysTrue(), config).getBindingContext();
    }

    @NotNull
    public static AnalyzeExhaust analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely, @NotNull Config config) {
        return analyzeFiles(files, filesToAnalyzeCompletely, config, false);
    }

    //TODO: refactor
    @NotNull
    public static AnalyzeExhaust analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely, @NotNull Config config,
            boolean storeContextForBodiesResolve) {
        Project project = config.getProject();

        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                completely, false, false, Collections.<AnalyzerScriptParameter>emptyList());

        BindingContext libraryBindingContext = config.getLibraryBindingContext();
        BindingTrace trace = libraryBindingContext == null ?
                             new ObservableBindingTrace(new BindingTraceContext()) :
                             new DelegatingBindingTrace(libraryBindingContext);
        InjectorForTopDownAnalyzerForJs injector = new InjectorForTopDownAnalyzerForJs(
                project, topDownAnalysisParameters, trace, owner,
                new JsConfiguration(project, libraryBindingContext));
        try {
            Collection<JetFile> allFiles = libraryBindingContext != null ?
                                           files :
                                           Config.withJsLibAdded(files, config);
            injector.getTopDownAnalyzer().analyzeFiles(allFiles, Collections.<AnalyzerScriptParameter>emptyList());
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(trace.getBindingContext(), bodiesResolveContext);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static AnalyzeExhaust analyzeBodiesInFiles(
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull Config config,
            @NotNull BindingTrace traceContext,
            @NotNull BodiesResolveContext bodiesResolveContext) {
        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                config.getProject(), Collections.<AnalyzerScriptParameter>emptyList(), completely, traceContext, bodiesResolveContext);
    }

    public static void checkForErrors(@NotNull Collection<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }

    @NotNull
    private static Predicate<PsiFile> notLibFiles(@NotNull final List<JetFile> jsLibFiles) {
        return new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                assert file instanceof JetFile;
                @SuppressWarnings("UnnecessaryLocalVariable") boolean notLibFile = !jsLibFiles.contains(file);
                return notLibFile;
            }
        };
    }
}
