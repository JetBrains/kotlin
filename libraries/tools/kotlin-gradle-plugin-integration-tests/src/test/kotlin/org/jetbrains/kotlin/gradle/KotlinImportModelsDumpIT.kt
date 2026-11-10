/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.ImportModelsTextFormat
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertImportModelsGolden
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.Result
import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val DUMP_TASK = "dumpKotlinImportModels"
private const val DUMP_TASK_ARGUMENT = "-Pkotlin.internal.dumpKotlinImportModels=true"

@JvmGradlePluginTests
class KotlinImportModelsDumpIT : KGPBaseTest() {
    @GradleTest
    fun `dump writes golden-comparable models without compiling`(gradleVersion: GradleVersion) {
        project(projectName = "simpleProject", gradleVersion = gradleVersion) {
            build(DUMP_TASK, DUMP_TASK_ARGUMENT) {
                assertTasksExecuted(":$DUMP_TASK")
                // The dump needs nothing else built
                assertEquals(setOf(":$DUMP_TASK"), tasks.map { it.path }.toSet())
                assertHasDiagnostic(
                    KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed,
                    "kotlin.internal.dumpKotlinImportModels",
                )
            }

            val dumpRoot = projectPath.resolve("build/kotlin-import-models")
            assertEquals(
                setOf("base.bin", "base.txt", "projectInformation.bin", "projectInformation.txt"),
                dumpRoot.toFile().listFiles().orEmpty().map { it.name }.toSet(),
            )

            val baseBytes = dumpRoot.resolve("base.bin").readBytes()
            assertImportModelsGolden(projectPath, "simpleProject", KotlinImportModelIds.BASE)
            assertImportModelsGolden(projectPath, "simpleProject", KotlinImportModelIds.PROJECT_INFORMATION)

            // The text dump carries the same message as the bytes, printed in the expanded readable
            // form: a dump that left the Any payload as escaped bytes would re-print differently
            val baseText = dumpRoot.resolve("base.txt").readText()
            val reparsed = ImportModelsTextFormat.parseResult(baseText)
            assertEquals(Result.parseFrom(baseBytes), reparsed)
            assertEquals(ImportModelsTextFormat.printResult(reparsed), baseText)

            val staleFile = dumpRoot.resolve("unexpected.txt")
            staleFile.writeText("stale")
            build(DUMP_TASK, DUMP_TASK_ARGUMENT) { assertTasksExecuted(":$DUMP_TASK") }
            assertFalse(Files.exists(staleFile))
            assertContentEquals(baseBytes, dumpRoot.resolve("base.bin").readBytes())
        }
    }
}
