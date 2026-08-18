/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.util.JsonFormat
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModelKt
import org.jetbrains.kotlin.importmodels.proto.CompilerArgumentsModel
import org.jetbrains.kotlin.importmodels.proto.DependenciesModel
import org.jetbrains.kotlin.importmodels.proto.DependenciesModelKt
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.SourceRoot
import org.jetbrains.kotlin.importmodels.proto.Action
import org.jetbrains.kotlin.importmodels.proto.action as actionModel
import org.jetbrains.kotlin.importmodels.proto.ActionKt.gradleTask as gradleTaskModel
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JvmGradlePluginTests
class KotlinImportModelsDumpIT : KGPBaseTest() {
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `dump task writes JSON JVM models without compilation`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                // The diagnostic dump task reads live project state
                configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
                runViaBuildToolsApi = true,
                generateCompilerRefIndex = true,
            ),
        ) {
            buildScriptInjection {
                kotlinJvm.compilerOptions {
                    optIn.add("my.custom.OptInAnnotation")
                    freeCompilerArgs.add("-Xdebug")
                }
                val generateImportModelSources = project.tasks.register("generateImportModelSources") {
                    it.outputs.dir(project.layout.buildDirectory.dir("generated/import-models"))
                }
                kotlinJvm.sourceSets.getByName("main").generatedKotlin.srcDir(generateImportModelSources)
            }
            build("dumpKotlinImportModels") {
                assertTasksExecuted(":dumpKotlinImportModels")
                assertTasksAreNotInTaskGraph(
                    ":compileJava",
                    ":compileKotlin",
                    ":compileTestJava",
                    ":compileTestKotlin",
                    ":compileDeployJava",
                    ":compileDeployKotlin",
                )
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
        val compilerArguments = listOf("main", "test").map { name ->
            parseCompilerArguments(root.resolve("compiler-arguments/$name.json"))
        }
        val dependencies = listOf("main", "test").map { name -> parseDependencies(root.resolve("dependencies/$name.json")) }
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, project.id)
        assertEquals(listOf("main", "test"), units.map { it.name })
        assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
        assertEquals(project.compilationUnitIdsList, compilerArguments.map { it.parameters.compilationUnitId })
        assertEquals(project.compilationUnitIdsList, dependencies.map { it.parameters.compilationUnitId })
        assertEquals(listOf(KotlinImportModelIds.COMPILER_ARGUMENTS, KotlinImportModelIds.COMPILER_ARGUMENTS), compilerArguments.map { it.id })
        assertEquals(listOf(KotlinImportModelIds.DEPENDENCIES, KotlinImportModelIds.DEPENDENCIES), dependencies.map { it.id })
        assertTrue("-Xdebug" in compilerArguments.first().argumentsList)
        assertTrue("-opt-in my.custom.OptInAnnotation" in compilerArguments.first().argumentsList.joinToString(" "))
        assertTrue(dependencies.all { it.binaryDependenciesList.isNotEmpty() })
        assertEquals(emptyList(), dependencies.first().sourceDependenciesList)
        assertEquals(
            listOf(
                DependenciesModelKt.sourceDependency {
                    kind = DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_FRIEND
                    targetCompilationUnitId = units.first().parameters.compilationUnitId
                }
            ),
            dependencies.last().sourceDependenciesList,
        )
        assertEquals(
            listOf(
                sourceRoot(
                    "build/generated/import-models",
                    SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED,
                    ":generateImportModelSources",
                ),
                sourceRoot("src/main/java"),
                sourceRoot("src/main/kotlin"),
            ),
            units.first().sourceRootsList,
        )
        assertEquals(
            listOf(
                output("build/classes/kotlin/main", ":compileKotlin"),
                output("build/kotlin/compileKotlin/cacheable/cri", ":compileKotlin"),
            ),
            units.first().outputsList,
        )
        assertEquals(
            listOf(sourceRoot("src/test/java"), sourceRoot("src/test/kotlin")),
            units.last().sourceRootsList,
        )
        assertEquals(
            listOf(
                output("build/classes/kotlin/test", ":compileTestKotlin"),
                output("build/kotlin/compileTestKotlin/cacheable/cri", ":compileTestKotlin"),
            ),
            units.last().outputsList,
        )
        return project.compilationUnitIdsList
    }
}

private fun sourceRoot(
    path: String,
    kind: SourceRoot.Kind = SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE,
    vararg producingTaskPaths: String,
): SourceRoot = sourceRootModel {
    this.path = path
    this.kind = kind
    producingActions += producingTaskPaths.map(::gradleAction)
}

private fun gradleAction(taskPath: String): Action = actionModel {
    gradleAction = gradleTaskModel { this.taskPath = taskPath }
}

private fun output(path: String, vararg producingTaskPaths: String): CompilationUnitModel.Output = CompilationUnitModelKt.output {
    this.path = path
    producingActions += producingTaskPaths.map(::gradleAction)
}

private fun parseBase(file: File): BaseModel = BaseModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseProject(file: File): ProjectModel = ProjectModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseCompilation(file: File): CompilationUnitModel =
    CompilationUnitModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseCompilerArguments(file: File): CompilerArgumentsModel =
    CompilerArgumentsModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseDependencies(file: File): DependenciesModel =
    DependenciesModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()
