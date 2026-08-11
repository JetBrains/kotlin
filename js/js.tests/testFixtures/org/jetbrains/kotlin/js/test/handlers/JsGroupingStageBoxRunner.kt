/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.js.test.JsAdditionalSourceProvider
import org.jetbrains.kotlin.js.test.converters.JsGroupingSecondStageFacade.Companion.BATCH_TEST_PASSED_MARKER
import org.jetbrains.kotlin.js.test.utils.extractEntryModulePath
import org.jetbrains.kotlin.js.test.utils.getAllFilesForRunner
import org.jetbrains.kotlin.js.test.utils.getModeOutputFilePath
import org.jetbrains.kotlin.js.test.utils.getTestModuleName
import org.jetbrains.kotlin.js.test.utils.testWithModuleSystem
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.grouping.computeProxyLauncherClassName
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.isSingleTestBatch
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingStageHandler
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.extractTestPackage
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.testInfraError

/**
 * Runs the JS a grouped batch was linked into (see
 * [org.jetbrains.kotlin.js.test.converters.JsGroupingSecondStageFacade]) and reports a verdict per test of the batch.
 *
 * The batch's JS files are loaded into one V8 realm, and every test is then invoked through its own `@JsExport`ed
 * `ProxyLauncher_<hash>()`. Unlike K/Wasm, K/JS needs no printed result protocol to demultiplex a batch: V8 is driven as
 * a REPL from the JVM, so the launchers can simply be called one at a time and each verdict is attributed to its test by
 * construction — a per-test `eval`, not a line in a shared output.
 *
 * A batch of a single test — an isolated test, or one that merely ended up alone — is instead run the way the ungrouped
 * [JsBoxRunner] runs it: `box()` reached through the module's own export. Its failures are deliberately *not* routed per
 * test but left to propagate, because a single-test batch's grouping-stage failures are merged into that test's own
 * failure interceptor by the engine, which is what lets `IGNORE_BACKEND` and friends still suppress them.
 */
