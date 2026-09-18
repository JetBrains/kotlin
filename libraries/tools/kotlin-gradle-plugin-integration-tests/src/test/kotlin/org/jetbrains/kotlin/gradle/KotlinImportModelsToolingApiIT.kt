/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.importmodels.proto.action as actionModel
import org.jetbrains.kotlin.importmodels.proto.ActionKt.gradleTask as gradleTaskModel
import org.jetbrains.kotlin.importmodels.proto.sourceRoot as sourceRootModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JvmGradlePluginTests
class KotlinImportModelsToolingApiIT : KGPBaseTest() {
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun `Tooling API returns stable basic JVM import models`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
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
            // POC: the models are task outputs, so an IDE sync has to run `generateKotlinImportModels` before reading them
            val first = runBuildAction(KotlinImportModelsBuildAction(), GENERATE_TASK).toModels()
            val second = runBuildAction(KotlinImportModelsBuildAction(), GENERATE_TASK).toModels()
            val base = first.base.model.unpack(BaseModel::class.java)
            val project = first.project.model.unpack(ProjectModel::class.java)
            val units = first.compilationUnits.map { it.model.unpack(CompilationUnitModel::class.java) }
            val compilerArguments = first.compilerArguments.map { it.model.unpack(CompilerArgumentsModel::class.java) }
            val dependencies = first.dependencies.map { it.model.unpack(DependenciesModel::class.java) }
            val main = units.single { it.name == "main" }
            val test = units.single { it.name == "test" }
            val mainDependencies = dependencies.single { it.parameters.compilationUnitId == main.parameters.compilationUnitId }
            val testDependencies = dependencies.single { it.parameters.compilationUnitId == test.parameters.compilationUnitId }

            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(listOf("main", "test"), units.map { it.name })
            assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, compilerArguments.map { it.parameters.compilationUnitId })
            assertEquals(project.compilationUnitIdsList, dependencies.map { it.parameters.compilationUnitId })
            assertTrue(compilerArguments.all { it.id == KotlinImportModelIds.COMPILER_ARGUMENTS })
            assertTrue(dependencies.all { it.id == KotlinImportModelIds.DEPENDENCIES })
            assertTrue(dependencies.all { it.parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE })
            assertTrue(dependencies.all { it.parameters.coverage == DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL })
            assertTrue(dependencies.all { it.binaryDependenciesList.isNotEmpty() })
            assertEquals(emptyList(), mainDependencies.sourceDependenciesList)
            assertEquals(
                listOf(
                    DependenciesModelKt.sourceDependency {
                        kind = DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_FRIEND
                        targetCompilationUnitId = main.parameters.compilationUnitId
                    }
                ),
                testDependencies.sourceDependenciesList,
            )
            assertTrue("-Xdebug" in compilerArguments.first().argumentsList)
            assertTrue("-opt-in my.custom.OptInAnnotation" in compilerArguments.first().argumentsList.joinToString(" "))
            assertEquals(CompilationUnitModel.Platform.PLATFORM_JVM, units.first().platform)
            assertFalse(units.first().isTest)
            assertEquals(
                listOf(output("build/classes/kotlin/main", ":compileKotlin")),
                units.first().outputsList,
            )
            assertEquals(
                sourceRoots(
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
            assertTrue(units.last().isTest)
            assertEquals(
                listOf(output("build/classes/kotlin/test", ":compileTestKotlin")),
                units.last().outputsList,
            )
            assertEquals(
                sourceRoots(sourceRoot("src/test/java"), sourceRoot("src/test/kotlin")),
                units.last().sourceRootsList,
            )
            assertEquals(project.compilationUnitIdsList, second.project.model.unpack(ProjectModel::class.java).compilationUnitIdsList)
            assertEquals(first.dependencies, second.dependencies)
        }
    }
}

private fun sourceRoots(vararg roots: SourceRoot): List<SourceRoot> = roots.toList()

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
