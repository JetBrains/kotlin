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
import org.jetbrains.kotlin.importmodels.proto.Capability
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.Platform
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Version
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
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

            val firstIds = assertKotlinImportModelDump(buildOptions.kotlinVersion)

            val staleFile = projectPath.resolve("build/kotlin-import-models/unexpected.json").toFile()
            staleFile.writeBytes(byteArrayOf(1, 2, 3))

            build("dumpKotlinImportModels") {
                assertTasksExecuted(":dumpKotlinImportModels")
                assertTasksAreNotInTaskGraph(":compileKotlin", ":compileTestKotlin", ":compileDeployKotlin")
            }

            assertFalse(staleFile.exists())
            assertEquals(firstIds, assertKotlinImportModelDump(buildOptions.kotlinVersion))
        }
    }

    private fun TestProject.assertKotlinImportModelDump(kotlinVersion: String): List<CompilationUnitId> {
        val root = projectPath.resolve("build/kotlin-import-models").toFile()

        assertFalse(root.resolve("proto").exists())

        val base = parseBaseModel(root.resolve("base.json"))
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(KotlinToolingVersion(kotlinVersion).toImportModelVersion(), base.pluginVersion)
        assertEquals(listOf(Capability.JVM_ONLY), base.capabilitiesList)

        val project = parseProjectModel(root.resolve("project.json"))
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, project.id)

        val main = readCompilationUnit(root, "main")
        val test = readCompilationUnit(root, "test")
        assertEquals(
            listOf(main.parameters.compilationUnitId, test.parameters.compilationUnitId),
            project.compilationUnitIdsList,
        )
        assertCompilationUnit(main, "main", false, ":compileKotlin")
        assertCompilationUnit(test, "test", true, ":compileTestKotlin")
        return project.compilationUnitIdsList
    }

    private fun readCompilationUnit(root: File, name: String): CompilationUnitModel {
        val builder = CompilationUnitModel.newBuilder()
        JsonFormat.parser().merge(root.resolve("compilation-units/$name.json").readText(), builder)
        return builder.build()
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedName: String,
        expectedIsTest: Boolean,
        expectedCompileTaskPath: String,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedName, model.compilationName)
        assertEquals(Platform.JVM, model.platform)
        assertEquals(expectedIsTest, model.isTest)
        assertEquals(expectedCompileTaskPath, model.compileTaskPath)
    }
}

private fun parseBaseModel(file: File): BaseModel {
    val builder = BaseModel.newBuilder()
    JsonFormat.parser().merge(file.readText(), builder)
    return builder.build()
}

private fun parseProjectModel(file: File): ProjectModel {
    val builder = ProjectModel.newBuilder()
    JsonFormat.parser().merge(file.readText(), builder)
    return builder.build()
}

private fun KotlinToolingVersion.toImportModelVersion(): Version = Version.newBuilder()
    .setMajor(major)
    .setMinor(minor)
    .setPatch(patch)
    .apply { this@toImportModelVersion.classifier?.let(::setClassifier) }
    .build()
