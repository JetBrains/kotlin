/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization.errorResult
import org.jetbrains.kotlin.importmodels.proto.Error
import javax.inject.Inject

/**
 * Registers [KotlinImportModelBuilder] on every project a Kotlin plugin is applied to,
 * so every Kotlin project serves its models and projects without the plugin serve none.
 */
internal val KotlinImportModelBuilderSetupAction = KotlinProjectSetupAction {
    objects.newInstance<KotlinImportModelBuilderRegistrar>().register()
}

internal abstract class KotlinImportModelBuilderRegistrar @Inject constructor(
    private val toolingModelBuilderRegistry: ToolingModelBuilderRegistry,
) {
    fun register() {
        toolingModelBuilderRegistry.register(KotlinImportModelBuilder())
    }
}

/**
 * Adapter that serves the import models of one Gradle project through the Tooling API:
 * it forwards a [ModelRequest] to the producer's serve operation and returns the serialized
 * `Result` bytes in a [KotlinGradleModel], so the transport never sees a Protobuf type.
 * A request the project cannot answer stays a typed error `Result`, never a failed call.
 */
internal class KotlinImportModelBuilder : ParameterizedToolingModelBuilder<ModelRequest> {
    override fun canBuild(modelName: String): Boolean = modelName == KotlinGradleModel::class.java.name

    override fun getParameterType(): Class<ModelRequest> = ModelRequest::class.java

    // The Tooling API calls this overload when the client requested no ModelRequest at all
    override fun buildAll(modelName: String, project: Project): KotlinGradleModel = KotlinGradleModelResult(
        errorResult(
            Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS,
            "Kotlin import model parameters are required",
        )
    )

    override fun buildAll(modelName: String, parameter: ModelRequest, project: Project): KotlinGradleModel =
        KotlinGradleModelResult(
            KotlinImportModelProducer(project).serveRequest(parameter.kotlinModelId, parameter.kotlinModelParameters)
        )
}

private class KotlinGradleModelResult(
    override val kotlinModelResult: ByteArray,
) : KotlinGradleModel
