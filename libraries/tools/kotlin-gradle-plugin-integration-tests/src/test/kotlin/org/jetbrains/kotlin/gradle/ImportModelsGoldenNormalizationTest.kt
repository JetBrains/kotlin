/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.ImportModelsTextFormat
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.normalizeImportModelsResultText
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.Any
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.baseModel
import org.jetbrains.kotlin.importmodels.proto.compilationUnitId
import org.jetbrains.kotlin.importmodels.proto.projectModel
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.jetbrains.kotlin.importmodels.proto.error as importModelError
import org.jetbrains.kotlin.importmodels.proto.result as resultModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@JvmGradlePluginTests
@MustRunOnChangesInGradle
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

    @Test
    fun `replaces the project dir only at path boundaries`() {
        // Only a whole path component replaces: glued to a neighbouring component on either
        // side, the dir keeps its text
        val projectDir = "/work/app"
        val dumpedResult = resultModel {
            error = importModelError {
                errorType = Error.Type.ERROR_TYPE_INTERNAL_ERROR
                errorMessage = "cache/work/app/out.bin cache+/work/app/out.bin " +
                    "/work/application/out.bin /work/app/out.bin"
            }
        }

        val normalized = normalizeImportModelsResultText(dumpedResult, projectDir)

        assertEquals(
            "cache/work/app/out.bin cache+/work/app/out.bin /work/application/out.bin " +
                "<PROJECT_DIR>/out.bin",
            ImportModelsTextFormat.parseResult(normalized).error.errorMessage,
        )
    }

    @Test
    fun `sorts set-like compilation unit ids for the golden`() {
        val dumpedResult = resultModel {
            model = Any.pack(
                projectModel {
                    id = KotlinImportModelIds.PROJECT_INFORMATION
                    compilationUnitIds += listOf(
                        compilationUnitId { value = ":|:|jvm|test" },
                        compilationUnitId { value = ":|:|jvm|main" },
                        compilationUnitId { value = ":|:|jvm|deploy" },
                    )
                },
            )
        }

        val normalized = normalizeImportModelsResultText(dumpedResult, "/work/app")

        assertEquals(
            listOf(":|:|jvm|deploy", ":|:|jvm|main", ":|:|jvm|test"),
            ImportModelsTextFormat.parseResult(normalized)
                .model.unpack(ProjectModel::class.java).compilationUnitIdsList.map { it.value },
        )
    }

    @Test
    fun `keeps a missing plugin version out of the golden`() {
        val dumpedResult = resultModel {
            model = Any.pack(baseModel { id = KotlinImportModelIds.BASE })
        }

        val normalized = normalizeImportModelsResultText(dumpedResult, "/work/app")

        assertFalse(
            ImportModelsTextFormat.parseResult(normalized)
                .model.unpack(BaseModel::class.java).hasPluginVersion(),
        )
    }
}
