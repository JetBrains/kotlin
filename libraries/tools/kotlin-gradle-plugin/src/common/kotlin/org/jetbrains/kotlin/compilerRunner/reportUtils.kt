/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.launchProcessWithFallback
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.logging.GradleErrorMessageCollector
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.report.TaskExecutionInfo
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.report.UsesBuildMetricsService
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipFile
import kotlin.concurrent.thread


internal fun loadCompilerVersion(compilerClasspath: Iterable<File>): String {
    var result: String? = null

    fun checkVersion(bytes: ByteArray) {
        ClassReader(bytes).accept(
            object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
                    if (name == KotlinCompilerVersion::VERSION.name && value is String) {
                        result = value
                    }
                    return super.visitField(access, name, desc, signature, value)
                }
            },
            ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG
        )
    }

    try {
        val versionClassFileName = KotlinCompilerVersion::class.java.name.replace('.', '/') + ".class"
        for (cpFile in compilerClasspath) {
            if (cpFile.isFile && cpFile.extension.toLowerCaseAsciiOnly() == "jar") {
                ZipFile(cpFile).use { jar ->
                    val versionFileEntry = jar.getEntry(KotlinCompilerVersion.VERSION_FILE_PATH)
                    if (versionFileEntry != null) {
                        result = jar.getInputStream(versionFileEntry).bufferedReader().use { it.readText() }
                    } else {
                        val bytes = jar.getInputStream(jar.getEntry(versionClassFileName)).use { it.readBytes() }
                        checkVersion(bytes)
                    }
                }
            } else if (cpFile.isDirectory) {
                val versionFile = File(cpFile, KotlinCompilerVersion.VERSION_FILE_PATH)
                if (versionFile.isFile) {
                    result = versionFile.readText()
                } else {
                    File(cpFile, versionClassFileName).takeIf { it.isFile }?.let {
                        checkVersion(it.readBytes())
                    }
                }
            }
            if (result != null) break
        }
    } catch (e: Throwable) {
    }

    return result ?: "<unknown>"
}

internal fun runToolInSeparateProcess(
    argsArray: Array<String>,
    compilerClassName: String,
    classpath: Iterable<File>,
    logger: KotlinLogger,
    buildDir: File,
    jvmArgs: List<String> = emptyList(),
): ExitCode {
    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val classpathString = classpath.joinToString(separator = File.pathSeparator) { it.absolutePath }

    val compilerOptions = writeArgumentsToFile(buildDir, argsArray)

    val builder = ProcessBuilder(
        javaBin,
        *(jvmArgs.toTypedArray()),
        "-cp",
        classpathString,
        compilerClassName,
        "@${compilerOptions.absolutePath}"
    )
    val messageCollector = GradleErrorMessageCollector(logger, createLoggingMessageCollector(logger))
    val process = launchProcessWithFallback(builder, DaemonReportingTargets(messageCollector = messageCollector))

    // important to read inputStream, otherwise the process may hang on some systems
    val readErrThread = thread {
        process.errorStream!!.bufferedReader().forEachLine {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, it)
        }
    }

    if (logger is GradleKotlinLogger) {
        process.inputStream!!.bufferedReader().forEachLine {
            logger.lifecycle(it)
        }
    } else {
        process.inputStream!!.bufferedReader().forEachLine {
            println(it)
        }
    }

    readErrThread.join()

    val exitCode = process.waitFor()
    logger.logFinish(KotlinCompilerExecutionStrategy.OUT_OF_PROCESS)
    return exitCodeFromProcessExitCode(logger, exitCode)
}

private fun writeArgumentsToFile(directory: File, argsArray: Array<String>): File {
    val prefix = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_"
    val suffix = ".compiler.options"
    val compilerOptions = if (directory.exists())
        Files.createTempFile(directory.toPath(), prefix, suffix).toFile()
    else
        Files.createTempFile(prefix, suffix).toFile()
    compilerOptions.writeText(
        argsArray.joinToString(" ") {
            "\"${it.escapeJavaStyleString()}\""
        }
    )
    return compilerOptions
}

