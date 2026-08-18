/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KotlinImportModelProviderTest {
    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `produces stable main and test JVM models`() {
        val project = buildProjectWithJvm {
            extraProperties.set("kotlin.compiler.runViaBuildToolsApi", true)
            extraProperties.set("kotlin.compiler.generateCompilerRefIndex", true)
            kotlinJvmExtension.target.compilations.create("deploy")
            val generateMainSources = tasks.register("generateMainSources") {
                it.outputs.dir(layout.projectDirectory.dir("src/main/generated-kotlin"))
            }
            kotlinJvmExtension.sourceSets.getByName("main").generatedKotlin.srcDir(generateMainSources)
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

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
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
            listOf(
                output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin"),
                output("build/kotlin/compileKotlin/cacheable/cri", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI, ":compileKotlin"),
            ),
        )
        assertCompilationUnit(
            provider.compilationUnit(testId),
            testId,
            "test",
            CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST,
            listOf(
                output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin"),
                output("build/kotlin/compileTestKotlin/cacheable/cri", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI, ":compileTestKotlin"),
            ),
        )
        listOf(
            "classes/kotlin/main",
            "kotlin/compileKotlin/cacheable/cri",
            "classes/kotlin/test",
            "kotlin/compileTestKotlin/cacheable/cri",
        ).forEach { relativePath ->
            assertFalse(project.layout.buildDirectory.dir(relativePath).get().asFile.exists())
        }
    }

    @Test
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `does not declare CRI outputs when Build Tools API is disabled`() {
        val project = buildProjectWithJvm {
            extraProperties.set("kotlin.compiler.runViaBuildToolsApi", false)
            extraProperties.set("kotlin.compiler.generateCompilerRefIndex", true)
        }
        project.evaluate()
        val provider = KotlinImportModelProvider(project)

        val compilationUnits = provider.projectInformation().compilationUnitIdsList.associateBy { compilationUnitId ->
            provider.compilationUnit(compilationUnitId).name
        }

        assertEquals(
            listOf(output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin")),
            provider.compilationUnit(compilationUnits.getValue("main")).outputsList,
        )
        assertEquals(
            listOf(output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin")),
            provider.compilationUnit(compilationUnits.getValue("test")).outputsList,
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

    @Test
    fun `escapes compilation unit ID components without collisions`() {
        assertEquals(
            ":included%7Cbuild|:app%25demo|jvm|test%7Cfixture",
            compilationUnitIdValue(":included|build", ":app%demo", "jvm", "test|fixture"),
        )
        assertNotEquals(
            compilationUnitIdValue("a|b", "c", "d", "e"),
            compilationUnitIdValue("a", "b", "c", "d|e"),
        )
    }

    private fun assertCompilationUnit(
        model: CompilationUnitModel,
        expectedId: CompilationUnitId,
        expectedName: String,
        expectedPurpose: CompilationUnitModel.Purpose,
        expectedOutputs: List<CompilationUnitModel.Output>,
    ) {
        assertEquals(KotlinImportModelIds.COMPILATION_UNIT, model.id)
        assertEquals(expectedId, model.parameters.compilationUnitId)
        assertEquals(expectedName, model.name)
        assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, model.platform)
        assertEquals(expectedPurpose, model.purpose)
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

    private fun output(
        path: String,
        kind: CompilationUnitModel.Output.Kind,
        vararg producingTaskPaths: String,
    ): CompilationUnitModel.Output = CompilationUnitModelKt.output {
        this.path = path
        this.kind = kind
        producingActions += producingTaskPaths.map(::gradleAction)
    }
}
