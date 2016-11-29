package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.net.URL

internal class GradleCompilerEnvironment(
        val compilerJar: File,
        messageCollector: MessageCollector,
        outputItemsCollector: OutputItemsCollector
) : CompilerEnvironment(Services.EMPTY, messageCollector, outputItemsCollector) {
    val compilerClasspath: List<File>
        get() = listOf(compilerJar).filterNotNull()

    val compilerClasspathURLs: List<URL>
        get() = compilerClasspath.map { it.toURI().toURL() }
}