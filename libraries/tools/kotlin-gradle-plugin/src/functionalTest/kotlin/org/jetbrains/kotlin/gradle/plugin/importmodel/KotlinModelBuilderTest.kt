/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.ErrorType
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinModelBuilderTest {
    private val builder = KotlinModelBuilder()

    @Test
    fun `returns the base model`() {
        val result = builder.buildResult(KotlinImportModelIds.BASE, projectWithJvm())

        assertTrue(result.hasModel())
        assertEquals(KotlinImportModelIds.BASE, result.model.unpack(BaseModel::class.java).id)
    }

    @Test
    fun `returns the project model`() {
        val result = builder.buildResult(KotlinImportModelIds.PROJECT_INFORMATION, projectWithJvm())

        assertTrue(result.hasModel())
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, result.model.unpack(ProjectModel::class.java).id)
    }

    @Test
    fun `returns the requested compilation model`() {
        val project = projectWithJvm()
        val compilationId = KotlinImportModelProvider(project).projectInformation().compilationUnitIdsList.first()
        val parameters = CompilationUnitModel.Parameters.newBuilder()
            .setCompilationUnitId(compilationId)
            .build()
            .toByteArray()

        val result = builder.buildResult(KotlinImportModelIds.COMPILATION_UNIT, project, parameters)

        assertTrue(result.hasModel())
        assertEquals(compilationId, result.model.unpack(CompilationUnitModel::class.java).parameters.compilationUnitId)
    }

    @Test
    fun `reports an unknown model ID`() {
        assertError("unknown", projectWithJvm(), ErrorType.UNKNOWN_MODEL_ID)
    }

    @Test
    fun `reports missing compilation parameters`() {
        assertError(KotlinImportModelIds.COMPILATION_UNIT, projectWithJvm(), ErrorType.UNKNOWN_MODEL_PARAMS)
    }

    @Test
    fun `reports absent compilation parameters`() {
        val result = Result.parseFrom(
            (builder.buildAll(
                KotlinGradleModel::class.java.name,
                TestModelRequest(KotlinImportModelIds.COMPILATION_UNIT, null),
                projectWithJvm(),
            ) as KotlinGradleModel).kotlinModelResult
        )

        assertEquals(ErrorType.UNKNOWN_MODEL_PARAMS, result.error.errorType)
    }

    @Test
    fun `reports malformed compilation parameters`() {
        assertError(KotlinImportModelIds.COMPILATION_UNIT, projectWithJvm(), ErrorType.UNKNOWN_MODEL_PARAMS, byteArrayOf(10, 1))
    }

    @Test
    fun `reports an unsupported compilation ID`() {
        val parameters = CompilationUnitModel.Parameters.newBuilder()
            .setCompilationUnitId(CompilationUnitId.newBuilder().setValue("unknown").build())
            .build()
            .toByteArray()

        assertError(KotlinImportModelIds.COMPILATION_UNIT, projectWithJvm(), ErrorType.UNSUPPORTED_MODEL_PARAMS, parameters)
    }

    @Test
    fun `reports parameters for a parameterless model as unsupported`() {
        assertError(KotlinImportModelIds.BASE, projectWithJvm(), ErrorType.UNSUPPORTED_MODEL_PARAMS, byteArrayOf(1))
    }

    @Test
    fun `reports unparameterized requests as unknown parameters`() {
        val result = Result.parseFrom(
            (builder.buildAll(KotlinGradleModel::class.java.name, projectWithJvm()) as KotlinGradleModel).kotlinModelResult
        )

        assertEquals(ErrorType.UNKNOWN_MODEL_PARAMS, result.error.errorType)
    }

    @Test
    fun `reports unexpected provider failures as internal errors`() {
        assertError(KotlinImportModelIds.PROJECT_INFORMATION, buildProject { }, ErrorType.INTERNAL_ERROR)
    }

    private fun assertError(modelId: String, project: Project, type: ErrorType, parameters: ByteArray = byteArrayOf()) {
        val result = builder.buildResult(modelId, project, parameters)
        assertTrue(result.hasError())
        assertEquals(type, result.error.errorType)
    }

    private fun KotlinModelBuilder.buildResult(modelId: String, project: Project, parameters: ByteArray = byteArrayOf()): Result =
        Result.parseFrom(
            (buildAll(KotlinGradleModel::class.java.name, TestModelRequest(modelId, parameters), project) as KotlinGradleModel)
                .kotlinModelResult
        )

    private fun projectWithJvm(): Project = buildProjectWithJvm { }.also { it.evaluate() }
}

private class TestModelRequest(
    override var kotlinModelId: String?,
    override var kotlinModelParameters: ByteArray?,
) : ModelRequest
