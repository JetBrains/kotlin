/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle.internal

import androidx.compose.compiler.mapping.ComposeMapping
import androidx.compose.compiler.mapping.ErrorReporter
import com.android.build.api.artifact.Artifact
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.security.MessageDigest
import java.util.Locale.getDefault
import java.util.zip.ZipFile
import javax.inject.Inject


internal fun Project.configureComposeMappingFile(
    enabled: Property<Boolean>
) {
    plugins.withId("com.android.application") {
        val configuration = configurations.maybeCreate("composeMappingProducerClasspath")
            .also {
                it.isCanBeConsumed = false
            }
            .defaultDependencies {
                it.add(
                    dependencies.create("org.jetbrains.kotlin:compose-group-mapping:${getKotlinPluginVersion()}")
                )
            }

        extensions.findByType<ApplicationAndroidComponentsExtension>()?.onVariants { variant ->
            if (!enabled.get()) return@onVariants
            if (!variant.isMinifyEnabled) return@onVariants

            val name = variant.name.capitalize()
            val produceTaskName = "produce${name}ComposeMapping"
            val produceTask = tasks.register<ProduceMappingFileTask>(produceTaskName) {
                classpath.from(configuration)
                val outputDir = layout.buildDirectory.dir("intermediates/compose_mapping/${variant.name}/")
                outputFile.set(outputDir.map { it.file("compose-mapping.txt") })
                errorFile.set(outputDir.map { it.file("compose-mapping-errors.txt") })
            }

            val reportErrorsTask =
                tasks.register<ReportMappingErrorsTask>("report${name}ComposeMappingErrors") {
                    errorFile.set(produceTask.map { it.errorFile.get() })
                }
            produceTask.configure { it.finalizedBy(reportErrorsTask) }

            variant.artifacts
                .forScope(ScopedArtifacts.Scope.ALL)
                .use(produceTask)
                .toGet(
                    ScopedArtifact.CLASSES,
                    ProduceMappingFileTask::projectJars,
                    ProduceMappingFileTask::projectDirectories
                )

            val mergeTaskProvider = tasks.register<MergeMappingFileTask>("merge${name}ComposeMapping") {
                composeMapping.set(produceTask.map { it.outputFile.get() })
            }

            variant.artifacts
                .use(mergeTaskProvider)
                .wiredWithFiles(MergeMappingFileTask::originalFile, MergeMappingFileTask::output)
                .toTransform(uncheckedCast(SingleArtifact.OBFUSCATION_MAPPING_FILE))
        }
    }
}

// This is a workaround to use `SingleArtifact.OBFUSCATION_MAPPING_FILE` as Transformable.
// The limitation only exists in AGP API, internally it allows to transform the file.
// Suggested and approved by cmw@google.com :)
// TODO(b/425875222): Update to use a proper AGP API
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun <T, R> uncheckedCast(value: Any): T where T : Artifact.Transformable, T : Artifact.Single<R> =
    value as T

private fun String.capitalize(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

@CacheableTask
internal abstract class ProduceMappingFileTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val errorFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectDirectories: ListProperty<Directory>

    @get:Classpath
    abstract val projectJars: ListProperty<RegularFile>

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun taskAction() {
        outputFile.get().asFile.parentFile.mkdirs()
        errorFile.get().asFile.parentFile.mkdirs()

        val workQueue = workerExecutor.classLoaderIsolation {
            it.classpath.from(classpath)
        }

        workQueue.submit(Action::class.java) {
            it.projectFiles.from(projectDirectories.get())
            it.projectJars.from(projectJars.get())
            it.output.set(outputFile)
            it.errorOutput.set(errorFile)
        }
    }

    abstract class Action : WorkAction<Action.Parameters> {
        interface Parameters : WorkParameters {
            val projectJars: ConfigurableFileCollection
            val projectFiles: ConfigurableFileCollection
            val output: RegularFileProperty
            val errorOutput: RegularFileProperty
        }

        override fun execute() {
            val errors = mutableListOf<String>()
            val mapping = ComposeMapping(object : ErrorReporter {
                override fun reportError(e: Throwable) {
                    errors.add(e.message.orEmpty())
                }
            })

            parameters.projectJars.forEach { jarFile ->
                ZipFile(jarFile).use { zipFile ->
                    for (entry in zipFile.entries()) {
                        if (entry.name.endsWith(".class")) {
                            val bytes = zipFile.getInputStream(entry).use {
                                it.readBytes()
                            }
                            mapping.append(bytes)
                        }
                    }
                }
            }


            parameters.projectFiles.forEach { file ->
                if (file.name.endsWith(".class")) {
                    val bytes = file.readBytes()
                    mapping.append(bytes)
                }
            }

            parameters.output.get().asFile.apply {
                bufferedWriter().use { writer ->
                    mapping.writeProguardMapping(writer)
                }
            }

            parameters.errorOutput.get().asFile.apply {
                bufferedWriter().use { writer ->
                    errors.forEach {
                        writer.appendLine(it)
                    }
                }
            }
        }
    }
}

@DisableCachingByDefault(because = "Logs warnings reported by worker from the previous task")
internal abstract class ReportMappingErrorsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val errorFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val file = errorFile.get().asFile
        if (!file.exists()) return

        file.useLines {
            it.forEach { line ->
                if (line.isEmpty()) return@forEach
                logger.warn(
                    "warning: Failed to collect Compose mapping ($line). Please report to Google through " +
                            "https://goo.gle/compose-feedback"
                )
            }
        }
    }
}


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
        /*
         * Read the proguard file generated by R8, calculate the new SHA-256 hash for the header, and append Compose mapping.
         */
        val newHeader = buildHeader()

        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()

        outputFile.bufferedWriter().use { writer ->
            writer.append(newHeader)

            var skippingHeader = true
            val originalFile = originalFile.get().asFile
            originalFile.forEachLine {
                if (skippingHeader && it.startsWith("#")) {
                    return@forEachLine
                }
                skippingHeader = false

                writer.appendLine(it)
            }

            val composeMapping = composeMapping.get().asFile
            composeMapping.forEachLine {
                writer.appendLine(it)
            }
        }
    }

    private fun buildHeader(): String {
        val mapping = originalFile.get().asFile

        val digest = MessageDigest.getInstance("SHA-256")

        fun MessageDigest.update(v: String) {
            update(v.toByteArray())
        }

        val headerString = StringBuilder()

        mapping.useLines { lines ->
            var readingHeader = true
            for (line in lines) {
                if (readingHeader) {
                    if (!line.startsWith('#')) {
                        readingHeader = false
                    } else {
                        if (line.startsWith("# pg_map_hash: SHA-256 ")) {
                            headerString.append("# pg_map_hash: SHA-256 ")
                            headerString.append("<SHA-256>")
                            headerString.appendLine()
                        } else {
                            headerString.appendLine(line)
                        }
                        continue
                    }
                }

                digest.update(line)
                digest.update("\n")
            }
        }

        val composeMapping = composeMapping.get().asFile
        composeMapping.forEachLine {
            digest.update(it)
            digest.update("\n")
        }

        val sha256String = digest.digest().joinToString("") {
            "%02x".format(it)
        }
        return headerString.replace("<SHA-256>".toRegex(), sha256String)
    }
}