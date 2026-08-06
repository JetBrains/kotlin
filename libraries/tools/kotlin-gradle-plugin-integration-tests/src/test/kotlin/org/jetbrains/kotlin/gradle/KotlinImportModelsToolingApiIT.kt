/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildController
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.ErrorType
import org.jetbrains.kotlin.importmodels.proto.Platform
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JvmGradlePluginTests
class KotlinImportModelsToolingApiIT : KGPBaseTest() {
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    fun `Tooling API returns stable JVM import models`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
            ),
        ) {
            val first = runBuildAction(KotlinImportModelsBuildAction())
            val second = runBuildAction(KotlinImportModelsBuildAction())

            val firstModels = first.map(Result::parseFrom)
            val secondModels = second.map(Result::parseFrom)
            val base = firstModels[0].model.unpack(BaseModel::class.java)
            val project = firstModels[1].model.unpack(ProjectModel::class.java)
            val compilationUnits = firstModels.drop(2).dropLast(1).map { it.model.unpack(CompilationUnitModel::class.java) }
            val main = compilationUnits.single { it.compilationName == "main" }
            val test = compilationUnits.single { it.compilationName == "test" }

            assertEquals(KotlinImportModelIds.BASE, base.id)
            assertEquals(listOf("main", "test"), compilationUnits.map { it.compilationName })
            assertEquals(project.compilationUnitIdsList, compilationUnits.map { it.parameters.compilationUnitId })
            assertEquals(Platform.PLATFORM_JVM, main.platform)
            assertFalse(main.isTest)
            assertEquals(":compileKotlin", main.compileTaskPath)
            assertEquals(Platform.PLATFORM_JVM, test.platform)
            assertTrue(test.isTest)
            assertEquals(":compileTestKotlin", test.compileTaskPath)
            assertEquals(
                firstModels[1].model.unpack(ProjectModel::class.java).compilationUnitIdsList,
                secondModels[1].model.unpack(ProjectModel::class.java).compilationUnitIdsList,
            )
            assertTrue(firstModels.last().hasError())
            assertEquals(ErrorType.ERROR_TYPE_UNKNOWN_MODEL_ID, firstModels.last().error.errorType)
        }
    }
}

private class KotlinImportModelsBuildAction : org.gradle.tooling.BuildAction<List<ByteArray>> {
    override fun execute(controller: BuildController): List<ByteArray> {
        fun request(modelId: String, parameters: ByteArray = byteArrayOf()): ByteArray = controller.getModel(
            KotlinGradleModel::class.java,
            ModelRequest::class.java,
        ) { request ->
            request.kotlinModelId = modelId
            request.kotlinModelParameters = parameters
        }.kotlinModelResult

        val result = mutableListOf(request(KotlinImportModelIds.BASE), request(KotlinImportModelIds.PROJECT_INFORMATION))
        val projectResult = Result.parseFrom(result[1])
        check(projectResult.hasModel()) {
            "Project import model request failed: ${projectResult.error.errorType}: ${projectResult.error.errorMessage}"
        }
        check(projectResult.model.`is`(ProjectModel::class.java)) {
            "Expected projectInformation model but received '${projectResult.model.typeUrl}'"
        }
        val project = projectResult.model.unpack(ProjectModel::class.java)
        result += project.compilationUnitIdsList.map { compilationUnitId ->
            request(
                KotlinImportModelIds.COMPILATION_UNIT,
                CompilationUnitModel.Parameters.newBuilder().setCompilationUnitId(compilationUnitId).build().toByteArray(),
            )
        }
        result += request("unknown")
        return result
    }
}
