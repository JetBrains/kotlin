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
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.Any
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.InvalidProtocolBufferException
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.Message
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import org.jetbrains.kotlin.importmodels.proto.Error as ImportModelError
import org.jetbrains.kotlin.importmodels.proto.ErrorType
import org.jetbrains.kotlin.importmodels.proto.Result
import javax.inject.Inject

internal val KotlinModelBuilderSetupAction = KotlinProjectSetupAction {
    objects.newInstance<KotlinModelBuilderRegistrar>().register()
}

internal abstract class KotlinModelBuilderRegistrar @Inject constructor(
    private val toolingModelBuilderRegistry: ToolingModelBuilderRegistry,
) {
    fun register() {
        toolingModelBuilderRegistry.register(KotlinModelBuilder())
    }
}

internal class KotlinModelBuilder : ParameterizedToolingModelBuilder<ModelRequest> {
    override fun canBuild(modelName: String): Boolean = modelName == KotlinGradleModel::class.java.name

    override fun getParameterType(): Class<ModelRequest> = ModelRequest::class.java

    override fun buildAll(modelName: String, project: Project): KotlinGradleModel =
        result(ErrorType.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Kotlin import model parameters are required")

    override fun buildAll(modelName: String, parameter: ModelRequest, project: Project): KotlinGradleModel = try {
        val provider = KotlinImportModelProvider(project)
        val parameters = parameter.kotlinModelParameters ?: byteArrayOf()
        when (parameter.kotlinModelId) {
            KotlinImportModelIds.BASE -> parameterlessModel(parameters) { provider.baseInformation() }
            KotlinImportModelIds.PROJECT_INFORMATION -> parameterlessModel(parameters) { provider.projectInformation() }
            KotlinImportModelIds.COMPILATION_UNIT -> compilationUnitModel(parameters, provider)
            else -> result(ErrorType.ERROR_TYPE_UNKNOWN_MODEL_ID, "Unknown Kotlin import model '${parameter.kotlinModelId}'")
        }
    } catch (failure: Exception) {
        result(ErrorType.ERROR_TYPE_INTERNAL_ERROR, "Failed to produce Kotlin import model: ${failure.message}")
    }

    private fun parameterlessModel(parameters: ByteArray, producer: () -> Message): KotlinGradleModel =
        if (parameters.isEmpty()) {
            model(producer())
        } else {
            result(ErrorType.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS, "Kotlin import model does not accept parameters")
        }

    private fun compilationUnitModel(parameters: ByteArray, provider: KotlinImportModelProvider): KotlinGradleModel {
        val parsedParameters = try {
            CompilationUnitModel.Parameters.parseFrom(parameters)
        } catch (_: InvalidProtocolBufferException) {
            return result(ErrorType.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Malformed compilation unit parameters")
        }
        if (!parsedParameters.hasCompilationUnitId() || parsedParameters.compilationUnitId.value.isEmpty()) {
            return result(ErrorType.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Compilation unit ID is required")
        }
        if (parsedParameters.compilationUnitId !in provider.projectInformation().compilationUnitIdsList) {
            return result(ErrorType.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS, "Unsupported compilation unit ID")
        }
        return model(provider.compilationUnit(parsedParameters.compilationUnitId))
    }

    private fun model(model: Message): KotlinGradleModel = KotlinGradleModelResult(
        Result.newBuilder().setModel(Any.pack(model)).build().toByteArray()
    )

    private fun result(type: ErrorType, message: String): KotlinGradleModel = KotlinGradleModelResult(
        Result.newBuilder()
            .setError(ImportModelError.newBuilder().setErrorType(type).setErrorMessage(message))
            .build()
            .toByteArray()
    )
}

private class KotlinGradleModelResult(
    override val kotlinModelResult: ByteArray,
) : KotlinGradleModel
