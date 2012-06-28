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
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.k2js.config.Config;

import java.util.ArrayList;
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
        checkForErrors(withJsLibAdded(files, config), bindingContext);
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

    @NotNull
    public static AnalyzeExhaust analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely, @NotNull Config config,
            boolean storeContextForBodiesResolve) {
        Project project = config.getProject();
        BindingTraceContext bindingTraceContext = new BindingTraceContext();

        final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                completely, false, false, Collections.<AnalyzerScriptParameter>emptyList());

        InjectorForTopDownAnalyzerForJs injector = new InjectorForTopDownAnalyzerForJs(
                project, topDownAnalysisParameters, new ObservableBindingTrace(bindingTraceContext), owner,
                JsConfiguration.jsLibConfiguration(project));
        try {
            injector.getTopDownAnalyzer().analyzeFiles(withJsLibAdded(files, config), Collections.<AnalyzerScriptParameter>emptyList());
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), bodiesResolveContext);
        }
        finally {
            injector.destroy();
        }
    }

    public static AnalyzeExhaust analyzeBodiesInFiles(
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull Config config,
            @NotNull BindingTrace traceContext,
            @NotNull BodiesResolveContext bodiesResolveContext) {
        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                config.getProject(), Collections.<AnalyzerScriptParameter>emptyList(), completely, traceContext, bodiesResolveContext);
    }

    private static void checkForErrors(@NotNull Collection<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }

    @NotNull
    public static Collection<JetFile> withJsLibAdded(@NotNull Collection<JetFile> files, @NotNull Config config) {
        Collection<JetFile> allFiles = new ArrayList<JetFile>();
        allFiles.addAll(files);
        allFiles.addAll(config.getLibFiles());
        return allFiles;
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

    private static final class JsConfiguration implements ModuleConfiguration {

        @NotNull
        private final Project project;

        public static JsConfiguration jsLibConfiguration(@NotNull Project project) {
            return new JsConfiguration(project);
        }

        private JsConfiguration(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
            //TODO: these things should not be hard-coded like that
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath("js.*")));
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath("java.lang.*")));
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath(JetStandardClasses.STANDARD_CLASSES_FQNAME, true)));
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath("kotlin.*")));
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor,
                @NotNull WritableScope namespaceMemberScope) {
            DefaultModuleConfiguration.createStandardConfiguration(project, true)
                    .extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
        }
    }
}
