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

internal fun computeJsGroupedTestModuleId(testServices: TestServices, module: TestModule): String =
    if (module.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME) {
        module.name.safeModuleName
    } else {
        val hash = Integer.toHexString(module.name.hashCode())
        makeValidES5Identifier("${testServices.testInfo.methodName.take(MAX_TEST_NAME_LENGTH_IN_MODULE_ID)}_$hash", withHash = false)
    }

private const val MAX_TEST_NAME_LENGTH_IN_MODULE_ID = 40

/**
 * Gives the modules of a grouped test short JS module names: a per-module file is named after its module, and the module
 * names of the grouping configuration are long enough to exceed the 255-character limit of a file name.
 */
class JsGroupedTestModuleOutputNameConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.name == JsTestHelpersModuleTransformer.HELPERS_MODULE_NAME) return
        if (testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)) return
        configuration.perModuleOutputName = computeJsGroupedTestModuleId(testServices, module)
    }
}
