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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.js.inline.JsInliner
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
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.backend.ast.JsStatement
import org.jetbrains.kotlin.js.coroutine.transformCoroutines
import org.jetbrains.kotlin.js.inline.util.collectDefinedNamesInAllScopes
import org.jetbrains.kotlin.js.translate.general.AstGenerationResult
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.serialization.js.ast.JsAstProtoBuf

/**
 * An entry point of translator.
 */
class K2JSTranslator @JvmOverloads constructor(
    private val config: JsConfig,
    private val shouldValidateJsAst: Boolean = false
) {

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
        val files = ArrayList<KtFile>()
        for (unit in units) {
            if (unit is TranslationUnit.SourceFile) {
                files.add(unit.file)
            }
        }

        val actualAnalysisResult = analysisResult ?: TopDownAnalyzerFacadeForJS.analyzeFiles(files, config)

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return translate(reporter, files, units, mainCallParameters, actualAnalysisResult)
    }

    @Throws(TranslationException::class)
    private fun translate(
        reporter: JsConfig.Reporter,
        files: List<KtFile>,
        allUnits: List<TranslationUnit>,
        mainCallParameters: MainCallParameters,
        analysisResult: JsAnalysisResult
    ): TranslationResult {
        val bindingTrace = analysisResult.bindingTrace
        TopDownAnalyzerFacadeForJS.checkForErrors(files, bindingTrace.bindingContext)
        val moduleDescriptor = analysisResult.moduleDescriptor
        val diagnostics = bindingTrace.bindingContext.diagnostics
        val pathResolver = SourceFilePathResolver.create(config)

        val translationResult = Translation.generateAst(bindingTrace, allUnits, mainCallParameters, moduleDescriptor, config, pathResolver)
        if (hasError(diagnostics)) return TranslationResult.Fail(diagnostics)
        checkCanceled()

        JsInliner(
            reporter,
            config,
            analysisResult.bindingTrace,
            translationResult
        ).process()
        if (hasError(diagnostics)) return TranslationResult.Fail(diagnostics)
        checkCanceled()

        transformLabeledBlockToDoWhile(translationResult.newFragments)
        checkCanceled()

        transformCoroutines(translationResult.newFragments)
        checkCanceled()

        expandIsCalls(translationResult.newFragments)
        checkCanceled()

        trySaveIncrementalData(translationResult, pathResolver, bindingTrace, moduleDescriptor)
        checkCanceled()

        // Global phases

        val (program, importedModules) = translationResult.buildProgram()

        program.resolveTemporaryNames()
        checkCanceled()

        return if (hasError(diagnostics)) {
            TranslationResult.Fail(diagnostics)
        } else {
            TranslationResult.Success(
                config,
                files,
                program,
                diagnostics,
                importedModules,
                moduleDescriptor,
                bindingTrace.bindingContext
            )
        }
    }

    private fun checkCanceled() {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
    }

    private fun trySaveIncrementalData(
        translationResult: AstGenerationResult,
        pathResolver: SourceFilePathResolver,
        bindingTrace: BindingTrace,
        moduleDescriptor: ModuleDescriptor
    ) {
        // TODO Maybe switch validation on for recompile
        if (incrementalResults == null && !shouldValidateJsAst) return

        val serializer = JsAstSerializer(if (shouldValidateJsAst) ::validateJsAst else null) { file ->
            try {
                pathResolver.getPathRelativeToSourceRoots(file)
            } catch (e: IOException) {
                throw RuntimeException("IO error occurred resolving path to source file", e)
            }
        }

        for ((sourceUnit, fileTranslationResult) in translationResult.translatedSourceFiles) {
            val file = sourceUnit.file
            val fragment = fileTranslationResult.fragment
            val output = ByteArrayOutputStream()
            serializer.serialize(fragment, output)
            val binaryAst = output.toByteArray()

            val scope = fileTranslationResult.memberScope
            val metadataVersion = config.configuration.get(CommonConfigurationKeys.METADATA_VERSION)
            val packagePart = KotlinJavascriptSerializationUtil.serializeDescriptors(
                bindingTrace.bindingContext, moduleDescriptor, scope, file.packageFqName,
                config.configuration.languageVersionSettings,
                metadataVersion ?: JsMetadataVersion.INSTANCE
            )

            val inlineData = serializeInlineData(fileTranslationResult.inlineFunctionTags)

            val ioFile = VfsUtilCore.virtualToIoFile(file.virtualFile)
            incrementalResults?.processPackagePart(ioFile, packagePart.toByteArray(), binaryAst, inlineData)
        }

        val settings = config.configuration.languageVersionSettings
        incrementalResults?.processHeader(KotlinJavascriptSerializationUtil.serializeHeader(moduleDescriptor, null, settings).toByteArray())
    }

    private fun serializeInlineData(importedTags: Set<String>): ByteArray {
        val output = ByteArrayOutputStream()
        val inlineDataBuilder = JsAstProtoBuf.InlineData.newBuilder()
        inlineDataBuilder.addAllInlineFunctionTags(importedTags)
        inlineDataBuilder.build().writeTo(output)
        return output.toByteArray()
    }

    // Checks that all non-temporary serialized JsName's are either declared locally, or linked via a NameBinding
    private fun validateJsAst(fragment: JsProgramFragment, serializedNames: Set<JsName>) {
        val knownNames = mutableSetOf<JsName>().apply {
            fragment.nameBindings.mapTo(this) { it.name }
            fragment.importedModules.mapTo(this) { it.internalName }
        }

        val allCode = JsBlock(mutableListOf<JsStatement>().apply {
            add(fragment.declarationBlock)
            add(fragment.exportBlock)
            add(fragment.initializerBlock)
            fragment.tests?.let { add(it) }
            fragment.mainFunction?.let { add(it) }
            addAll(fragment.inlinedLocalDeclarations.values)
        })

        val definedNames = collectDefinedNamesInAllScopes(allCode)

        serializedNames.forEach {
            assert(!it.isTemporary || it in definedNames || it in knownNames) { "JsName ${it.ident} is unbound" }
        }
    }
}
