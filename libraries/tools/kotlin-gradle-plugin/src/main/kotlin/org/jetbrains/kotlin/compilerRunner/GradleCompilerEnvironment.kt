package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.gradle.tasks.GradleMessageCollector
import org.jetbrains.kotlin.gradle.tasks.findToolsJar
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ICReporter
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProvider
import java.io.File
import java.net.URL

internal open class GradleCompilerEnvironment(
        val compilerJar: File,
        messageCollector: GradleMessageCollector,
        outputItemsCollector: OutputItemsCollector,
        val compilerArgs: CommonCompilerArguments
) : CompilerEnvironment(Services.EMPTY, messageCollector, outputItemsCollector) {
    val toolsJar: File? by lazy { findToolsJar() }

    val compilerClasspath: List<File>
            get() = listOf(compilerJar, toolsJar).filterNotNull()

    val compilerClasspathURLs: List<URL>
        get() = compilerClasspath.map { it.toURI().toURL() }
}

internal class GradleIncrementalCompilerEnvironment(
        compilerJar: File,
        val changedFiles: ChangedFiles,
        val reporter: ICReporter,
        val workingDir: File,
        messageCollector: GradleMessageCollector,
        outputItemsCollector: OutputItemsCollector,
        compilerArgs: CommonCompilerArguments,
        val kaptAnnotationsFileUpdater: AnnotationFileUpdater? = null,
        val artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider? = null,
        val artifactFile: File? = null
) : GradleCompilerEnvironment(compilerJar, messageCollector, outputItemsCollector, compilerArgs)