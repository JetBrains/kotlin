/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.Model
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.*
import java.io.Serializable

/** The KGP task that produces the import models the build actions below read. */
internal const val GENERATE_TASK = "generateKotlinImportModels"

/**
 * Requests every Kotlin import model of the root project the way an IDE sync would.
 */
class KotlinImportModelsBuildAction : BuildAction<KotlinImportModelsBuildActionResult> {
    override fun execute(controller: BuildController): KotlinImportModelsBuildActionResult =
        controller.requestKotlinImportModels(target = null)
            ?: error("Kotlin import models are not available for the root project")
}

private fun BuildController.requestKotlinImportModels(target: Model?): KotlinImportModelsBuildActionResult? {
    fun request(modelId: String, parameters: ByteArray? = null): ByteArray? {
        val initializer: (ModelRequest) -> Unit = { request ->
            request.kotlinModelId = modelId
            request.kotlinModelParameters = parameters
        }
        val model = if (target == null) {
            getModel(KotlinGradleModel::class.java, ModelRequest::class.java, initializer)
        } else {
            findModel(target, KotlinGradleModel::class.java, ModelRequest::class.java, initializer)
        }
        return model?.kotlinModelResult
    }

    val base = request(KotlinImportModelIds.BASE) ?: return null
    val projectInformation = request(KotlinImportModelIds.PROJECT_INFORMATION) ?: return null
    val projectResult = Result.parseFrom(projectInformation)
    if (!projectResult.hasModel()) {
        return KotlinImportModelsBuildActionResult(base, projectInformation, emptyList(), emptyList(), emptyList())
    }
    val project = projectResult.model.unpack(ProjectModel::class.java)
    val compilationUnits = project.compilationUnitIdsList.map { compilationUnitId ->
        request(
            KotlinImportModelIds.COMPILATION_UNIT,
            CompilationUnitModelKt.parameters { this.compilationUnitId = compilationUnitId }.toByteArray(),
        )!!
    }
    val compilerArguments = project.compilationUnitIdsList.map { compilationUnitId ->
        request(
            KotlinImportModelIds.COMPILER_ARGUMENTS,
            CompilerArgumentsModelKt.parameters { this.compilationUnitId = compilationUnitId }.toByteArray(),
        )!!
    }
    val dependencies = project.compilationUnitIdsList.map { compilationUnitId ->
        request(
            KotlinImportModelIds.DEPENDENCIES,
            DependenciesModelKt.parameters {
                this.compilationUnitId = compilationUnitId
                scope = DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE
                coverage = DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL
            }.toByteArray(),
        )!!
    }
    return KotlinImportModelsBuildActionResult(base, projectInformation, compilationUnits, compilerArguments, dependencies)
}

data class KotlinImportModelsBuildActionResult(
    val base: ByteArray,
    val project: ByteArray,
    val compilationUnits: List<ByteArray>,
    val compilerArguments: List<ByteArray>,
    val dependencies: List<ByteArray>,
) : Serializable

internal data class KotlinImportModelsModels(
    val base: Result,
    val project: Result,
    val compilationUnits: List<Result>,
    val compilerArguments: List<Result>,
    val dependencies: List<Result>,
)

internal fun KotlinImportModelsBuildActionResult.toModels(): KotlinImportModelsModels = KotlinImportModelsModels(
    base = Result.parseFrom(base),
    project = Result.parseFrom(project),
    compilationUnits = compilationUnits.map(Result::parseFrom),
    compilerArguments = compilerArguments.map(Result::parseFrom),
    dependencies = dependencies.map(Result::parseFrom),
)
