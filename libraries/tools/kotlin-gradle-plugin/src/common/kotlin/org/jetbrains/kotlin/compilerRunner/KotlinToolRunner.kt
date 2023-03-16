/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import com.intellij.openapi.util.text.StringUtil.escapeStringCharacters
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Note: this class is public because it is used in the K/N build infrastructure.
abstract class KotlinToolRunner(
    private val executionContext: GradleExecutionContext
) {
    @Deprecated(
        "Using Project object is not compatible with Gradle Configuration Cache",
        ReplaceWith("KotlinToolRunner(GradleExecutionContext.fromTaskContext())"),
        DeprecationLevel.WARNING
    )
    constructor(project: Project): this(GradleExecutionContext.fromProject(project))

    /**
     * Context Services that are required for [KotlinToolRunner] during Gradle Task Execution Phase
     */
    class GradleExecutionContext(
        val filesProvider: (Any) -> ConfigurableFileCollection,
        val javaexec: ((JavaExecSpec) -> Unit) -> ExecResult,
        val logger: Logger,
    ) {
        companion object {
            /**
             * Executing [KotlinToolRunner] during Gradle Configuration Phase is undesired behaviour.
             * Currently only [KotlinNativeLibraryGenerationRunner] used in this way.
             * It should be fixed as part of KT-51255
             */
            @Deprecated(
                "Building execution context from Project object isn't compatible with Gradle Configuration Cache",
                ReplaceWith("fromTaskContext()"),
                DeprecationLevel.WARNING
            )
            fun fromProject(project: Project) = GradleExecutionContext(
                filesProvider = project::files,
                javaexec = { spec -> project.javaexec(spec) }, // project::javaexec won't work due to different Classloaders
                logger = project.logger
            )

            /** Gradle Configuration Cache friendly context, should be used inside Task Execution Phase */
            fun fromTaskContext(
                objectFactory: ObjectFactory,
                execOperations: ExecOperations,
                logger: Logger
            ) = GradleExecutionContext(
                filesProvider = objectFactory.fileCollection()::from,
                javaexec = { spec -> execOperations.javaexec(spec) }, // execOperations::javaexec won't work due to different Classloaders
                logger = logger
            )
        }
    }

    // name that will be used in logs
    abstract val displayName: String

    abstract val mainClass: String
    open val daemonEntryPoint: String get() = "main"

    open val execEnvironment: Map<String, String> = emptyMap()
    open val execEnvironmentBlacklist: Set<String> = emptySet()

    open val execSystemProperties: Map<String, String> = emptyMap()
    open val execSystemPropertiesBlacklist: Set<String> = setOf(
        "java.endorsed.dirs",       // Fix for KT-25887
        "user.dir",                 // Don't propagate the working dir of the current Gradle process
        "java.system.class.loader"  // Don't use custom class loaders
    )

    abstract val classpath: Set<File>
    open fun checkClasspath(): Unit = check(classpath.isNotEmpty()) { "Classpath of the tool is empty: $displayName" }

    abstract val isolatedClassLoaderCacheKey: Any
    protected open val isolatedClassLoaders: ConcurrentHashMap<Any, URLClassLoader> get() = isolatedClassLoadersMap

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaders.computeIfAbsent(isolatedClassLoaderCacheKey) {
        val arrayOfURLs = classpath.map { File(it.absolutePath).toURI().toURL() }.toTypedArray()
        URLClassLoader(arrayOfURLs, null).apply {
            setDefaultAssertionStatus(enableAssertions)
        }
    }

    open val defaultMaxHeapSize: String get() = "3G"
    open val enableAssertions: Boolean get() = true
    open val disableC2: Boolean get() = true

    abstract val mustRunViaExec: Boolean
    open fun transformArgs(args: List<String>): List<String> = args

    // for the purpose if there is a way to specify JVM args, for instance, straight in project configs
    open fun getCustomJvmArgs(): List<String> = emptyList()

    private val jvmArgs: List<String> by lazy {
        mutableListOf<String>().apply {
            if (enableAssertions) add("-ea")

            val customJvmArgs = getCustomJvmArgs()
            if (customJvmArgs.none { it.startsWith("-Xmx") }) add("-Xmx$defaultMaxHeapSize")

            // Disable C2 compiler for HotSpot VM to improve compilation speed.
            if (disableC2) {
                System.getProperty("java.vm.name")?.let { vmName ->
                    if (vmName.contains("HotSpot", true)) add("-XX:TieredStopAtLevel=1")
                }
            }

            addAll(customJvmArgs)
        }
    }

    fun run(args: List<String>) {
        checkClasspath()

        if (mustRunViaExec) runViaExec(args) else runInProcess(args)
    }

    private fun runViaExec(args: List<String>) {
        val transformedArgs = transformArgs(args)
        val classpath = executionContext.filesProvider(classpath)
        val systemProperties = System.getProperties()
            /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
            .snapshot()
            .asSequence()
            .map { (k, v) -> k.toString() to v.toString() }
            .filter { (k, _) -> k !in execSystemPropertiesBlacklist }
            .escapeQuotesForWindows()
            .toMap() + execSystemProperties

        executionContext.logger.info(
            """|Run "$displayName" tool in a separate JVM process
               |Main class = $mainClass
               |Arguments = ${args.toPrettyString()}
               |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
               |Classpath = ${classpath.files.map { it.absolutePath }.toPrettyString()}
               |JVM options = ${jvmArgs.toPrettyString()}
               |Java system properties = ${systemProperties.toPrettyString()}
               |Suppressed ENV variables = ${execEnvironmentBlacklist.toPrettyString()}
               |Custom ENV variables = ${execEnvironment.toPrettyString()}
            """.trimMargin()
        )

        executionContext.javaexec { spec ->
            spec.mainClass.set(mainClass)
            spec.classpath = classpath
            spec.jvmArgs(jvmArgs)
            spec.systemProperties(systemProperties)
            execEnvironmentBlacklist.forEach { spec.environment.remove(it) }
            spec.environment(execEnvironment)
            spec.args(transformedArgs)
        }
    }

    private fun runInProcess(args: List<String>) {
        val transformedArgs = transformArgs(args)
        val isolatedClassLoader = getIsolatedClassLoader()

        executionContext.logger.info(
            """|Run in-process tool "$displayName"
               |Entry point method = $mainClass.$daemonEntryPoint
               |Classpath = ${isolatedClassLoader.urLs.map { it.file }.toPrettyString()}
               |Arguments = ${args.toPrettyString()}
               |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
            """.trimMargin()
        )

        try {
            val mainClass = isolatedClassLoader.loadClass(mainClass)
            val entryPoint = mainClass.methods
                .singleOrNull { it.name == daemonEntryPoint } ?: error("Couldn't find daemon entry point '$daemonEntryPoint'")

            entryPoint.invoke(null, transformedArgs.toTypedArray())
        } catch (t: InvocationTargetException) {
            throw t.targetException
        }
    }

    companion object {
        private fun String.escapeQuotes() = replace("\"", "\\\"")

        private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
            if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

        private val isolatedClassLoadersMap = ConcurrentHashMap<Any, URLClassLoader>()

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

        private fun Properties.snapshot(): Properties = clone() as Properties
    }
}
