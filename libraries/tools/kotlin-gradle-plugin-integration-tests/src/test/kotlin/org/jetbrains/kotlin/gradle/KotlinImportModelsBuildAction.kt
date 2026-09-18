/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.model.Model
import org.gradle.tooling.model.gradle.GradleBuild
import org.jetbrains.kotlin.importmodels.KotlinGradleModel
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.ModelRequest
import org.jetbrains.kotlin.importmodels.proto.*
import java.io.Serializable

/** The KGP task that produces the import models the build actions below read. */
internal const val GENERATE_TASK = "generateKotlinImportModels"

/**
 * Requests every Kotlin import model of the root project the way an IDE sync would.
 *
 * Public with a no-arg constructor on purpose: besides the integration tests, it is used as a `gradle-profiler`
 * `tooling-api { action = ... }` entry point, see `src/test/resources/import-models-profiler/README.md`.
 */
class KotlinImportModelsBuildAction : BuildAction<KotlinImportModelsBuildActionResult> {
    override fun execute(controller: BuildController): KotlinImportModelsBuildActionResult =
        controller.requestKotlinImportModels(target = null)
            ?: error("Kotlin import models are not available for the root project")
}

/**
 * Requests every Kotlin import model of every project of the build (projects without the Kotlin plugin are skipped),
 * keyed by project path. Used to benchmark multi-project syncs with `gradle-profiler`.
 *
 * The requests are issued sequentially, project by project and model by model, which mirrors how the IntelliJ IDEA
 * counterpart (`KotlinImportModelsProvider`) fetches them: every model request depends on the previous response
 * (`BASE` -> `PROJECT_INFORMATION` -> per-compilation-unit models), so they cannot be batched into parallel requests.
 */
class KotlinImportModelsAllProjectsBuildAction : BuildAction<KotlinImportModelsAllProjectsBuildActionResult> {
    override fun execute(controller: BuildController): KotlinImportModelsAllProjectsBuildActionResult {
        val projects = controller.getModel(GradleBuild::class.java).projects
        val models = projects.mapNotNull { project ->
            controller.requestKotlinImportModels(project)?.let { project.path to it }
        }.toMap()
        return KotlinImportModelsAllProjectsBuildActionResult(models).also { it.validate() }
    }
}

data class KotlinImportModelsAllProjectsBuildActionResult(
    val projects: Map<String, KotlinImportModelsBuildActionResult>,
) : Serializable

/**
 * Fails if no project provides Kotlin import models or if any of the models is an [Error], so that a benchmark
 * cannot accidentally measure a sync that returned errors instead of models. Prints a one-line summary to the build
 * output for the same reason.
 */
private fun KotlinImportModelsAllProjectsBuildActionResult.validate() {
    check(projects.isNotEmpty()) { "No project of the build provides Kotlin import models" }
    var modelCount = 0
    var compilationUnitCount = 0
    for (entry in projects) {
        val projectPath = entry.key
        val result = entry.value
        val all = buildList {
            addAll(listOf(result.base, result.project))
            addAll(result.compilationUnits)
            addAll(result.compilerArguments)
            addAll(result.dependencies)
        }
        for (bytes in all) {
            val parsed = Result.parseFrom(bytes)
            check(parsed.hasModel()) { "Kotlin import model of '$projectPath' is an error: ${parsed.error.errorMessage}" }
        }
        modelCount += all.size
        compilationUnitCount += result.compilationUnits.size
    }
    println("Kotlin import models: ${projects.size} projects, $compilationUnitCount compilation units, $modelCount models")
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
