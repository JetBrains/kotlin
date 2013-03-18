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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacadeForEverything;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJs;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.config.Config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class AnalyzerFacadeForJS {

    public static final PlatformToKotlinClassMap JS_CLASS_MAP = PlatformToKotlinClassMap.EMPTY;

    private AnalyzerFacadeForJS() {
    }

    @NotNull
    public static AnalyzeExhaust analyzeFilesAndCheckErrors(@NotNull List<JetFile> files,
            @NotNull Config config) {
        AnalyzeExhaust exhaust = analyzeFiles(files, Predicates.<PsiFile>alwaysTrue(), config);
        checkForErrors(Config.withJsLibAdded(files, config), exhaust.getBindingContext());
        return exhaust;
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

        MutableModuleDescriptor module = new MutableModuleDescriptor(Name.special("<module>"), JS_CLASS_MAP);
        MutableSubModuleDescriptor subModule = new MutableSubModuleDescriptor(module, Name.special("<sub-module>"));
        module.addSubModule(subModule);
        MutableModuleSourcesManager sourcesManager = new MutableModuleSourcesManager(project);
        for (JetFile file : files) {
            VirtualFile virtualFile = file.getVirtualFile();
            assert virtualFile != null : "No virtual file for " + file;
            sourcesManager.registerRoot(subModule, PackageFragmentKind.SOURCE, virtualFile);
        }

        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(
                completely, false, false, Collections.<AnalyzerScriptParameter>emptyList());

        BindingContext libraryBindingContext = config.getLibraryBindingContext();
        BindingTrace trace = libraryBindingContext == null ?
                             new ObservableBindingTrace(new BindingTraceContext()) :
                             new DelegatingBindingTrace(libraryBindingContext, "trace for analyzing library in js");
        InjectorForTopDownAnalyzerForJs injector = new InjectorForTopDownAnalyzerForJs(
                project, topDownAnalysisParameters, trace, sourcesManager,
                new JsConfiguration(libraryBindingContext));
        try {
            Collection<JetFile> allFiles = libraryBindingContext != null ?
                                           files :
                                           Config.withJsLibAdded(files, config);
            injector.getTopDownAnalyzer().analyzeFiles(allFiles, Collections.<AnalyzerScriptParameter>emptyList());
            BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ?
                                                        new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) :
                                                        null;
            return AnalyzeExhaust.success(trace.getBindingContext(), bodiesResolveContext, sourcesManager);
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
            @NotNull BodiesResolveContext bodiesResolveContext,
            @NotNull ModuleSourcesManager moduleSourcesManager) {
        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(
                config.getProject(), Collections.<AnalyzerScriptParameter>emptyList(), completely, traceContext, bodiesResolveContext,
                moduleSourcesManager);
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

    @NotNull
    public static ResolveSession getLazyResolveSession(Collection<JetFile> files, final Config config) {
        LockBasedStorageManager storageManager = new LockBasedStorageManager();
        FileBasedDeclarationProviderFactory declarationProviderFactory = new FileBasedDeclarationProviderFactory(
                storageManager, Config.withJsLibAdded(files, config), Predicates.<FqName>alwaysFalse());
        ModuleDescriptor lazyModule = new MutableModuleDescriptor(Name.special("<lazy module>"), JS_CLASS_MAP);
        return new ResolveSession(config.getProject(), storageManager, lazyModule, new JsConfiguration(null), declarationProviderFactory);
    }
}
