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

import groovy.json.StringEscapeUtils
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.launchProcessWithFallback
import org.jetbrains.kotlin.gradle.logging.GradleKotlinLogger
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipFile
import kotlin.concurrent.thread

internal fun loadCompilerVersion(compilerClasspath: List<File>): String {
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
            if (cpFile.isFile && cpFile.extension.toLowerCase() == "jar") {
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
    classpath: List<File>,
    logger: KotlinLogger,
    buildDir: File
): ExitCode {
    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val classpathString = classpath.map { it.absolutePath }.joinToString(separator = File.pathSeparator)

    val compilerOptions = writeArgumentsToFile(buildDir, argsArray)

    val builder = ProcessBuilder(javaBin, "-cp", classpathString, compilerClassName, "@${compilerOptions.absolutePath}")
    val messageCollector = createLoggingMessageCollector(logger)
    val process = launchProcessWithFallback(builder, DaemonReportingTargets(messageCollector = messageCollector))

    // important to read inputStream, otherwise the process may hang on some systems
    val readErrThread = thread {
        process.errorStream!!.bufferedReader().forEachLine {
            logger.error(it)
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
    logger.logFinish(OUT_OF_PROCESS_EXECUTION_STRATEGY)
    return exitCodeFromProcessExitCode(logger, exitCode)
}

private fun writeArgumentsToFile(directory: File, argsArray: Array<String>): File {
    val compilerOptions =
        Files.createTempFile(directory.toPath(), LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "_", ".compiler.options").toFile()
    compilerOptions.deleteOnExit()
    compilerOptions.writeText(argsArray.joinToString(" ") { "\"${StringEscapeUtils.escapeJava(it)}\"" })
    return compilerOptions
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
            CompilerMessageSeverity.INFO
            -> log.info(locMessage)
            CompilerMessageSeverity.LOGGING -> log.debug(locMessage)
            CompilerMessageSeverity.OUTPUT -> {
            }
        }
    }
}

internal fun KotlinLogger.logFinish(strategy: String) {
    debug("Finished executing kotlin compiler using $strategy strategy")
}

internal fun exitCodeFromProcessExitCode(log: KotlinLogger, code: Int): ExitCode {
    val exitCode = ExitCode.values().find { it.code == code }
    if (exitCode != null) return exitCode

    log.debug("Could not find exit code by value: $code")
    return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
}