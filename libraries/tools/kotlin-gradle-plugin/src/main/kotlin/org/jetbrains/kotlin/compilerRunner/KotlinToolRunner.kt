/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

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

    open val environment: Map<String, String> = emptyMap()
    open val environmentBlacklist: Set<String> = emptySet()

    open val systemProperties: Map<String, String> = emptyMap()
    open val systemPropertiesBlacklist: Set<String> = emptySet()

    abstract val classpath: Set<File>
    open fun checkClasspath(): Unit = check(classpath.isNotEmpty()) { "Classpath of the tool is empty: $displayName" }

    abstract val isolatedClassLoaderCacheKey: Any
    private fun getIsolatedClassLoader(): ClassLoader = isolatedClassLoadersMap.computeIfAbsent(isolatedClassLoaderCacheKey) {
        val arrayOfURLs = classpath.map { File(it.absolutePath).toURI().toURL() }.toTypedArray()
        URLClassLoader(arrayOfURLs, null)
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
        project.logger.info("Run tool: \"$displayName\" with args: ${args.joinToString(separator = " ")}")
        checkClasspath()

        if (mustRunViaExec) runViaExec(args) else runInProcess(args)
    }

    private fun runViaExec(args: List<String>) {
        project.javaexec { spec ->
            spec.main = mainClass
            spec.classpath = project.files(classpath)
            spec.jvmArgs(jvmArgs)
            spec.systemProperties(
                System.getProperties().asSequence()
                    .map { (k, v) -> k.toString() to v.toString() }
                    .filter { (k, _) -> k !in systemPropertiesBlacklist }
                    .escapeQuotesForWindows()
                    .toMap()
            )
            spec.systemProperties(systemProperties)
            environmentBlacklist.forEach { spec.environment.remove(it) }
            spec.environment(environment)
            spec.args(transformArgs(args))
        }
    }

    private fun runInProcess(args: List<String>) {
        val oldProperties = setUpSystemProperties()

        try {
            val mainClass = getIsolatedClassLoader().loadClass(mainClass)
            val entryPoint = mainClass.methods.single { it.name == daemonEntryPoint }

            entryPoint.invoke(null, transformArgs(args).toTypedArray())
        } catch (t: InvocationTargetException) {
            throw t.targetException
        } finally {
            restoreSystemProperties(oldProperties)
        }
    }

    private fun setUpSystemProperties(): Map<String, String?> {
        val oldProperties = mutableMapOf<String, String?>()

        systemProperties.forEach { (k, v) -> oldProperties[k] = System.getProperty(v) }

        System.getProperties().toList()
            .map { (k, v) -> k.toString() to v.toString() }
            .filter { (k, _) -> k in systemPropertiesBlacklist }
            .forEach { (k, v) ->
                oldProperties[k] = v
                System.clearProperty(k)
            }

        systemProperties.forEach { (k, v) -> System.setProperty(k, v) }

        return oldProperties
    }

    private fun restoreSystemProperties(oldProperties: Map<String, String?>) {
        oldProperties.forEach { (k, v) ->
            if (v == null) System.clearProperty(k) else System.setProperty(k, v)
        }
    }

    companion object {
        private fun String.escapeQuotes() = replace("\"", "\\\"")

        private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
            if (HostManager.hostIsMingw) map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() } else this

        private val isolatedClassLoadersMap = ConcurrentHashMap<Any, ClassLoader>()
    }
}
