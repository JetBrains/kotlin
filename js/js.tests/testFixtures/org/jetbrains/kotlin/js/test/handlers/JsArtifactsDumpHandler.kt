/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.config.JsGenerationGranularity
import org.jetbrains.kotlin.js.engine.ScriptExecutionException
import org.jetbrains.kotlin.js.test.utils.compiledTestOutputDirectory
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.unreachableBranch
import java.io.File

/**
 * Copy JS artifacts from the temporary directory to the `js/js.tests/build/out` directory.
 */
object JsArtifactsDumpHandler {
    private val supportedTranslationModes = listOf(
        TranslationMode.FULL_DEV,
        TranslationMode.FULL_PROD_MINIMIZED_NAMES,
        TranslationMode.PER_MODULE_DEV,
        TranslationMode.PER_MODULE_PROD_MINIMIZED_NAMES,
        TranslationMode.PER_FILE_DEV,
        TranslationMode.PER_FILE_PROD_MINIMIZED_NAMES,
    )

    class Suppressor(testServices: TestServices) : TestFailureSuppressor(testServices) {
        override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> {
            // Replace paths to the temporary directory with paths to the dump directory so that JS stack traces are navigatable in the IDE.
            return failedAssertions.map {
                val cause = it.cause as? ScriptExecutionException ?: return@map it
                it.withReplacedCause(
                    ScriptExecutionException(cause.stdout.replacePaths(testServices), cause.stderr.replacePaths(testServices)).apply {
                        stackTrace = cause.stackTrace
                    }
                )
            }
        }

        override fun checkIfTestShouldBeUnmuted() {}
    }

    class Checker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
        override fun check(thereWereFailures: Boolean) {
            for (translationMode in supportedTranslationModes) {
                val outputDir = getOutputDir(translationMode, testServices)
                copy(from = JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, translationMode), into = outputDir)
            }
        }
    }

    private fun String.replacePaths(testServices: TestServices): String = supportedTranslationModes.fold(this) { s, translationMode ->
        val outputDir = getOutputDir(translationMode, testServices)
        JsEnvironmentConfigurator
            .getJsArtifactsOutputDir(testServices, translationMode)
            .listFiles { it.isFile }!!
            .fold(s) { s, file ->
                s.replace(file.absolutePath, outputDir.resolve(file.name).absolutePath)
            }
    }

    private fun getOutputDir(translationMode: TranslationMode, testServices: TestServices): File {
        val prefix = when (translationMode) {
            TranslationMode.FULL_DEV -> "out"
            TranslationMode.FULL_PROD -> unreachableBranch(translationMode)
            TranslationMode.FULL_PROD_MINIMIZED_NAMES -> "out-min"
            TranslationMode.PER_MODULE_DEV -> "out-per-module"
            TranslationMode.PER_MODULE_PROD -> unreachableBranch(translationMode)
            TranslationMode.PER_MODULE_PROD_MINIMIZED_NAMES -> "out-per-module-min"
            TranslationMode.PER_FILE_DEV -> "out-per-file"
            TranslationMode.PER_FILE_PROD -> unreachableBranch(translationMode)
            TranslationMode.PER_FILE_PROD_MINIMIZED_NAMES -> "out-per-file-min"
        }
        return testServices.compiledTestOutputDirectory(
            prefix,
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR,
            JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX,
            JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR,
        ).applyIf(translationMode.granularity != JsGenerationGranularity.WHOLE_PROGRAM) {
            // For per-module and per-file tests, put the artifacts into a separate subdirectory
            // so that artifacts with common names (like kotlin-kotlin-stdlib.js) don't overwrite each other.
            resolve(
                testServices.klibEnvironmentConfigurator.getKlibArtifactSimpleName(
                    testServices,
                    ModuleStructureExtractor.DEFAULT_MODULE_NAME,
                ),
            )
        }
    }

    private fun copy(from: File, into: File) {
        if (from.listFiles()?.size == 0) return
        from.copyRecursively(into, overwrite = true)
    }
}
