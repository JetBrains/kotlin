/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.util.JsonFormat
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@JvmGradlePluginTests
class KotlinImportModelsDumpIT : KGPBaseTest() {
    @GradleTest
    fun `dump task writes JSON JVM models without compilation`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // The diagnostic dump task reads live project state
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
            ),
        ) {
            build("dumpKotlinImportModels") {
                assertTasksExecuted(":dumpKotlinImportModels")
                assertTasksAreNotInTaskGraph(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
            }
            val firstIds = assertDump()
            val staleFile = projectPath.resolve("build/kotlin-import-models/unexpected.json").toFile().also { it.writeText("stale") }
            build("dumpKotlinImportModels") { assertTasksExecuted(":dumpKotlinImportModels") }
            assertFalse(staleFile.exists())
            assertEquals(firstIds, assertDump())
        }
    }

    private fun TestProject.assertDump(): List<CompilationUnitId> {
        val root = projectPath.resolve("build/kotlin-import-models").toFile()
        assertEquals(KotlinImportModelIds.BASE, parseBase(root.resolve("base.json")).id)
        val project = parseProject(root.resolve("project.json"))
        val units = listOf("main", "test").map { name -> parseCompilation(root.resolve("compilation-units/$name.json")) }
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, project.id)
        assertEquals(listOf("main", "test"), units.map { it.name })
        assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
        return project.compilationUnitIdsList
    }
}

private fun parseBase(file: File): BaseModel = BaseModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseProject(file: File): ProjectModel = ProjectModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseCompilation(file: File): CompilationUnitModel =
    CompilationUnitModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()
