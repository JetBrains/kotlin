/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import com.intellij.openapi.util.text.StringUtil.escapeStringCharacters
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

internal abstract class KotlinToolRunner(
    val project: Project
) {
    // name that will be used in logs
    abstract val displayName: String

    abstract val mainClass: String
    open val daemonEntryPoint: String get() = "main"

    open val execEnvironment: Map<String, String> = emptyMap()
    open val execEnvironmentBlacklist: Set<String> = emptySet()

    open val execSystemProperties: Map<String, String> = emptyMap()
    open val execSystemPropertiesBlacklist: Set<String> = setOf("java.endorsed.dirs")

    abstract val classpath: Set<File>
    open fun checkClasspath(): Unit = check(classpath.isNotEmpty()) { "Classpath of the tool is empty: $displayName" }

    abstract val isolatedClassLoaderCacheKey: Any
    private fun getIsolatedClassLoader(): ClassLoader = isolatedClassLoadersMap.computeIfAbsent(isolatedClassLoaderCacheKey) {
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
        val classpath = project.files(classpath)
        val systemProperties = System.getProperties().asSequence()
            .map { (k, v) -> k.toString() to v.toString() }
            .filter { (k, _) -> k !in execSystemPropertiesBlacklist }
            .escapeQuotesForWindows()
            .toMap() + execSystemProperties

        project.logger.info(
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

        project.javaexec { spec ->
            spec.main = mainClass
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

        project.logger.info(
            """|Run in-process tool "$displayName"
               |Entry point method = $mainClass.$daemonEntryPoint
               |Arguments = ${args.toPrettyString()}
               |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
            """.trimMargin()
        )

        try {
            val mainClass = getIsolatedClassLoader().loadClass(mainClass)
            val entryPoint = mainClass.methods.single { it.name == daemonEntryPoint }

            entryPoint.invoke(null, transformedArgs.toTypedArray())
        } catch (t: InvocationTargetException) {
            throw t.targetException
        }
    }

    companion object {
        private fun String.escapeQuotes() = replace("\"", "\\\"")

        private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
            if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

        private val isolatedClassLoadersMap = ConcurrentHashMap<Any, ClassLoader>()

        private fun Map<String, String>.toPrettyString(): String = buildString {
            append('{')
            if (this@toPrettyString.isNotEmpty()) append('\n')
            this@toPrettyString.entries.forEach { (key, value) ->
                append('\t').append(key).append(" = ").append(value.toPrettyString()).append('\n')
            }
            append('}')
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
    }
}
