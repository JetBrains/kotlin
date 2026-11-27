/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.UnknownModelException
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.Error
import org.jetbrains.kotlin.importmodels.proto.Result
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@MustRunOnChangesInGradle
class KotlinImportModelsToolingApiIT : KGPBaseTest() {
    @GradleTest
    @JvmGradlePluginTests
    fun `serves the enumerated requests over the Tooling API`(gradleVersion: GradleVersion) {
        modelRetrievalBuildOptions().forEach { buildOptions ->
            project(projectName = "simpleProject", gradleVersion = gradleVersion, buildOptions = buildOptions) {
                exposeKotlinImportModelRequests()

                val action = KotlinImportModelsBuildAction(
                    probeRequests = listOf(
                        KotlinImportModelRequestImpl("unknownModel", null),
                        KotlinImportModelRequestImpl(null, null),
                        KotlinImportModelRequestImpl(KotlinImportModelIds.BASE, byteArrayOf(0)),
                    )
                )
                val first = runBuildAction(action)
                val second = runBuildAction(action)

                val servedIds = first.enumeratedResponses.map { it.request.modelId }
                assertTrue(KotlinImportModelIds.BASE in servedIds)
                assertTrue(KotlinImportModelIds.PROJECT_INFORMATION in servedIds)

                first.enumeratedResponses.forEach { response ->
                    assertEquals(Result.ResultCase.MODEL, Result.parseFrom(response.resultBytes).resultCase)
                    assertImportModelsGoldenResult(
                        response.resultBytes,
                        projectPath,
                        "simpleProject",
                        "${response.request.modelId}.txt",
                    )
                }

                // Bad requests answer their error types; the call itself never fails
                assertEquals(
                    listOf(
                        Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID,
                        Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID,
                        Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS,
                    ),
                    first.probeResponses.map { it.errorType() },
                )
                assertEquals(
                    Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS,
                    Result.parseFrom(first.resultWithoutRequest).error.errorType,
                )

                // The served bytes are identical across walks; with the configuration cache
                // plus Isolated Projects the second walk can come from the cache
                assertEquals(
                    first.enumeratedResponses.map { it.request.modelId },
                    second.enumeratedResponses.map { it.request.modelId },
                )
                first.enumeratedResponses.zip(second.enumeratedResponses).forEach { (firstResponse, secondResponse) ->
                    assertContentEquals(firstResponse.request.parameters, secondResponse.request.parameters)
                    assertContentEquals(firstResponse.resultBytes, secondResponse.resultBytes)
                }
                assertContentEquals(first.resultWithoutRequest, second.resultWithoutRequest)
            }
        }
    }

    @GradleTest
    @JvmGradlePluginTests
    fun `a project without the Kotlin plugin serves no model`(gradleVersion: GradleVersion) {
        project(projectName = "empty", gradleVersion = gradleVersion) {
            // assertFailsWith would catch the Windows abort, so the skip must come before it
            assumeBuildActionsSupported()
            // The Tooling API fails the build action, with the request's UnknownModelException as the cause
            val failure = assertFailsWith<Throwable> { runBuildAction(KotlinGradleModelBuildAction()) }
            assertIs<UnknownModelException>(failure.cause)
        }
    }
}

private class KotlinGradleModelBuildAction : BuildAction<ByteArray> {
    override fun execute(controller: BuildController): ByteArray =
        controller.getModel(KotlinGradleModel::class.java, ModelRequest::class.java) { }.kotlinModelResult
}

private fun KotlinImportModelResponse.errorType(): Error.Type = Result.parseFrom(resultBytes).error.errorType
