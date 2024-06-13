/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import com.intellij.openapi.util.text.StringUtil.escapeStringCharacters
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerArgumentsLogLevel
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.logging.gradleLogLevel
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.inject.Inject

internal abstract class KotlinNativeToolRunner @Inject constructor(
    private val metricsReporterProvider: Provider<BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>>,
    private val classLoadersCachingBuildServiceProvider: Provider<ClassLoadersCachingBuildService>,
    private val toolSpec: ToolSpec,
    private val execOperations: ExecOperations,
) {
    private val logger = Logging.getLogger(toolSpec.displayName.get())
    private val classLoadersCachingBuildService: ClassLoadersCachingBuildService
        get() = classLoadersCachingBuildServiceProvider.get()
    private val metricsReporter get() = metricsReporterProvider.get()

    fun runTool(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.RUN_COMPILATION_IN_WORKER) {
            if (args.shouldRunInProcessMode) {
                runInProcess(args)
            } else {
                runViaExec(args)
            }
        }
    }

    private fun runViaExec(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.NATIVE_IN_EXECUTOR) {
            val systemProperties = System.getProperties()
                /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
                .snapshot()
                .asSequence()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k !in toolSpec.systemPropertiesBlacklist }
                .escapeQuotesForWindows()
                .toMap() + toolSpec.systemProperties

            val toolArgsPair = if (toolSpec.shouldPassArgumentsViaArgFile.get()) {
                val argFile = args.toArgFile()
                argFile to listOfNotNull(
                    toolSpec.optionalToolName.orNull,
                    "@${argFile.toFile().absolutePath}"
                )
            } else {
                null to args.arguments
            }

            try {
                logger.log(
                    args.compilerArgumentsLogLevel.gradleLogLevel,
                    """
                |Run "${toolSpec.displayName.get()}" tool in a separate JVM process
                |Main class = ${toolSpec.mainClass.get()}
                |Arguments = ${args.arguments.toPrettyString()}
                |Transformed arguments = ${toolArgsPair.second.toPrettyString()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |JVM options = ${toolSpec.jvmArgs.get().toPrettyString()}
                |Java system properties = ${systemProperties.toPrettyString()}
                |Suppressed ENV variables = ${toolSpec.environmentBlacklist.toPrettyString()}
                |Custom ENV variables = ${toolSpec.environment.toPrettyString()}
                """.trimMargin()
                )

                execOperations.javaexec { spec ->
                    spec.mainClass.set(toolSpec.mainClass)
                    spec.classpath = toolSpec.classpath
                    spec.jvmArgs(toolSpec.jvmArgs.get())
                    spec.systemProperties(toolSpec.systemProperties)
                    spec.environment(toolSpec.environment)
                    spec.args(toolArgsPair.second)
                }
            } finally {
                toolArgsPair.first?.let {
                    try {
                        Files.deleteIfExists(it)
                    } catch (_: IOException) {}
                }
            }
        }
    }

    private fun runInProcess(args: ToolArguments) {
        metricsReporter.measure(GradleBuildTime.NATIVE_IN_PROCESS) {
            val isolatedClassLoader = classLoadersCachingBuildService.getClassLoader(toolSpec.classpath.files.toList())
            if (toolSpec.jvmArgs.get().contains("-ea")) isolatedClassLoader.setDefaultAssertionStatus(true)

            logger.log(
                args.compilerArgumentsLogLevel.gradleLogLevel,
                """
                |Run in-process tool "${toolSpec.displayName.get()}"
                |Entry point method = ${toolSpec.mainClass.get()}.${toolSpec.daemonEntryPoint.get()}
                |Classpath = ${toolSpec.classpath.files.map { it.absolutePath }.toPrettyString()}
                |Arguments = ${args.arguments.toPrettyString()}
                """.trimMargin()
            )


            try {
                val mainClass = isolatedClassLoader.loadClass(toolSpec.mainClass.get())
                val entryPoint = mainClass
                    .methods
                    .singleOrNull { it.name == toolSpec.daemonEntryPoint.get() }
                    ?: error("Couldn't find daemon entry point '${toolSpec.daemonEntryPoint.get()}'")

                metricsReporter.measure(GradleBuildTime.RUN_ENTRY_POINT) {
                    entryPoint.invoke(null, args.arguments.toTypedArray())
                }
            } catch (t: InvocationTargetException) {
                throw t.targetException
            }
        }
    }

    private fun Properties.snapshot(): Properties = clone() as Properties

    private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
        if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun Map<String, String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.entries.forEach { (key, value) ->
            append('\t').append(key).append(" = ").append(value.toPrettyString()).append('\n')
        }
        append(']')
    }

    private fun Collection<String>.toPrettyString(): String = buildString {
        append('[')
        if (this@toPrettyString.isNotEmpty()) append('\n')
        this@toPrettyString.forEach { append('\t').append(it.toPrettyString()).append('\n') }
        append(']')
    }

    private fun String.toPrettyString(): String =
        when {
            isEmpty() -> "\"\""
            any { it == '"' || it.isWhitespace() } -> '"' + escapeStringCharacters(this) + '"'
            else -> this
        }

    private fun ToolArguments.toArgFile(): Path {
        val argFile = Files.createTempFile(
            "kotlinc-native-args",
            ".lst"
        )

        argFile.toFile().printWriter().use { w ->
            arguments.forEach { arg ->
                val escapedArg = arg
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return argFile
    }

    class ToolSpec(
        val displayName: Provider<String>,
        val optionalToolName: Provider<String>,
        val mainClass: Provider<String>,
        val daemonEntryPoint: Provider<String>,
        val classpath: FileCollection,
        val jvmArgs: ListProperty<String>,
        val shouldPassArgumentsViaArgFile: Provider<Boolean>,
        val systemProperties: Map<String, String> = emptyMap(),
        val environment: Map<String, String> = emptyMap(),
        val environmentBlacklist: Map<String, String> = emptyMap(),
    ) {
        val systemPropertiesBlacklist: Set<String> = setOf(
            "java.endorsed.dirs",       // Fix for KT-25887
            "user.dir",                 // Don't propagate the working dir of the current Gradle process
            "java.system.class.loader"  // Don't use custom class loaders
        )

        /**
         * Disable C2 compiler for HotSpot VM to improve compilation speed.
         */
        fun disableC2(): ToolSpec {
            System.getProperty("java.vm.name")?.let { vmName ->
                if (vmName.contains("HotSpot", true)) jvmArgs.add("-XX:TieredStopAtLevel=1")
            }

            return this
        }

        fun enableAssertions(): ToolSpec {
            jvmArgs.add("-ea")

            return this
        }

        fun configureDefaultMaxHeapSize(): ToolSpec {
            jvmArgs.add("-Xmx3g")

            return this
        }
    }

    data class ToolArguments(
        val shouldRunInProcessMode: Boolean,
        val compilerArgumentsLogLevel: KotlinCompilerArgumentsLogLevel,
        val arguments: List<String>,
    )
}
