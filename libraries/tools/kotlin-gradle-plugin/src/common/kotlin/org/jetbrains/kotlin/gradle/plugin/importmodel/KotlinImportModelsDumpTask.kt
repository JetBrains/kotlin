/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.Message
import org.jetbrains.kotlin.importmodels.internal.protobuf.com.google.protobuf.util.JsonFormat
import java.io.File

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

    @TaskAction
    fun dumpModels() {
        val outputRoot = outputDirectory.get().asFile
        if (outputRoot.exists() && !outputRoot.deleteRecursively()) {
            throw GradleException("Failed to clear Kotlin import model output directory '$outputRoot'")
        }

        val base = produce("base") { modelProvider.baseInformation() }
        val projectModel = produce("projectInformation") { modelProvider.projectInformation() }
        val compilationUnits = projectModel.compilationUnitIdsList.map { id ->
            produce("compilationUnit[${id.value}]") { modelProvider.compilationUnit(id) }
        }

        write(label = "base", jsonFile = outputRoot.resolve("base.json"), model = base)
        write(label = "projectInformation", jsonFile = outputRoot.resolve("project.json"), model = projectModel)
        compilationUnits.forEach { model ->
            write(
                label = "compilationUnit[${model.parameters.compilationUnitId.value}]",
                jsonFile = outputRoot.resolve("compilation-units/${model.compilationName}.json"),
                model = model,
            )
        }
    }

    private fun write(
        label: String,
        jsonFile: File,
        model: Message,
    ) {
        try {
            jsonFile.parentFile.mkdirs()
            jsonFile.writeText(JsonFormat.printer().preservingProtoFieldNames().alwaysPrintFieldsWithNoPresence().print(model))
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
