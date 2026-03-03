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
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.START_TASK_ACTION_EXECUTION
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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
import java.util.zip.ZipFile


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
    } catch (_: Throwable) {
    }

    return result ?: "<unknown>"
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
    metricsReporter: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>,
    languageVersion: KotlinVersion?,
    fn: () -> Any
) {
    metricsReporter.addTimeMetric(START_TASK_ACTION_EXECUTION)
    buildMetricsService.orNull?.also { it.addTask(path, this.javaClass, metricsReporter) }

    try {
        fn.invoke()
    } finally {
        val result = TaskExecutionResult(
            buildMetrics = BuildMetrics(),
            taskInfo = TaskExecutionInfo(kotlinLanguageVersion = languageVersion)
        )
        TaskExecutionResults[path] = result
    }

}
