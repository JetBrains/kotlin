/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.blackbox

import org.jetbrains.kotlin.test.grouping.GroupedTestsExportedEntryPointGenerator
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter.Companion.computePackage
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import java.io.File

/** The name of the exported top-level function, in the root package of the launcher module, that runs a grouped batch. */
const val RUN_GROUPED_TESTS_FUNCTION_NAME: String = "runGroupedTests"

/**
 * The entry point is exported by `@JsExport` rather than by a compiler configuration key, as `box()` is in the one-stage
 * pipeline: a released compiler invoked via CLI has no such key.
 */
object JsGroupedTestsExportedEntryPointGenerator : GroupedTestsExportedEntryPointGenerator() {
    override fun generateExportedEntryPointSource(runAllFunctionName: String): String =
        """
        @OptIn(kotlin.js.ExperimentalJsExport::class)
        @JsExport
        fun $RUN_GROUPED_TESTS_FUNCTION_NAME() {
            $runAllFunctionName()
        }
        """.trimIndent()
}

/**
 * The executable of a grouped batch: [delegate] is linked from the synthetic [launcherModule], whose result-collecting
 * driver runs every test of the batch. An isolated test is represented by a plain [JsIrArtifact] instead.
 */
class JsGroupedBatchArtifact(
    val delegate: JsIrArtifact,
    val launcherModule: TestModule,
) : BinaryArtifacts.Js() {
    override val outputFile: File
        get() = delegate.outputFile
}

/**
 * Computes the synthetic per-test `ProxyLauncher` class name, which also identifies the test in the grouped result
 * protocol. It is an injective encoding of the per-test additional package (see [computePackage]), so distinct tests
 * always get distinct names.
 */
fun computeJsProxyLauncherClassName(testInfo: KotlinTestInfo): String =
    "ProxyLauncher_${computePackage(testInfo).encodeToIdentifier()}"

/**
 * An ASCII letter or digit is kept as it is, an underscore is doubled, and every other UTF-8 byte becomes an underscore
 * followed by its two hex digits. A hex digit is never an underscore, so the two escapes cannot be confused.
 */
private fun String.encodeToIdentifier(): String = buildString(length + 16) {
    for (byte in encodeToByteArray()) {
        when (val char = (byte.toInt() and 0xFF).toChar()) {
            in 'A'..'Z', in 'a'..'z', in '0'..'9' -> append(char)
            '_' -> append("__")
            else -> append('_').append(byte.toHexString())
        }
    }
}