// Ported method from Groovy:
// https://github.com/apache/groovy/blob/73c0f12ab35427bc3e7fd76929b482df61e1b80d/subprojects/groovy-json/src/main/java/groovy/json/StringEscapeUtils.java#L175
// Note: using '/f' char produces a compilation error, so removed it
internal fun String.escapeJavaStyleString(
    escapeSingleQuote: Boolean = false,
    escapeForwardSlash: Boolean = false,
): String {
    return buildString {
        this@escapeJavaStyleString.forEach { ch ->
            when {
                ch.toInt() > 0xfff -> append("\\u${ch.hex()}")
                ch.toInt() > 0xff -> append("\\u0${ch.hex()}")
                ch.toInt() >= 0x7f -> append("\\u00${ch.hex()}")
                ch < 32.toChar() -> when (ch) {
                    '\b' -> append('\\').append('b')
                    '\n' -> append('\\').append('n')
                    '\t' -> append('\\').append('t')
                    '\r' -> append('\\').append('r')
                    else -> if (ch > 0xf.toChar()) {
                        append("\\u00${ch.hex()}")
                    } else {
                        append("\\u000${ch.hex()}")
                    }
                }
                else -> when (ch) {
                    '\'' -> {
                        if (escapeSingleQuote) append('\\')
                        append('\'')
                    }
                    '"' -> append('\\').append('"')
                    '\\' -> append('\\').append('\\')
                    '/' -> {
                        if (escapeForwardSlash) append('\\')
                        append('/')
                    }
                    else -> append(ch)
                }
            }
        }
    }
}

private fun Char.hex(): String {
    return Integer.toHexString(toInt()).toUpperCase(Locale.ENGLISH)
}

private fun createLoggingMessageCollector(log: KotlinLogger): MessageCollector = object : MessageCollector {
    private var hasErrors = false
    private val messageRenderer = MessageRenderer.PLAIN_FULL_PATHS

    override fun clear() {
        hasErrors = false
    }

    override fun hasErrors(): Boolean = hasErrors

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val locMessage = messageRenderer.render(severity, message, location)
        when (severity) {
            CompilerMessageSeverity.EXCEPTION -> log.error(locMessage)
            CompilerMessageSeverity.ERROR,
            CompilerMessageSeverity.STRONG_WARNING,
            CompilerMessageSeverity.WARNING,
            CompilerMessageSeverity.INFO,
            -> log.info(locMessage)
            CompilerMessageSeverity.LOGGING -> log.debug(locMessage)
            CompilerMessageSeverity.OUTPUT -> {
            }
        }
    }
}

internal val KotlinCompilerExecutionStrategy.asFinishLogMessage: String
    get() = "Finished executing kotlin compiler using $this strategy"

internal fun KotlinLogger.logFinish(strategy: KotlinCompilerExecutionStrategy) {
    info(strategy.asFinishLogMessage)
}

internal fun exitCodeFromProcessExitCode(log: KotlinLogger, code: Int): ExitCode {
    val exitCode = ExitCode.values().find { it.code == code }
    if (exitCode != null) return exitCode

    log.debug("Could not find exit code by value: $code")
    return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
}

internal fun UsesBuildMetricsService.addBuildMetricsForTaskAction(
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    languageVersion: KotlinVersion?,
    fn: () -> Any
) {
    metricsReporter.addTimeMetric(GradleBuildPerformanceMetric.START_TASK_ACTION_EXECUTION)
    buildMetricsService.orNull?.also { it.addTask(path, this.javaClass, metricsReporter) }

    try {
        fn.invoke()
    } finally {
        val result = TaskExecutionResult(buildMetrics = BuildMetrics(), taskInfo = TaskExecutionInfo(kotlinLanguageVersion = languageVersion))
        TaskExecutionResults[path] = result
    }

}