/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.js.config.perModuleOutputName
import org.jetbrains.kotlin.test.grouping.GroupedTestsResultProtocol
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo
import java.io.File

/** The name of the exported top-level function, in the root package of the launcher module, that runs a grouped batch. */
const val RUN_GROUPED_TESTS_FUNCTION_NAME: String = "runGroupedTests"

internal fun jsGroupedTestsEntryPointSource(runAllFunctionName: String): String =
    """
    @OptIn(kotlin.js.ExperimentalJsExport::class)
    @JsExport
    fun $RUN_GROUPED_TESTS_FUNCTION_NAME() {
        $runAllFunctionName()
    }
    """.trimIndent()

/**
 * The executable of a grouped batch: [delegate] is linked from the synthetic [launcherModule],
 * whose result-collecting driver runs every test of the batch.
 * An isolated test is represented by a plain [JsIrArtifact] instead.
 *
 * @param linkedPerModule whether the batch is linked in the per-module translation modes as well as in the whole-program ones.
 */
class JsGroupedBatchArtifact(
    val delegate: JsIrArtifact,
    val launcherModule: TestModule,
    val linkedPerModule: Boolean = false,
) : BinaryArtifacts.Js() {
    override val outputFile: File
        get() = delegate.outputFile

    override fun unwrap(): BinaryArtifacts.Js = delegate
}

/**
 * Computes the id a test is reported under in the grouped result protocol (see [GroupedTestsResultProtocol]): the
 * package `BatchingPackageInserter` has put the test into, which is unique to the test within its batch (see
 * [computePackage]) and carries none of the characters the protocol lines are built of.
 */
fun computeJsGroupedTestId(testInfo: KotlinTestInfo): String = computePackage(testInfo)

/**
 * Computes the name of the JS module a module of a grouped test is linked into in the per-module translation modes,
 * which also names its file (see [JsGroupedTestModuleOutputNameConfigurator]).
 */
fun computeJsGroupedTestModuleId(testServices: TestServices, module: TestModule): String =
    if (module.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME) {
        module.name.safeModuleName
    } else {
        // The name of the module is unique to the test, the name of the test makes the file recognizable.
        val hash = Integer.toHexString(module.name.hashCode())
        makeValidES5Identifier("${testServices.testInfo.methodName.take(MAX_TEST_NAME_LENGTH_IN_MODULE_ID)}_$hash", withHash = false)
    }

private const val MAX_TEST_NAME_LENGTH_IN_MODULE_ID = 40

/**
 * Gives every module of a grouped test, but the shared helpers module, a short name of its JS module (see
 * [computeJsGroupedTestModuleId]), which is written into its KLIB. The modules of a grouped test are named after the test
 * and its test class (see `ModuleStructureExtractorImpl`), and the file of a module produced by a per-module translation
 * mode of a batch is named after both the first test of the batch and that module, so the names of the files could
 * exceed the limit of the file system of 255 characters (see `JsInProcessSecondStageFacade.Grouping`).
 * An isolated test keeps the names of its modules, which the JS of its own may refer to.
 */
class JsGroupedTestModuleOutputNameConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME) return
        if (testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)) return
        configuration.perModuleOutputName = computeJsGroupedTestModuleId(testServices, module)
    }
}
