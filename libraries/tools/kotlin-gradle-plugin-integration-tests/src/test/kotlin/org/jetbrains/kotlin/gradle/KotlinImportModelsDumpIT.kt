/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.gradle.kotlin.dsl.kotlin
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

class KotlinImportModelsDumpIT : KGPBaseTest() {
    @GradleTest
    @JvmGradlePluginTests
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

    @GradleTest
    @MppGradlePluginTests
    fun `dump task writes distinct ordered KMP JSON models including test compilations without compilation`(gradleVersion: GradleVersion) {
        project(
            projectName = "empty",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()
            }
            build("dumpKotlinImportModels") {
                assertTasksExecuted(":dumpKotlinImportModels")
                assertTasksAreNotInTaskGraph(
                    ":compileCommonMainKotlinMetadata",
                    ":compileKotlinJvm",
                    ":compileKotlinLinuxX64",
                    ":compileTestKotlinJvm",
                    ":compileTestKotlinLinuxX64",
                )
            }
            assertKmpDump()
        }
    }

    private fun TestProject.assertDump(): List<CompilationUnitId> {
        val root = projectPath.resolve("build/kotlin-import-models").toFile()
        parseBase(root.resolve("base.json")).also { base ->
            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), base.capabilitiesList)
        }
        val project = parseProject(root.resolve("project.json"))
        val units = parseDumpModels(root, "compilation-units", ::parseCompilation)
        val compilerArguments = parseDumpModels(root, "compiler-arguments", ::parseCompilerArguments)
        val dependencies = parseDumpModels(root, "dependencies", ::parseDependencies)
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, project.id)
        assertDumpFileNames(root, listOf("000-jvm-main.json", "001-jvm-test.json"))
        assertEquals(listOf("main", "test"), units.map { it.name })
        assertEquals(
            listOf(
                listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
                listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
            ),
            units.map(CompilationUnitModel::getTargetPlatformsList),
        )
        assertEquals(listOf("jvm", "jvm"), units.map(CompilationUnitModel::getTargetName))
        assertTrue(units.all(CompilationUnitModel::hasTargetName))
        assertEquals(
            listOf(
                CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
                CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST,
            ),
            units.map { it.purpose },
        )
        assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
        assertEquals(project.compilationUnitIdsList, compilerArguments.map { it.parameters.compilationUnitId })
        assertEquals(project.compilationUnitIdsList, dependencies.map { it.parameters.compilationUnitId })
        assertEquals(
            listOf(KotlinImportModelIds.COMPILER_ARGUMENTS, KotlinImportModelIds.COMPILER_ARGUMENTS),
            compilerArguments.map { it.id })
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
                output("build/classes/kotlin/main", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileKotlin"),
                output("build/kotlin/compileKotlin/cacheable/cri", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI, ":compileKotlin"),
            ),
            units.first().outputsList,
        )
        assertEquals(
            listOf(sourceRoot("src/test/java"), sourceRoot("src/test/kotlin")),
            units.last().sourceRootsList,
        )
        assertEquals(
            listOf(
                output("build/classes/kotlin/test", CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES, ":compileTestKotlin"),
                output(
                    "build/kotlin/compileTestKotlin/cacheable/cri",
                    CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI,
                    ":compileTestKotlin"
                ),
            ),
            units.last().outputsList,
        )
        return project.compilationUnitIdsList
    }

    private fun TestProject.assertKmpDump() {
        val root = projectPath.resolve("build/kotlin-import-models").toFile()
        assertEquals(
            listOf(BaseModel.Capability.CAPABILITY_KOTLIN_MULTIPLATFORM),
            parseBase(root.resolve("base.json")).capabilitiesList,
        )
        val project = parseProject(root.resolve("project.json"))
        val expectedIds = listOf(
            ":|:|jvm|main",
            ":|:|jvm|test",
            ":|:|linuxX64|main",
            ":|:|linuxX64|test",
            ":|:|metadata|commonMain",
        )
        assertEquals(expectedIds, project.compilationUnitIdsList.map(CompilationUnitId::getValue))
        assertDumpFileNames(
            root,
            listOf(
                "000-jvm-main.json",
                "001-jvm-test.json",
                "002-linuxX64-main.json",
                "003-linuxX64-test.json",
                "004-metadata-commonMain.json",
            ),
        )
        assertKmpDumpModels(root, "compilation-units", expectedIds, ::parseCompilation) { it.parameters.compilationUnitId.value }
        assertKmpDumpModels(root, "compiler-arguments", expectedIds, ::parseCompilerArguments) { it.parameters.compilationUnitId.value }
        assertKmpDumpModels(root, "dependencies", expectedIds, ::parseDependencies) { it.parameters.compilationUnitId.value }
    }

    private fun <T> parseDumpModels(root: File, directory: String, parser: (File) -> T): List<T> =
        dumpJsonFiles(root, directory).map(parser)

    private fun assertDumpFileNames(root: File, expectedFileNames: List<String>) {
        listOf("compilation-units", "compiler-arguments", "dependencies").forEach { directory ->
            assertEquals(expectedFileNames, dumpJsonFiles(root, directory).map(File::getName))
        }
    }

    private fun <T> assertKmpDumpModels(
        root: File,
        directory: String,
        expectedIds: List<String>,
        parser: (File) -> T,
        id: (T) -> String,
    ) {
        val files = dumpJsonFiles(root, directory)
        assertEquals(expectedIds.indices.map { "%03d-".format(it) }, files.map { it.name.take(4) })
        assertEquals(expectedIds, files.map(parser).map(id))
    }
}

private fun dumpJsonFiles(root: File, directory: String): List<File> = root.resolve(directory)
    .listFiles { file -> file.isFile && file.extension == "json" }
    .orEmpty()
    .sortedBy(File::getName)

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

private fun output(
    path: String,
    kind: CompilationUnitModel.Output.Kind,
    vararg producingTaskPaths: String,
): CompilationUnitModel.Output = CompilationUnitModelKt.output {
    this.path = path
    this.kind = kind
    producingActions += producingTaskPaths.map(::gradleAction)
}

private fun parseBase(file: File): BaseModel = BaseModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseProject(file: File): ProjectModel =
    ProjectModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseCompilation(file: File): CompilationUnitModel =
    CompilationUnitModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseCompilerArguments(file: File): CompilerArgumentsModel =
    CompilerArgumentsModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()

private fun parseDependencies(file: File): DependenciesModel =
    DependenciesModel.newBuilder().also { JsonFormat.parser().merge(file.readText(), it) }.build()
