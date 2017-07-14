/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.facade;

import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult;
import org.jetbrains.kotlin.js.backend.ast.JsImportedModule;
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.coroutine.CoroutineTransformer;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException;
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer;
import org.jetbrains.kotlin.js.inline.JsInliner;
import org.jetbrains.kotlin.js.inline.clean.LabeledBlockToDoWhileTransformation;
import org.jetbrains.kotlin.js.inline.clean.RemoveDuplicateImportsKt;
import org.jetbrains.kotlin.js.inline.clean.RemoveUnusedImportsKt;
import org.jetbrains.kotlin.js.inline.clean.ResolveTemporaryNamesKt;
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver;
import org.jetbrains.kotlin.js.translate.general.AstGenerationResult;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.ExpandIsCallsKt;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil;
import org.jetbrains.kotlin.serialization.js.ast.JsAstSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.diagnostics.DiagnosticUtils.hasError;

/**
 * An entry point of translator.
 */
public final class K2JSTranslator {

    @NotNull
    private final JsConfig config;

    @Nullable
    private final IncrementalResultsConsumer incrementalResults;

    public K2JSTranslator(@NotNull JsConfig config) {
        this.config = config;
        this.incrementalResults = config.getConfiguration().get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER);
    }

    @NotNull
    public TranslationResult translate(
            @NotNull JsConfig.Reporter reporter,
            @NotNull List<KtFile> files,
            @NotNull MainCallParameters mainCallParameters
    ) throws TranslationException {
        return translate(reporter, files, mainCallParameters, null);
    }

    @NotNull
    public TranslationResult translate(
            @NotNull JsConfig.Reporter reporter,
            @NotNull List<KtFile> files,
            @NotNull MainCallParameters mainCallParameters,
            @Nullable JsAnalysisResult analysisResult
    ) throws TranslationException {
        List<TranslationUnit> units = new ArrayList<>();
        for (KtFile file : files) {
            units.add(new TranslationUnit.SourceFile(file));
        }
        return translateUnits(reporter, units, mainCallParameters, analysisResult);
    }

    @NotNull
    public TranslationResult translateUnits(
            @NotNull JsConfig.Reporter reporter,
            @NotNull List<TranslationUnit> units,
            @NotNull MainCallParameters mainCallParameters
    ) throws TranslationException {
        return translateUnits(reporter, units, mainCallParameters, null);
    }

    @NotNull
    public TranslationResult translateUnits(
            @NotNull JsConfig.Reporter reporter,
            @NotNull List<TranslationUnit> units,
            @NotNull MainCallParameters mainCallParameters,
            @Nullable JsAnalysisResult analysisResult
    ) throws TranslationException {
        List<KtFile> files = new ArrayList<>();
        for (TranslationUnit unit : units) {
            if (unit instanceof TranslationUnit.SourceFile) {
                files.add(((TranslationUnit.SourceFile) unit).getFile());
            }
        }

        if (analysisResult == null) {
            analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, config);
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        }

        BindingTrace bindingTrace = analysisResult.getBindingTrace();
        TopDownAnalyzerFacadeForJS.checkForErrors(files, bindingTrace.getBindingContext());
        ModuleDescriptor moduleDescriptor = analysisResult.getModuleDescriptor();
        Diagnostics diagnostics = bindingTrace.getBindingContext().getDiagnostics();

        AstGenerationResult translationResult = Translation.generateAst(bindingTrace, units, mainCallParameters, moduleDescriptor, config);
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        if (hasError(diagnostics)) return new TranslationResult.Fail(diagnostics);

        List<JsProgramFragment> newFragments = new ArrayList<>(translationResult.getNewFragments());
        List<JsProgramFragment> allFragments = new ArrayList<>(translationResult.getFragments());

        JsInliner.process(reporter, config, analysisResult.getBindingTrace(), translationResult.getInnerModuleName(),
                          allFragments, newFragments, translationResult.getImportStatements());

        LabeledBlockToDoWhileTransformation.INSTANCE.apply(newFragments);

        CoroutineTransformer coroutineTransformer = new CoroutineTransformer();
        for (JsProgramFragment fragment : newFragments) {
            coroutineTransformer.accept(fragment.getDeclarationBlock());
            coroutineTransformer.accept(fragment.getInitializerBlock());
        }
        RemoveUnusedImportsKt.removeUnusedImports(translationResult.getProgram());
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        if (hasError(diagnostics)) return new TranslationResult.Fail(diagnostics);

        ExpandIsCallsKt.expandIsCalls(newFragments);
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        List<File> sourceRoots = config.getSourceMapRoots().stream().map(File::new).collect(Collectors.toList());
        SourceFilePathResolver pathResolver = new SourceFilePathResolver(sourceRoots);

        JsAstSerializer serializer = new JsAstSerializer(file -> {
            try {
                return pathResolver.getPathRelativeToSourceRoots(file);
            }
            catch (IOException e) {
                throw new RuntimeException("IO error occurred resolving path to source file", e);
            }
        });

        if (incrementalResults != null) {
            KotlinJavascriptSerializationUtil serializationUtil = KotlinJavascriptSerializationUtil.INSTANCE;

            for (KtFile file : files) {
                JsProgramFragment fragment = translationResult.getFragmentMap().get(file);
                assert fragment != null : "Could not find AST for file: " + file;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                serializer.serialize(fragment, output);
                byte[] binaryAst = output.toByteArray();

                List<DeclarationDescriptor> scope = translationResult.getFileMemberScopes().get(file);
                assert scope != null : "Could not find descriptors for file: " + file;
                ProtoBuf.PackageFragment packagePart = serializationUtil.serializeDescriptors(
                        bindingTrace.getBindingContext(), moduleDescriptor, scope, file.getPackageFqName());

                File ioFile = VfsUtilCore.virtualToIoFile(file.getVirtualFile());
                incrementalResults.processPackagePart(ioFile, packagePart.toByteArray(), binaryAst);
            }

            incrementalResults.processHeader(serializationUtil.serializeHeader(null).toByteArray());
        }

        RemoveDuplicateImportsKt.removeDuplicateImports(translationResult.getProgram());
        ResolveTemporaryNamesKt.resolveTemporaryNames(translationResult.getProgram());
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        if (hasError(diagnostics)) return new TranslationResult.Fail(diagnostics);

        List<String> importedModules = new ArrayList<>();
        for (JsImportedModule module : translationResult.getImportedModuleList()) {
            importedModules.add(module.getExternalName());
        }

        return new TranslationResult.Success(config, files, translationResult.getProgram(), diagnostics, importedModules,
                                             moduleDescriptor, bindingTrace.getBindingContext());
    }
}
