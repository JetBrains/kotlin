/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.daemon

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled
import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.withIC
import java.io.File
import java.nio.file.Files
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles

internal fun parseArgs(
    args: Array<String>,
    composePluginPath: String? = null,
    compiler: K2JVMCompiler = K2JVMCompiler()
): K2JVMCompilerArguments {
    val compilerArgs = compiler.createArguments()
    compiler.parseArguments(args, compilerArgs)

    composePluginPath?.let {
        compilerArgs.pluginClasspaths = (compilerArgs.pluginClasspaths ?: emptyArray()) + it
        compilerArgs.pluginOptions =
            (compilerArgs.pluginOptions ?: emptyArray()) +
                "plugin:androidx.compose.plugins.idea:enabled=true"
        compilerArgs.useIR = true // Force IR since it's required for Compose
    }
    return compilerArgs
}

data class DaemonCompilerSettings(val composePluginPath: String? = null) {
    companion object {
        val DefaultSettings = DaemonCompilerSettings()
    }
}

interface DaemonCompiler {
    fun compile(
        args: Array<String>,
        daemonCompilerSettings: DaemonCompilerSettings = DaemonCompilerSettings.DefaultSettings
    ): ExitCode
}

/**
 * A [DaemonCompiler] that uses regular `kotlinc` invocations.
 */
object BasicDaemonCompiler : DaemonCompiler {
    private val compiler = K2JVMCompiler()

    override fun compile(
        args: Array<String>,
        daemonCompilerSettings: DaemonCompilerSettings
    ): ExitCode {
        return try {
            val compilerArgs = parseArgs(args, daemonCompilerSettings.composePluginPath)
            compiler.exec(
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, true),
                Services.EMPTY,
                compilerArgs
            )
        } catch (t: Throwable) {
            t.printStackTrace(System.err)
            ExitCode.INTERNAL_ERROR
        }
    }
}

/**
 * A [DaemonCompiler] that calls `kotlinc` in incremental mode.
 */
object IncrementalDaemonCompiler : DaemonCompiler {
    private val compiler = IncrementalJvmCompilerRunner(
        workingDir = Files.createTempDirectory("workingDir").toFile(),
        reporter = BuildReporter(DoNothingICReporter, DoNothingBuildMetricsReporter),
        usePreciseJavaTracking = true,
        outputFiles = emptyList(),
        buildHistoryFile = Files.createTempFile("build-history", ".bin").toFile(),
        modulesApiHistory = EmptyModulesApiHistory,
        kotlinSourceFilesExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
        classpathChanges = ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
            ClasspathSnapshotFiles(emptyList(), Files.createTempDirectory("snapshots").toFile())
        )
    )

    override fun compile(
        args: Array<String>,
        daemonCompilerSettings: DaemonCompilerSettings
    ): ExitCode {
        println("Incremental compiler invoke")
        return try {
            val compilerArgs = parseArgs(args, daemonCompilerSettings.composePluginPath)
            compilerArgs.moduleName = "test"
            withIC(compilerArgs) {
                compiler.compile(
                    compilerArgs.freeArgs.map { File(it) },
                    compilerArgs,
                    PrintingMessageCollector(
                        System.err,
                        MessageRenderer.PLAIN_RELATIVE_PATHS,
                        true
                    ),
                    null,
                    null
                )
            }
        } catch (t: Throwable) {
            t.printStackTrace(System.err)
            ExitCode.INTERNAL_ERROR
        }
    }
}