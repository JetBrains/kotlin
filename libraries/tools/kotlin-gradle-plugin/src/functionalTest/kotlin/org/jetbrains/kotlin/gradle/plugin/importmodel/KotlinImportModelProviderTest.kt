/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.Action
import org.jetbrains.kotlin.importmodels.proto.Capability
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.GradleTaskAction
import org.jetbrains.kotlin.importmodels.proto.Platform
import org.jetbrains.kotlin.importmodels.proto.SourceRoot
import org.jetbrains.kotlin.importmodels.proto.SourceRootKind
import org.jetbrains.kotlin.importmodels.proto.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinImportModelProviderTest {
    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces stable main and test JVM models`() {
        val project = buildProjectWithJvm {
            kotlinJvmExtension.target.compilations.create("deploy")
            val generateMainSources = tasks.register("generateMainSources") {
                it.outputs.dir(layout.projectDirectory.dir("src/main/generated-kotlin"))
            }
            kotlinJvmExtension.sourceSets.getByName("main").generatedKotlin.srcDir(generateMainSources)
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

        val base = provider.baseInformation()
        val pluginVersion = project.kotlinToolingVersion
        val expectedVersion = Version.newBuilder()
            .setMajor(pluginVersion.major)
            .setMinor(pluginVersion.minor)
            .setPatch(pluginVersion.patch)
            .apply { pluginVersion.classifier?.let(::setClassifier) }
            .build()
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(expectedVersion, base.pluginVersion)
        assertEquals(listOf(Capability.CAPABILITY_JVM), base.capabilitiesList)

        val firstProjectModel = provider.projectInformation()
        val secondProjectModel = provider.projectInformation()
        val mainId = CompilationUnitId.newBuilder().setValue(":|:|jvm|main").build()
        val testId = CompilationUnitId.newBuilder().setValue(":|:|jvm|test").build()
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, firstProjectModel.id)
        assertEquals(listOf(mainId, testId), firstProjectModel.compilationUnitIdsList)
        assertEquals(firstProjectModel.compilationUnitIdsList, secondProjectModel.compilationUnitIdsList)

        assertCompilationUnit(provider.compilationUnit(mainId), mainId, "main", false, ":compileKotlin")
        assertCompilationUnit(provider.compilationUnit(testId), testId, "test", true, ":compileTestKotlin")
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedIsTest: Boolean,
        expectedBuildTaskPath: String,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.compilationName)
        assertEquals(Platform.PLATFORM_JVM, model.platform)
        assertEquals(expectedIsTest, model.isTest)
        val action = model.buildActionsList.single()
        assertTrue(action.hasGradleAction())
        assertEquals(
            GradleTaskAction.newBuilder().setTaskPath(expectedBuildTaskPath).build(),
            action.gradleAction,
        )
        assertEquals(
            when (expectedName) {
                "main" -> listOf(
                    sourceRoot(
                        "src/main/generated-kotlin",
                        SourceRootKind.SOURCE_ROOT_KIND_GENERATED,
                        gradleAction(":generateMainSources"),
                    ),
                    sourceRoot("src/main/java", SourceRootKind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/main/kotlin", SourceRootKind.SOURCE_ROOT_KIND_SOURCE),
                )
                "test" -> listOf(
                    sourceRoot("src/test/java", SourceRootKind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/test/kotlin", SourceRootKind.SOURCE_ROOT_KIND_SOURCE),
                )
                else -> error("Unexpected compilation name: $expectedName")
            },
            model.sourceRootsList,
        )
    }

    private fun sourceRoot(path: String, kind: SourceRootKind, vararg producingActions: Action): SourceRoot = SourceRoot.newBuilder()
        .setPath(path)
        .setKind(kind)
        .addAllProducingActions(producingActions.asIterable())
        .build()

    private fun gradleAction(taskPath: String): Action = Action.newBuilder()
        .setGradleAction(GradleTaskAction.newBuilder().setTaskPath(taskPath))
        .build()
}
