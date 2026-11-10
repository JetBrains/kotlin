/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.ImportModelsTextFormat
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.normalizeImportModelsResultText
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel
import kotlin.test.Test
import kotlin.test.assertEquals

@JvmGradlePluginTests
class ImportModelsGoldenNormalizationTest : KGPBaseTest() {
    @Test
    fun `replaces the project dir in its printed escaped form`() {
        // A Windows-style path with an apostrophe, printed by the same printer the harness uses:
        // the path appears with doubled backslashes, the apostrophe stays literal
        val projectDir = "C:\\work\\it's project"
        val dumpedResult = resultModel {
            error = importModelError {
                errorType = Error.Type.ERROR_TYPE_INTERNAL_ERROR
                errorMessage = "$projectDir\\out.bin"
            }
        }

        val normalized = normalizeImportModelsResultText(dumpedResult, projectDir)

        assertEquals(
            "<PROJECT_DIR>\\out.bin",
            ImportModelsTextFormat.parseResult(normalized).error.errorMessage,
        )
    }
}
