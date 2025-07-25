/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.internal

import androidx.compose.compiler.mapping.ComposeMapping
import androidx.compose.compiler.mapping.ErrorReporter
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.internal.extensions.core.get
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import java.util.Locale.getDefault
import javax.inject.Inject

internal fun Project.configureComposeMappingFile() {
    plugins.withId("com.android.application") {
        project.extensions.findByType<ApplicationAndroidComponentsExtension>()?.onVariants { variant ->
            if (!variant.isMinifyEnabled) return@onVariants

            val produceTaskName = "produce${variant.name.capitalize()}ComposeMapping"
            val taskProvider = project.tasks.register<ProduceMappingFileTask>(produceTaskName) {
                output.set(project.layout.buildDirectory.file("intermediates/compose_mapping/${variant.name}/compose-mapping.txt"))
            }

            variant.artifacts
                .forScope(ScopedArtifacts.Scope.ALL)
                .use(taskProvider)
                .toGet(
                    ScopedArtifact.CLASSES,
                    ProduceMappingFileTask::projectJars,
                    ProduceMappingFileTask::projectDirectories
                )

            val mergeTaskName = "merge${variant.name.capitalize()}ComposeMapping"
            val mergeTaskProvider = project.tasks.register<MergeMappingFileTask>(mergeTaskName) {
                composeMapping.set(taskProvider.map { it.output.get() })
            }

            variant.artifacts
                .use(mergeTaskProvider)
                .wiredWithFiles(MergeMappingFileTask::originalFile, MergeMappingFileTask::output)
                .toTransform(uncheckedCast(SingleArtifact.OBFUSCATION_MAPPING_FILE))
        }
    }
}


@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun <T> uncheckedCast(value: Any): T = value as T

private fun String.capitalize(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

@CacheableTask
internal abstract class MergeMappingFileTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val originalFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeMapping: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        // todo: fix previous mapping file hash
        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.bufferedWriter().use { writer ->
            originalFile.orNull?.let { writer.write(it.asFile.readText()) }
            composeMapping.orNull?.let { writer.write(it.asFile.readText()) }
        }
    }
}

@CacheableTask
internal abstract class ProduceMappingFileTask @Inject constructor(
    private val problems: Problems
) : DefaultTask() {
    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectDirectories: ListProperty<Directory>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectJars: ListProperty<RegularFile>

    private val files by lazy {
        services.get<FileOperations>()
    }

    @TaskAction
    fun taskAction() {
        val reporter = object : ErrorReporter {
            override fun reportError(e: Throwable) {
                problems.reporter.report(MappingGenerationFailedProblemId) { spec ->
                    spec.withException(e)
                        .severity(Severity.WARNING)
                }
            }
        }

        val mappings = buildList {
            projectJars.get().forEach { jar ->
                val contents = files.zipTree(jar)
                contents.forEach { file ->
                    if (file.name.endsWith(".class")) {
                        val mapping = ComposeMapping.fromBytecode(reporter, file.readBytes())
                        add(mapping)
                    }
                }
            }

            projectDirectories.get().forEach {
                val contents = files.fileTree(it)
                contents.forEach { file ->
                    if (file.name.endsWith(".class")) {
                        val mapping = ComposeMapping.fromBytecode(reporter, file.readBytes())
                        add(mapping)
                    }
                }
            }
        }

        output.get().asFile.bufferedWriter().use { writer ->
            writer.write("ComposeStackTrace -> ${"$$"}compose:\n")
            mappings.forEach {
                writer.write(it.asProguardMapping())
            }
        }
    }
}

private val Group = ProblemGroup.create("compose-mapping", "Compose Mapping Generator Group")

private val MappingGenerationFailedProblemId = ProblemId.create(
    "compose-mapping-fail",
    "Failed to generate Compose mapping entry.",
    Group
)
