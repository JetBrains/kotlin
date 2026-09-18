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
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
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

/**
 * POC: hands out the `Result` files written by the [GENERATE_KOTLIN_IMPORT_MODELS_TASK_NAME] task without computing anything.
 * The task is expected to be executed in the same invocation (`BuildActionExecuter.forTasks`) or earlier.
 */
internal class KotlinModelBuilder : ParameterizedToolingModelBuilder<ModelRequest> {
    override fun canBuild(modelName: String): Boolean = modelName == KotlinGradleModel::class.java.name

    override fun getParameterType(): Class<ModelRequest> = ModelRequest::class.java

    override fun buildAll(modelName: String, project: Project): KotlinGradleModel = KotlinGradleModelResult(
        KotlinImportModelSerialization.errorResult(Error.Type.ERROR_TYPE_UNKNOWN_MODEL_PARAMS, "Kotlin import model parameters are required")
    )

    override fun buildAll(modelName: String, parameter: ModelRequest, project: Project): KotlinGradleModel {
        val fileName = importModelFileName(parameter.kotlinModelId.orEmpty(), parameter.kotlinModelParameters)
        val file = importModelsDirectory(project).get().file(fileName).asFile
        return KotlinGradleModelResult(
            if (file.isFile) file.readBytes()
            else KotlinImportModelSerialization.errorResult(
                Error.Type.ERROR_TYPE_GENERIC_ERROR,
                "Kotlin import model '${parameter.kotlinModelId}' was not generated; run the '$GENERATE_KOTLIN_IMPORT_MODELS_TASK_NAME' task first",
            )
        )
    }
}

private class KotlinGradleModelResult(
    override val kotlinModelResult: ByteArray,
) : KotlinGradleModel