class JsGroupingStageBoxRunner(testServices: TestServices) : GroupingStageHandler<BinaryArtifacts.Js>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false,
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Js>
        get() = ArtifactKinds.Js

    override fun processArtifact(artifact: BinaryArtifacts.Js) {
        val inputs = testServices.groupingStageInputs
        val jsArtifact = artifact.unwrap() as? JsIrArtifact
            ?: testInfraError("JsGroupingStageBoxRunner expects a JsIrArtifact, but got ${artifact::class}")

        // Anything the batch links has to be run from the perspective of one of its tests: the grouping stage has no
        // module structure of its own, and the linked artifacts were written into the first test's output directories.
        val services = inputs.first().testServices
        if (JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE in services.moduleStructure.allDirectives) return
        if (dontRunGeneratedCode(services)) return

        if (testServices.isSingleTestBatch()) {
            runSingleTest(services, jsArtifact)
        } else {
            runGroupedBatch(services, jsArtifact, inputs)
        }
    }

    private fun dontRunGeneratedCode(services: TestServices): Boolean =
        services.moduleStructure.allDirectives[JsEnvironmentConfigurationDirectives.DONT_RUN_GENERATED_CODE]
            .contains(services.defaultsProvider.targetBackend?.name)

    /**
     * Runs the only test of the batch exactly as the ungrouped [JsBoxRunner] would: `box()` is called through the test's
     * own module export, in every translation mode the link produced.
     */
    private fun runSingleTest(services: TestServices, artifact: JsIrArtifact) {
        val mainModule = JsEnvironmentConfigurator.getMainModule(services)
        val withModuleSystem = testWithModuleSystem(services)
        val testModuleName = getTestModuleName(services)
        val testPackage = extractTestPackage(services)

        for ([mode, jsFiles] in getAllFilesForRunner(services, mapOf(mainModule to artifact))) {
            V8JsTestChecker.check(
                jsFiles,
                testModuleName,
                testPackage,
                BOX_FUNCTION_NAME,
                EXPECTED_BOX_RESULT,
                withModuleSystem,
                extractEntryModulePath(mode, jsFiles, services),
            )
        }
    }

    private fun runGroupedBatch(services: TestServices, artifact: JsIrArtifact, inputs: List<NonGroupingStageOutput>) {
        // The batch is linked as a single `PLAIN` module (any other module system isolates the test, see
        // `JsGroupingTestIsolator`), whose object the generated JS exposes under its raw module name — either as a
        // top-level `var` or, when that name is not a valid ES5 identifier, as a `globalThis` entry. `this[...]` reaches
        // it in both cases, which is also how the ungrouped runner reaches a test module.
        val moduleReference = "this['${JsEnvironmentConfigurator.getMainModuleName(services)}']"

        for (jsFiles in groupedBatchFilesByMode(services, artifact, inputs).values) {
            runGroupedBatchOnFiles(jsFiles, moduleReference, inputs)
        }
    }

    private fun runGroupedBatchOnFiles(
        jsFiles: List<String>,
        moduleReference: String,
        inputs: List<NonGroupingStageOutput>,
    ) {
        try {
            V8JsTestChecker.run(jsFiles) {
                for (input in inputs) {
                    val launcherName = computeProxyLauncherClassName(input.testServices.testInfo)
                    try {
                        val output = eval("$moduleReference.$launcherName()")
                        if (BATCH_TEST_PASSED_MARKER !in output) {
                            input.failWith(
                                "Test '$launcherName' of the grouped batch reported no pass marker " +
                                        "('$BATCH_TEST_PASSED_MARKER'). Its launcher returned:\n${output.abridged()}"
                            )
                        }
                    } catch (e: Throwable) {
                        input.failWith("Test '$launcherName' of the grouped batch failed:\n${e.message?.abridged()}")
                    }
                }
                ""
            }
        } catch (e: Throwable) {
            // Loading the batch's JS failed, or the VM died while it was being loaded, so no test of the batch ran.
            // Fail all of them rather than let the batch pass silently.
            val reason = "Failed to load the JS of the grouped batch, so no test of it ran:\n${e.message?.abridged()}"
            inputs.forEach { input -> input.failWith(reason) }
        }
    }

    /**
     * The JS files to load, per translation mode the link produced.
     *
     * Deliberately not [getAllFilesForRunner]: that resolves a test's companion and input JS files from *one* test's
     * module structure, which for a batch would silently mean the first test's. A grouped batch has none of those (they
     * isolate the test), so all that remains is the linked output plus the `_common.js` helpers — collected across every
     * test's directory, since a batch spans several of them.
     */
    private fun groupedBatchFilesByMode(
        services: TestServices,
        artifact: JsIrArtifact,
        inputs: List<NonGroupingStageOutput>,
    ): Map<TranslationMode, List<String>> {
        val mainModule = JsEnvironmentConfigurator.getMainModule(services)
        val commonJsFiles = inputs
            .flatMap { input -> input.testServices.moduleStructure.originalTestDataFiles }
            .flatMap { testDataFile -> JsAdditionalSourceProvider.getAdditionalJsFiles(testDataFile.parent) }
            .map { it.absolutePath }
            .distinct()

        return artifact.compilerResult.entries.associate { entry ->
            val mode = entry.key
            mode to (commonJsFiles + getModeOutputFilePath(services, mainModule, mode))
        }
    }

    /** Routes [message] into this specific test's failure sink, so the engine reports it against this test. */
    private fun NonGroupingStageOutput.failWith(message: String) {
        catchingExecutor.executeWithCatching({ WrappedException.FromGroupingHandler(it, this@JsGroupingStageBoxRunner) }) {
            throw AssertionError(message)
        }
    }

    companion object {
        private const val BOX_FUNCTION_NAME = "box"
        private const val EXPECTED_BOX_RESULT = "OK"

        /**
         * How much of a VM output a failure message may carry.
         *
         * What V8 echoes for a launcher call includes everything the test body printed, and a batch failing to load
         * hands the same output to each of its tests: unabridged, one broken batch of 50 tests can produce hundreds of
         * megabytes of failure text, which is far past the point where it helps anyone read the failure.
         */
        private const val MAX_REPORTED_OUTPUT_LENGTH = 20_000

        private fun String.abridged(): String =
            if (length <= MAX_REPORTED_OUTPUT_LENGTH) {
                this
            } else {
                take(MAX_REPORTED_OUTPUT_LENGTH) + "\n<...${length - MAX_REPORTED_OUTPUT_LENGTH} more characters omitted...>"
            }
    }
}
