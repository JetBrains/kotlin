/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.incremental.web.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.web.analyzer.WebAnalysisResult
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.utils.JsClassicIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsClassicIncrementalDataProvider
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendFacade
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.service
import java.io.File

class ClassicJsBackendFacade(
    testServices: TestServices,
    val incrementalCompilationEnabled: Boolean
) : ClassicBackendFacade<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js) {
    companion object {
        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"

        fun wrapWithModuleEmulationMarkers(content: String, moduleKind: ModuleKind, moduleId: String): String {
            val escapedModuleId = StringUtil.escapeStringCharacters(moduleId)

            return when (moduleKind) {
                ModuleKind.COMMON_JS -> "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                        "$content\n" +
                        "$KOTLIN_TEST_INTERNAL.endModule(\"$escapedModuleId\");"

                ModuleKind.AMD, ModuleKind.UMD ->
                    "if (typeof $KOTLIN_TEST_INTERNAL !== \"undefined\") { " +
                            "$KOTLIN_TEST_INTERNAL.setModuleId(\"$escapedModuleId\"); }\n" +
                            "$content\n"

                ModuleKind.PLAIN, ModuleKind.ES -> content
            }
        }
    }

    constructor(testServices: TestServices) : this(testServices, incrementalCompilationEnabled = false)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsClassicIncrementalDataProvider))

    override fun transform(module: TestModule, inputArtifact: ClassicBackendInput): BinaryArtifacts.Js {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val (psiFiles, analysisResult, project, _) = inputArtifact

        // TODO how to reuse this config from frontend
        val jsConfig = JsEnvironmentConfigurator.createJsConfig(project, configuration)

        val unitsByPath: MutableMap<String, TranslationUnit> = psiFiles.associateTo(mutableMapOf()) {
            (it.virtualFile.canonicalPath ?: "") to TranslationUnit.SourceFile(it)
        }
        if (incrementalCompilationEnabled) {
            val incrementalData = testServices.jsClassicIncrementalDataProvider.getIncrementalData(module)
            for ((file, data) in incrementalData.translatedFiles) {
                unitsByPath[file.canonicalPath] = TranslationUnit.BinaryAst(data.binaryAst, data.inlineData)
            }
        }

        val units = unitsByPath.entries.sortedBy { it.key }.map { it.value }

        val mainCallParameters = when {
            JsEnvironmentConfigurationDirectives.CALL_MAIN in module.directives -> MainCallParameters.mainWithArguments(listOf())
            JsEnvironmentConfigurationDirectives.MAIN_ARGS in module.directives -> {
                MainCallParameters.mainWithArguments(module.directives[JsEnvironmentConfigurationDirectives.MAIN_ARGS].first())
            }
            else -> MainCallParameters.noCall()
        }

        val translator = K2JSTranslator(jsConfig, false)
        val translationResult = translator.translateUnits(
            JsEnvironmentConfigurator.Companion.ExceptionThrowingReporter, units, mainCallParameters, analysisResult as? WebAnalysisResult
        )

        if (!incrementalCompilationEnabled) {
            jsConfig.configuration[WebConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER]?.let {
                val incrementalData = JsClassicIncrementalDataProvider.IncrementalData()
                val incrementalService = it as IncrementalResultsConsumerImpl

                for ((srcFile, data) in incrementalService.packageParts) {
                    incrementalData.translatedFiles[srcFile] = data
                }

                incrementalData.packageMetadata += incrementalService.packageMetadata

                incrementalData.header = incrementalService.headerMetadata
                testServices.jsClassicIncrementalDataProvider.recordIncrementalData(module, incrementalData)
            }
        }

        val outputFile = File(JsEnvironmentConfigurator.getJsModuleArtifactPath(testServices, module.name) + ".js")
        if (translationResult !is TranslationResult.Success) {
            return BinaryArtifacts.Js.OldJsArtifact(outputFile, translationResult)
        }

        val outputPrefixFile = JsEnvironmentConfigurator.getPrefixFile(module)
        val outputPostfixFile = JsEnvironmentConfigurator.getPostfixFile(module)
        val outputFiles = translationResult.getOutputFiles(outputFile, outputPrefixFile, outputPostfixFile)
        outputFiles.writeAllTo(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices))

        if (jsConfig.moduleKind != ModuleKind.PLAIN) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = wrapWithModuleEmulationMarkers(content, moduleId = jsConfig.moduleId, moduleKind = jsConfig.moduleKind)
            FileUtil.writeToFile(outputFile, wrappedContent)
        }

        return BinaryArtifacts.Js.OldJsArtifact(outputFile, translationResult)
    }
}
