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
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
    fun `forwards every request to the producer's serve operation`() {
        val project = projectWithJvm()
        // The producer's tests pin what each request answers; this test pins only the forwarding
        listOf(
            TestModelRequest(KotlinImportModelIds.BASE, null),
            TestModelRequest(KotlinImportModelIds.PROJECT_INFORMATION, null),
            TestModelRequest("unknownModel", null),
            // The Tooling API passes the request as a proxy, so fields the IDE didn't set arrive as null
            TestModelRequest(null, null),
            TestModelRequest(KotlinImportModelIds.BASE, byteArrayOf(0)),
        ).forEach { request ->
            assertContentEquals(
                KotlinImportModelProducer(project).serveRequest(request.kotlinModelId, request.kotlinModelParameters),
                builder.buildAll(KotlinGradleModel::class.java.name, request, project).kotlinModelResult,
            )
        }
    }

    @Test
    fun `answers a request without parameters with UNKNOWN_MODEL_PARAMS`() {
        // The Tooling API calls this overload when the client requested no ModelRequest at all
        val result = builder.buildAll(KotlinGradleModel::class.java.name, projectWithJvm()).result()

        assertEquals(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, result.error.errorType)
    }

    private fun Any?.result(): Result = Result.parseFrom((this as KotlinGradleModel).kotlinModelResult)

    private fun projectWithJvm(): Project = buildProjectWithJvm { }.also { it.evaluate() }
}

private class TestModelRequest(
    override var kotlinModelId: String?,
    override var kotlinModelParameters: ByteArray?,
) : ModelRequest
