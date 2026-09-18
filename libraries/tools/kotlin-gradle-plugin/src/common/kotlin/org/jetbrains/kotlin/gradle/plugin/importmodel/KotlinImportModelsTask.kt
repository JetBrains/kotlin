/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModelKt
import org.jetbrains.kotlin.importmodels.proto.CompilerArgumentsModelKt
import org.jetbrains.kotlin.importmodels.proto.DependenciesModel
import org.jetbrains.kotlin.importmodels.proto.DependenciesModelKt
import java.security.MessageDigest

internal const val GENERATE_KOTLIN_IMPORT_MODELS_TASK_NAME = "generateKotlinImportModels"

internal fun importModelsDirectory(project: Project): Provider<Directory> = project.layout.buildDirectory.dir("kotlin/import-models")

internal fun importModelFileName(modelId: String, parameters: ByteArray?): String {
    val digest = MessageDigest.getInstance("SHA-1").digest(parameters ?: byteArrayOf())
    return "$modelId-${digest.joinToString("") { "%02x".format(it) }}.pb"
}

internal val KotlinImportModelsTaskSetupAction = KotlinProjectSetupAction {
    val modelProvider = KotlinImportModelProvider(project)
    tasks.register(GENERATE_KOTLIN_IMPORT_MODELS_TASK_NAME, KotlinImportModelsTask::class.java) { task ->
        task.group = "ide"
        task.description = "Generates Kotlin import models as serialized protobuf files"
        task.models.set(provider { modelProvider.allModels() })
        task.outputDirectory.set(importModelsDirectory(project))
    }
}

// File name -> serialized `Result` of every model the project provides
private fun KotlinImportModelProvider.allModels(): Map<String, ByteArray> = buildMap {
    fun model(modelId: String, parameters: ByteArray?, result: ByteArray) {
        put(importModelFileName(modelId, parameters), result)
    }

    val projectInformation = projectInformation()
    model(KotlinImportModelIds.BASE, null, KotlinImportModelSerialization.modelResult(baseInformation()))
    model(KotlinImportModelIds.PROJECT_INFORMATION, null, KotlinImportModelSerialization.modelResult(projectInformation))
    projectInformation.compilationUnitIdsList.forEach { id ->
        model(
            KotlinImportModelIds.COMPILATION_UNIT,
            CompilationUnitModelKt.parameters { compilationUnitId = id }.toByteArray(),
            KotlinImportModelSerialization.modelResult(compilationUnit(id)),
        )
        model(
            KotlinImportModelIds.COMPILER_ARGUMENTS,
            CompilerArgumentsModelKt.parameters { compilationUnitId = id }.toByteArray(),
            KotlinImportModelSerialization.modelResult(compilerArguments(id)),
        )
        val dependenciesParameters = DependenciesModelKt.parameters {
            compilationUnitId = id
            scope = DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE
            coverage = DependenciesModel.Coverage.DEPENDENCY_COVERAGE_ALL
        }
        model(
            KotlinImportModelIds.DEPENDENCIES,
            dependenciesParameters.toByteArray(),
            KotlinImportModelSerialization.modelResult(dependencies(dependenciesParameters)),
        )
    }
}

/**
 * POC: Kotlin import models are produced by a regular Gradle task instead of inside [KotlinModelBuilder].
 *
 * The task input is the map of all serialized `Result` models of the project, computed lazily by [KotlinImportModelProvider].
 * Under the configuration cache the input is computed once when the cache entry is stored and restored on subsequent
 * invocations, so the (expensive) model computation is skipped entirely on a configuration cache hit and the task is
 * `UP-TO-DATE`. [KotlinModelBuilder] only hands out the written files.
 */
@DisableCachingByDefault(because = "Outputs contain absolute paths; the POC relies on up-to-date checks and the configuration cache")
internal abstract class KotlinImportModelsTask : DefaultTask() {
    // File name -> serialized `Result`, see [importModelFileName]
    @get:Input
    abstract val models: MapProperty<String, ByteArray>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val directory = outputDirectory.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()
        models.get().forEach { (fileName, bytes) -> directory.resolve(fileName).writeBytes(bytes) }
    }
}
