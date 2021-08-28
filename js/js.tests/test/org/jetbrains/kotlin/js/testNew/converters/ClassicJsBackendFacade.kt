/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testNew.converters

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendFacade
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset

class ClassicJsBackendFacade(
    testServices: TestServices
) : ClassicBackendFacade<BinaryArtifacts.Js>(testServices, ArtifactKinds.Js) {
    companion object {
        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"
    }

    private fun wrapWithModuleEmulationMarkers(content: String, moduleKind: ModuleKind, moduleId: String): String {
        val escapedModuleId = StringUtil.escapeStringCharacters(moduleId)

        return when (moduleKind) {
            ModuleKind.COMMON_JS -> "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                    "$content\n" +
                    "$KOTLIN_TEST_INTERNAL.endModule(\"$escapedModuleId\");"

            ModuleKind.AMD, ModuleKind.UMD ->
                "if (typeof $KOTLIN_TEST_INTERNAL !== \"undefined\") { " +
                        "$KOTLIN_TEST_INTERNAL.setModuleId(\"$escapedModuleId\"); }\n" +
                        "$content\n"

            ModuleKind.PLAIN -> content

            ModuleKind.ES -> error("Module emulation markers are not supported for ES modules")
        }
    }

    override fun transform(module: TestModule, inputArtifact: ClassicBackendInput): BinaryArtifacts.Js? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val (psiFiles, analysisResult, project, _) = inputArtifact

        // TODO how to reuse this config from frontend
        val jsConfig = JsEnvironmentConfigurator.createJsConfig(project, configuration)
        val units = psiFiles.map(TranslationUnit::SourceFile)
        val mainCallParameters = when (JsEnvironmentConfigurationDirectives.CALL_MAIN) {
            in module.directives -> MainCallParameters.mainWithArguments(listOf())
            else -> MainCallParameters.noCall()
        }

        val translator = K2JSTranslator(jsConfig, false)
        val translationResult = translator.translateUnits(
            JsEnvironmentConfigurator.Companion.ExceptionThrowingReporter, units, mainCallParameters, analysisResult as? JsAnalysisResult
        )

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