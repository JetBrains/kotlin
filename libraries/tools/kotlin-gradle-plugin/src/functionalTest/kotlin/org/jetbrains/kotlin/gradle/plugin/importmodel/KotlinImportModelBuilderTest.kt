/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.BaseModel
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.ProjectModel
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinImportModelBuilderTest {
    private val builder = KotlinImportModelBuilder()

    @Test
    fun `builds the model the Tooling API names and takes ModelRequest parameters`() {
        assertTrue(builder.canBuild(KotlinGradleModel::class.java.name))
        assertFalse(builder.canBuild("not.a.kotlin.Model"))
        assertEquals(ModelRequest::class.java, builder.parameterType)
    }

    @Test
    fun `serves base and project information through the serve operation`() {
        val project = projectWithJvm()
        val base = builder.buildResult(KotlinImportModelIds.BASE, project).model.unpack(BaseModel::class.java)
        val projectModel = builder
            .buildResult(KotlinImportModelIds.PROJECT_INFORMATION, project)
            .model.unpack(ProjectModel::class.java)

        assertEquals(KotlinImportModelIds.BASE, base.id)
        assertEquals(listOf(BaseModel.Capability.CAPABILITY_KOTLIN_JVM), base.capabilitiesList)
        assertEquals(KotlinImportModelIds.PROJECT_INFORMATION, projectModel.id)
        assertEquals(listOf(":|:|jvm|main", ":|:|jvm|test"), projectModel.compilationUnitIdsList.map { it.value })
    }

    @Test
    fun `answers an unknown model id with UNKNOWN_MODEL_ID`() {
        val result = builder.buildResult("unknownModel", projectWithJvm())

        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, result.error.errorType)
    }

    @Test
    fun `answers a request with null fields with UNKNOWN_MODEL_ID`() {
        // The Tooling API passes the request as a proxy, so fields the IDE didn't set arrive as null
        val result = builder.buildResult(modelId = null, project = projectWithJvm(), parameters = null)

        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, result.error.errorType)
    }

    @Test
    fun `answers parameters for a parameterless model with UNSUPPORTED_MODEL_PARAMS`() {
        val result = builder.buildResult(KotlinImportModelIds.BASE, projectWithJvm(), parameters = byteArrayOf(0))

        assertEquals(Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS, result.error.errorType)
    }

    @Test
    fun `answers a request without parameters with UNKNOWN_MODEL_PARAMS`() {
        // The Tooling API calls this overload when the client requested no ModelRequest at all
        val result = builder.buildAll(KotlinGradleModel::class.java.name, projectWithJvm()).result()

        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, result.error.errorType)
    }

    private fun KotlinImportModelBuilder.buildResult(
        modelId: String?,
        project: Project,
        parameters: ByteArray? = null,
    ): Result = buildAll(KotlinGradleModel::class.java.name, TestModelRequest(modelId, parameters), project).result()

    private fun Any?.result(): Result = Result.parseFrom((this as KotlinGradleModel).kotlinModelResult)

    private fun projectWithJvm(): Project = buildProjectWithJvm { }.also { it.evaluate() }
}

private class TestModelRequest(
    override var kotlinModelId: String?,
    override var kotlinModelParameters: ByteArray?,
) : ModelRequest
