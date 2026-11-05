/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.normalizeImportModelsResultJson
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel
import kotlin.test.Test
import kotlin.test.assertEquals

@JvmGradlePluginTests
class ImportModelsGoldenNormalizationTest {
    @Test
    fun `replaces the project dir in its printed JSON-escaped form`() {
        // A Windows-style path with an apostrophe, printed by the same printer the dump uses:
        // the path appears with doubled backslashes and the apostrophe as a unicode escape
        val projectDir = "C:\\work\\it's project"
        val dumpedJson = KotlinImportModelSerialization.toJson(
            resultModel {
                error = importModelError {
                    errorType = Error.Type.ERROR_TYPE_INTERNAL_ERROR
                    errorMessage = "$projectDir\\out.bin"
                }
            },
        )

        val normalized = normalizeImportModelsResultJson(dumpedJson, projectDir)

        assertEquals(
            "<PROJECT_DIR>\\out.bin",
            KotlinImportModelSerialization.parseResult(normalized).error.errorMessage,
        )
    }
}
