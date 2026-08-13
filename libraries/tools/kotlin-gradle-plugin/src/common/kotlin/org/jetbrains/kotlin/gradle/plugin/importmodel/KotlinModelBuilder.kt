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
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitId
import org.jetbrains.kotlin.importmodels.proto.Error
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
        result(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Kotlin import model parameters are required")

    override fun buildAll(modelName: String, parameter: ModelRequest, project: Project): KotlinGradleModel = try {
        val provider = KotlinImportModelProvider(project)
        when (parameter.kotlinModelId) {
            KotlinImportModelIds.BASE -> parameterlessModel(parameter.kotlinModelParameters) {
                KotlinImportModelSerialization.modelResult(provider.baseInformation())
            }
            KotlinImportModelIds.PROJECT_INFORMATION -> parameterlessModel(parameter.kotlinModelParameters) {
                KotlinImportModelSerialization.modelResult(provider.projectInformation())
            }
            KotlinImportModelIds.COMPILATION_UNIT -> compilationUnitModel(parameter.kotlinModelParameters ?: byteArrayOf(), provider)
            KotlinImportModelIds.COMPILER_ARGUMENTS -> compilerArgumentsModel(parameter.kotlinModelParameters ?: byteArrayOf(), provider)
            else -> result(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_ID, "Unknown Kotlin import model '${parameter.kotlinModelId}'")
        }
    } catch (failure: Exception) {
        result(Error.Type.ERROR_TYPE_INTERNAL_ERROR, "Failed to produce Kotlin import model: ${failure.message}")
    }

    private fun parameterlessModel(parameters: ByteArray?, producer: () -> ByteArray): KotlinGradleModel =
        if (parameters == null) KotlinGradleModelResult(producer())
        else result(Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS, "Kotlin import model does not accept parameters")

    private fun compilationUnitModel(parameters: ByteArray, provider: KotlinImportModelProvider): KotlinGradleModel =
        compilationScopedModel(parameters, provider, KotlinImportModelSerialization::parseCompilationUnitId) { compilationUnitId ->
            KotlinImportModelSerialization.modelResult(provider.compilationUnit(compilationUnitId))
        }

    private fun compilerArgumentsModel(parameters: ByteArray, provider: KotlinImportModelProvider): KotlinGradleModel =
        compilationScopedModel(parameters, provider, KotlinImportModelSerialization::parseCompilerArgumentsCompilationUnitId) { compilationUnitId ->
            KotlinImportModelSerialization.modelResult(provider.compilerArguments(compilationUnitId))
        }

    private fun compilationScopedModel(
        parameters: ByteArray,
        provider: KotlinImportModelProvider,
        parseCompilationUnitId: (ByteArray) -> CompilationUnitId?,
        produce: (CompilationUnitId) -> ByteArray,
    ): KotlinGradleModel {
        val compilationUnitId = parseCompilationUnitId(parameters)
            ?: return result(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Compilation unit ID is required")
        if (compilationUnitId !in provider.projectInformation().compilationUnitIdsList) {
            return result(Error.Type.ERROR_TYPE_UNSUPPORTED_MODEL_PARAMS, "Unsupported compilation unit ID")
        }
        return KotlinGradleModelResult(produce(compilationUnitId))
    }

    private fun result(type: Error.Type, message: String): KotlinGradleModel = KotlinGradleModelResult(
        KotlinImportModelSerialization.errorResult(type, message)
    )
}

private class KotlinGradleModelResult(
    override val kotlinModelResult: ByteArray,
) : KotlinGradleModel
