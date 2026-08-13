/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildController
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
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
                val generateImportModelSources = project.tasks.register("generateImportModelSources") {
                    it.outputs.dir(project.layout.buildDirectory.dir("generated/import-models"))
                }
                kotlinJvm.sourceSets.getByName("main").generatedKotlin.srcDir(generateImportModelSources)
            }
            val first = runBuildAction(KotlinImportModelsBuildAction()).map(Result::parseFrom)
            val second = runBuildAction(KotlinImportModelsBuildAction()).map(Result::parseFrom)
            val base = first[0].model.unpack(BaseModel::class.java)
            val project = first[1].model.unpack(ProjectModel::class.java)
            val units = first.drop(2).map { it.model.unpack(CompilationUnitModel::class.java) }

            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(listOf("main", "test"), units.map { it.name })
            assertEquals(project.compilationUnitIdsList, units.map { it.parameters.compilationUnitId })
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
            assertEquals(project.compilationUnitIdsList, second[1].model.unpack(ProjectModel::class.java).compilationUnitIdsList)
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

private class KotlinImportModelsBuildAction : org.gradle.tooling.BuildAction<List<ByteArray>> {
    override fun execute(controller: BuildController): List<ByteArray> {
        fun request(modelId: String, parameters: ByteArray? = null): ByteArray = controller.getModel(
            KotlinGradleModel::class.java,
            ModelRequest::class.java,
        ) { request ->
            request.kotlinModelId = modelId
            request.kotlinModelParameters = parameters
        }.kotlinModelResult

        val base = request(KotlinImportModelIds.BASE)
        val projectInformation = request(KotlinImportModelIds.PROJECT_INFORMATION)
        val project = Result.parseFrom(projectInformation).model.unpack(ProjectModel::class.java)
        return listOf(base, projectInformation) + project.compilationUnitIdsList.map { compilationUnitId ->
            request(
                KotlinImportModelIds.COMPILATION_UNIT,
                CompilationUnitModelKt.parameters { this.compilationUnitId = compilationUnitId }.toByteArray(),
            )
        }
    }
}
