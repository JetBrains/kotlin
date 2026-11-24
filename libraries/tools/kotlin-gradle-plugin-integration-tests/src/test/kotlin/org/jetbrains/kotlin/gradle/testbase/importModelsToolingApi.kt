/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.importmodel.KotlinImportModelProducer
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.ModelRequest
import java.io.Serializable

/**
 * One model request the project serves, as the Tooling API client sees it:
 * the model id and its serialized parameters, or null when the request has none.
 */
interface KotlinImportModelRequest : Serializable {
    val modelId: String?
    val parameters: ByteArray?
}

class KotlinImportModelRequestImpl(
    override val modelId: String?,
    override val parameters: ByteArray?,
) : KotlinImportModelRequest

/**
 * The requests a project serves, fetched through [exposeKotlinImportModelRequests].
 */
interface KotlinImportModelRequests : Serializable {
    val requests: List<KotlinImportModelRequest>
}

class KotlinImportModelRequestsImpl(override val requests: List<KotlinImportModelRequest>) : KotlinImportModelRequests

/**
 * The Result bytes the Tooling API builder served for one [request].
 */
class KotlinImportModelResponse(
    val request: KotlinImportModelRequest,
    val resultBytes: ByteArray,
) : Serializable

/**
 * The responses of one Tooling API walk: one per enumerated request, one per probe request,
 * plus the bytes of the call that passes no request at all.
 */
class KotlinImportModelsBuildActionResult(
    val enumeratedResponses: List<KotlinImportModelResponse>,
    val probeResponses: List<KotlinImportModelResponse>,
    val resultWithoutRequest: ByteArray,
) : Serializable

/**
 * Serves every enumerated request, plus [probeRequests], through the Tooling API,
 * so a test walks the models a project serves without hardcoding the catalog.
 *
 * Gradle infers the payload's classpath by walking the classes the action references, and
 * the test classpath is a partial view of the build's, so the action stays at JDK and
 * Tooling API types; its serve helper is a member for this reason.
 */
class KotlinImportModelsBuildAction(
    private val probeRequests: List<KotlinImportModelRequest> = emptyList(),
) : BuildAction<KotlinImportModelsBuildActionResult> {
    override fun execute(controller: BuildController): KotlinImportModelsBuildActionResult {
        val enumeratedRequests = controller.getModel(KotlinImportModelRequests::class.java).requests
        return KotlinImportModelsBuildActionResult(
            enumeratedResponses = enumeratedRequests.map { request ->
                KotlinImportModelResponse(request, controller.serve(request.modelId, request.parameters))
            },
            probeResponses = probeRequests.map { request ->
                KotlinImportModelResponse(request, controller.serve(request.modelId, request.parameters))
            },
            resultWithoutRequest = controller.getModel(KotlinGradleModel::class.java).kotlinModelResult,
        )
    }

    // The initializer serializes with the action, so its captures' classes must resolve on the build side
    private fun BuildController.serve(modelId: String?, parameters: ByteArray?): ByteArray = getModel(
        KotlinGradleModel::class.java,
        ModelRequest::class.java,
    ) { parameter ->
        parameter.kotlinModelId = modelId
        parameter.kotlinModelParameters = parameters
    }.kotlinModelResult
}

/**
 * Injects a test model builder that exposes the producer's enumerate operation as
 * [KotlinImportModelRequests], so the Tooling API client can walk the requests the project serves.
 */
fun GradleProject.exposeKotlinImportModelRequests() = buildScriptInjection {
    project.serviceOf<ToolingModelBuilderRegistry>().register(
        KotlinTestModelBuilder(KotlinImportModelRequests::class.java.name) { project: Project ->
            KotlinImportModelRequestsImpl(
                KotlinImportModelProducer(project).enumerateRequests().map { request ->
                    KotlinImportModelRequestImpl(request.modelId, request.parameters)
                }
            )
        }
    )
}

/**
 * The build-option sets model retrieval is contract-tested under: plain, and with the
 * configuration cache plus Isolated Projects, the mode IDEA imports in; with both enabled,
 * Gradle can cache the built models and serve a later request from the cache.
 */
fun KGPBaseTest.modelRetrievalBuildOptions(): List<BuildOptions> = listOf(
    defaultBuildOptions.copy(
        configurationCache = BuildOptions.ConfigurationCacheValue.DISABLED,
        isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED,
    ),
    defaultBuildOptions.copy(
        configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
        isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
    ),
)
