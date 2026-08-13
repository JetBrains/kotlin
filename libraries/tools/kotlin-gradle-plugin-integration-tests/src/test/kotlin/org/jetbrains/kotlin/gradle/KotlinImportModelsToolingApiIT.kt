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
import org.jetbrains.kotlin.importmodels.proto.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@JvmGradlePluginTests
class KotlinImportModelsToolingApiIT : KGPBaseTest() {
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    fun `Tooling API returns stable basic JVM import models`(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
                isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
            ),
        ) {
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
            assertTrue(units.last().isTest)
            assertEquals(project.compilationUnitIdsList, second[1].model.unpack(ProjectModel::class.java).compilationUnitIdsList)
        }
    }
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
