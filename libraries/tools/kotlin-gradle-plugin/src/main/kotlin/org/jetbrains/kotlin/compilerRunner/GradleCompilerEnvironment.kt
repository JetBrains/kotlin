package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.internal.artifacts.dsl.ArtifactFile
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.GradleICReporter
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistry
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import java.io.File
import java.net.URL

internal open class GradleCompilerEnvironment(
        val compilerJar: File,
        messageCollector: MessageCollector,
        outputItemsCollector: OutputItemsCollector
) : CompilerEnvironment(Services.EMPTY, messageCollector, outputItemsCollector) {
    val compilerClasspath: List<File>
        get() = listOf(compilerJar).filterNotNull()

    val compilerClasspathURLs: List<URL>
        get() = compilerClasspath.map { it.toURI().toURL() }
}

internal class GradleIncrementalCompilerEnvironment(
        compilerJar: File,
        val changedFiles: ChangedFiles,
        val reporter: GradleICReporter,
        val workingDir: File,
        messageCollector: MessageCollector,
        outputItemsCollector: OutputItemsCollector,
        val kaptAnnotationsFileUpdater: AnnotationFileUpdater?,
        val artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider?,
        val artifactFile: File?
) : GradleCompilerEnvironment(compilerJar, messageCollector, outputItemsCollector)