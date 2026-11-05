/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.importmodels.internal.KotlinImportModelSerialization
import org.jetbrains.kotlin.importmodels.proto.Result
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

private const val DUMP_KOTLIN_IMPORT_MODELS_TASK_NAME = "dumpKotlinImportModels"

// The dump is a diagnostic, so the task exists only when the internal property asks for it
internal val KotlinImportModelsDumpTaskSetupAction = KotlinProjectSetupAction {
    if (kotlinPropertiesProvider.dumpKotlinImportModels) {
        locateOrRegisterTask<KotlinImportModelsDumpTask>(DUMP_KOTLIN_IMPORT_MODELS_TASK_NAME) { task ->
            task.group = "ide"
            task.description = "Dumps the served Kotlin import models as ProtoJSON and serialized Result bytes"
            // Reading live project state makes the task configuration-cache-incompatible
            task.notCompatibleWithConfigurationCache("Kotlin import model dump is a diagnostic reading live project state")
        }
    }
}

@DisableCachingByDefault(because = "The dump is a diagnostic of the current configuration and is never reused")
internal abstract class KotlinImportModelsDumpTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    private val outputDirectory = project.layout.buildDirectory.dir("kotlin-import-models")
    private val producer = KotlinImportModelProducer(project)

    @TaskAction
    fun dumpModels() {
        val outputRoot = outputDirectory.get().asFile.toPath()
        if (outputRoot.exists()) fileSystem.delete { it.delete(outputRoot) }

        producer.enumerateRequests().forEach { request ->
            val resultBytes = producer.serveRequest(request.modelId, request.parameters)
            val resultJson = KotlinImportModelSerialization.toJson(Result.parseFrom(resultBytes))
            write(outputRoot.resolve("${request.modelId}.json")) { it.writeText(resultJson) }
            write(outputRoot.resolve("${request.modelId}.bin")) { it.writeBytes(resultBytes) }
        }
    }

    private fun write(file: Path, writeBody: (Path) -> Unit) = try {
        file.parent.createDirectories()
        writeBody(file)
    } catch (e: Exception) {
        throw GradleException("Failed to write the Kotlin import model dump '${file.fileName}'", e)
    }
}
