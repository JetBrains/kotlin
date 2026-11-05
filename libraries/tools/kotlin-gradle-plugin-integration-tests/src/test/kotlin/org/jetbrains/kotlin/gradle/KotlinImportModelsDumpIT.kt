/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.assertHasDiagnostic
import org.jetbrains.kotlin.gradle.testbase.assertImportModelsGolden
import org.jetbrains.kotlin.gradle.testbase.assertOutputContains
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.project
import java.nio.file.Files
import kotlin.io.path.readBytes
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
                setOf("base.bin", "base.json", "projectInformation.bin", "projectInformation.json"),
                dumpRoot.toFile().listFiles().orEmpty().map { it.name }.toSet(),
            )

            val baseBytes = dumpRoot.resolve("base.bin").readBytes()
            assertImportModelsGolden(projectPath, "simpleProject", "base.json")
            assertImportModelsGolden(projectPath, "simpleProject", "projectInformation.json")

            val staleFile = dumpRoot.resolve("unexpected.json")
            staleFile.writeText("stale")
            build(DUMP_TASK, DUMP_TASK_ARGUMENT) { assertTasksExecuted(":$DUMP_TASK") }
            assertFalse(Files.exists(staleFile))
            assertContentEquals(baseBytes, dumpRoot.resolve("base.bin").readBytes())
        }
    }

    @GradleTest
    fun `without the property the dump task does not exist`(gradleVersion: GradleVersion) {
        project(projectName = "simpleProject", gradleVersion = gradleVersion) {
            buildAndFail(DUMP_TASK) {
                assertOutputContains("Task '$DUMP_TASK' not found in root project")
            }
        }
    }
}
