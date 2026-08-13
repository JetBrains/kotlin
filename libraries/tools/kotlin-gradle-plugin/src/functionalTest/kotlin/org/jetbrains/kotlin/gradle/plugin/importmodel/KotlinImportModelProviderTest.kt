/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val mainOutput = project.layout.buildDirectory.dir("classes/kotlin/main").get().asFile
        val testOutput = project.layout.buildDirectory.dir("classes/kotlin/test").get().asFile
        assertFalse(mainOutput.exists())
        assertFalse(testOutput.exists())

        val base = provider.baseInformation()
        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(project.kotlinToolingVersion.major, base.pluginVersion.major)
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_JVM), base.capabilitiesList)

        val projectModel = provider.projectInformation()
        val mainId = compilationUnitId { value = ":|:|jvm|main" }
        val testId = compilationUnitId { value = ":|:|jvm|test" }
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectModel.id)
        assertEquals(listOf(mainId, testId), projectModel.compilationUnitIdsList)

        assertCompilationUnit(
            provider.compilationUnit(mainId),
            mainId,
            "main",
            false,
            listOf(output("build/classes/kotlin/main", ":compileKotlin")),
        )
        assertCompilationUnit(
            provider.compilationUnit(testId),
            testId,
            "test",
            true,
            listOf(output("build/classes/kotlin/test", ":compileTestKotlin")),
        )
    }

    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces effective compiler arguments for a JVM compilation`() {
        val project = buildProjectWithJvm {
            kotlinJvmExtension.compilerOptions {
                optIn.add("my.custom.OptInAnnotation")
                freeCompilerArgs.add("-Xdebug")
            }
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)
        val mainId = provider.projectInformation().compilationUnitIdsList.first()

        val model = provider.compilerArguments(mainId)

        assertEquals(KotlinImportModelIds.COMPILER_ARGUMENTS, model.id)
        assertEquals(mainId, model.parameters.compilationUnitId)
        assertTrue("-Xdebug" in model.argumentsList)
        assertTrue("-opt-in my.custom.OptInAnnotation" in model.argumentsList.joinToString(" "))
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedIsTest: Boolean,
        expectedOutputs: List<CompilationUnitModel.Output>,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.name)
        assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, model.platform)
        assertEquals(expectedIsTest, model.isTest)
        assertEquals(expectedOutputs, model.outputsList)
        assertEquals(
            when (expectedName) {
                "main" -> listOf(
                    sourceRoot(
                        "src/main/generated-kotlin",
                        SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                        gradleAction(":generateMainSources"),
                    ),
                    sourceRoot("src/main/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/main/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                )
                "test" -> listOf(
                    sourceRoot("src/test/java", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                    sourceRoot("src/test/kotlin", SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE),
                )
                else -> error("Unexpected compilation name: $expectedName")
            },
            model.sourceRootsList,
        )
    }

    private fun sourceRoot(path: String, kind: SourceRoot.Kind, vararg producingActions: Action): SourceRoot = sourceRootModel {
        this.path = path
        this.kind = kind
        this.producingActions += producingActions.asIterable()
    }

    private fun gradleAction(taskPath: String): Action = action {
        gradleAction = ActionKt.gradleTask { this.taskPath = taskPath }
    }

    private fun output(path: String, vararg producingTaskPaths: String): CompilationUnitModel.Output = CompilationUnitModelKt.output {
        this.path = path
        producingActions += producingTaskPaths.map(::gradleAction)
    }
}
