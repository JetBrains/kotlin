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

package org.jetbrains.kotlin.js.facade

import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.coroutine.CoroutineTransformer
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.js.inline.JsInliner
import org.jetbrains.kotlin.js.inline.clean.LabeledBlockToDoWhileTransformation
import org.jetbrains.kotlin.js.inline.clean.*
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ast.JsAstSerializer
import org.jetbrains.kotlin.utils.JsMetadataVersion

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList

import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.hasError

/**
 * An entry point of translator.
 */
class K2JSTranslator(private val config: JsConfig) {

    private val incrementalResults = config.configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)

    @Throws(TranslationException::class)
    @JvmOverloads
    fun translate(
        reporter: JsConfig.Reporter,
        files: List<KtFile>,
        mainCallParameters: MainCallParameters,
        analysisResult: JsAnalysisResult? = null
    ): TranslationResult {
        val units = ArrayList<TranslationUnit>()
        for (file in files) {
            units.add(TranslationUnit.SourceFile(file))
        }
        return translateUnits(reporter, units, mainCallParameters, analysisResult)
    }

    @Throws(TranslationException::class)
    @JvmOverloads
    fun translateUnits(
        reporter: JsConfig.Reporter,
        units: List<TranslationUnit>,
        mainCallParameters: MainCallParameters,
        analysisResult: JsAnalysisResult? = null
    ): TranslationResult {
        var analysisResult = analysisResult
        val files = ArrayList<KtFile>()
        for (unit in units) {
            if (unit is TranslationUnit.SourceFile) {
                files.add(unit.file)
            }
        }

        if (analysisResult == null) {
            analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, config)
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        }

        val bindingTrace = analysisResult.bindingTrace
        TopDownAnalyzerFacadeForJS.checkForErrors(files, bindingTrace.bindingContext)
        val moduleDescriptor = analysisResult.moduleDescriptor
        val diagnostics = bindingTrace.bindingContext.diagnostics

        val pathResolver = SourceFilePathResolver.create(config)

        val translationResult = Translation.generateAst(
            bindingTrace, units, mainCallParameters, moduleDescriptor, config, pathResolver
        )
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        if (hasError(diagnostics)) return TranslationResult.Fail(diagnostics)

        val newFragments = ArrayList(translationResult.newFragments)
        val allFragments = ArrayList(translationResult.fragments)

        JsInliner.process(
            reporter, config, analysisResult.bindingTrace, translationResult.innerModuleName,
            allFragments, newFragments, translationResult.importStatements
        )

        LabeledBlockToDoWhileTransformation.apply(newFragments)

        val coroutineTransformer = CoroutineTransformer()
        for (fragment in newFragments) {
            coroutineTransformer.accept(fragment.declarationBlock)
            coroutineTransformer.accept(fragment.initializerBlock)
        }
        removeUnusedImports(translationResult.program)

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        if (hasError(diagnostics)) return TranslationResult.Fail(diagnostics)

        expandIsCalls(newFragments)
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        val serializer = JsAstSerializer { file ->
            try {
                pathResolver.getPathRelativeToSourceRoots(file)
            } catch (e: IOException) {
                throw RuntimeException("IO error occurred resolving path to source file", e)
            }
        }

        if (incrementalResults != null) {
            val serializationUtil = KotlinJavascriptSerializationUtil

            for (file in files) {
                val fragment = translationResult.fragmentMap[file] ?: error("Could not find AST for file: $file")
                val output = ByteArrayOutputStream()
                serializer.serialize(fragment, output)
                val binaryAst = output.toByteArray()

                val scope = translationResult.fileMemberScopes[file] ?: error("Could not find descriptors for file: $file")
                val metadataVersion = config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                val packagePart = serializationUtil.serializeDescriptors(
                    bindingTrace.bindingContext, moduleDescriptor, scope, file.packageFqName,
                    config.configuration.languageVersionSettings,
                    metadataVersion ?: JsMetadataVersion.INSTANCE
                )

                val ioFile = VfsUtilCore.virtualToIoFile(file.virtualFile)
                incrementalResults.processPackagePart(ioFile, packagePart.toByteArray(), binaryAst)
            }

            val settings = config.configuration.languageVersionSettings
            incrementalResults.processHeader(serializationUtil.serializeHeader(moduleDescriptor, null, settings).toByteArray())
        }

        removeDuplicateImports(translationResult.program)
        translationResult.program.resolveTemporaryNames()

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        if (hasError(diagnostics)) return TranslationResult.Fail(diagnostics)

        val importedModules = ArrayList<String>()
        for (module in translationResult.importedModuleList) {
            importedModules.add(module.externalName)
        }

        return TranslationResult.Success(
            config, files, translationResult.program, diagnostics, importedModules,
            moduleDescriptor, bindingTrace.bindingContext
        )
    }
}
