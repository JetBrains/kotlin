/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.CompilationUnitModel
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import javax.inject.Inject

private const val DUMP_KOTLIN_IMPORT_MODELS_TASK_NAME = "dumpKotlinImportModels"

internal val KotlinImportModelsDumpTaskSetupAction = KotlinProjectSetupAction {
    locateOrRegisterKotlinImportModelsDumpTask()
}

internal fun Project.locateOrRegisterKotlinImportModelsDumpTask(): TaskProvider<KotlinImportModelsDumpTask> =
    locateOrRegisterTask(DUMP_KOTLIN_IMPORT_MODELS_TASK_NAME) { task ->
        task.group = "ide"
        task.description = "Dumps Kotlin import models as diagnostic ProtoJSON"
        task.notCompatibleWithConfigurationCache("Kotlin import model dump is a diagnostic POC")
    }

@DisableCachingByDefault(because = "Used for debugging and diagnostic purposes")
internal abstract class KotlinImportModelsDumpTask : DefaultTask() {
    private val outputDirectory = project.layout.buildDirectory.dir("kotlin-import-models")
    private val modelProvider = KotlinImportModelProvider(project)

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun dumpModels() {
        val outputRoot = outputDirectory.get().asFile.toPath()
        if (outputRoot.exists()) fileSystem.delete { it.delete(outputRoot) }

        val base = produce("base") { modelProvider.baseInformation() }
        val projectModel = produce("projectInformation") { modelProvider.projectInformation() }
        val compilationUnits = projectModel.compilationUnitIdsList.map { id ->
            produce("compilationUnit[${id.value}]") { modelProvider.compilationUnit(id) }
        }

        write("base", outputRoot.resolve("base.json"), KotlinImportModelSerialization.toJson(base))
        write("projectInformation", outputRoot.resolve("project.json"), KotlinImportModelSerialization.toJson(projectModel))
        compilationUnits.forEach { model ->
            write(
                "compilationUnit[${model.parameters.compilationUnitId.value}]",
                outputRoot.resolve("compilation-units/${model.name}.json"),
                KotlinImportModelSerialization.toJson(model),
            )
        }
    }

    private fun write(label: String, jsonFile: Path, json: String) {
        try {
            jsonFile.parent.createDirectories()
            jsonFile.writeText(json)
        } catch (failure: Exception) {
            throw GradleException("Failed to write Kotlin import model '$label'", failure)
        }
    }

    private inline fun <T> produce(label: String, action: () -> T): T = try {
        action()
    } catch (failure: Exception) {
        throw GradleException("Failed to produce Kotlin import model '$label'", failure)
    }
}
