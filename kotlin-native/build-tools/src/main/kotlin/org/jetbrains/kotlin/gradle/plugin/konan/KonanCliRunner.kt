/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.*

private const val runFromDaemonPropertyName = "kotlin.native.tool.runFromDaemon"

internal abstract class KonanCliRunner(
        protected val toolName: String,
        private val fileOperations: FileOperations,
        private val execOperations: ExecOperations,
        private val logger: Logger,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        private val konanHome: String,
) {
    private val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    private val daemonEntryPoint get() = "daemonMain"

    protected open val execEnvironment: Map<String, String> = emptyMap()
    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    private val execEnvironmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KonanCliRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    private val execSystemPropertiesBlacklist: Set<String> = setOf(
            "java.endorsed.dirs",       // Fix for KT-25887
            "user.dir",                 // Don't propagate the working dir of the current Gradle process
            "java.system.class.loader",  // Don't use custom class loaders
            "runFromDaemonPropertyName"
    )

    private val classpath: Set<File> by lazy {
        fileOperations.fileTree("$konanHome/konan/lib/").apply {
            include("trove4j.jar")
            include("kotlin-native-compiler-embeddable.jar")
        }.files
    }

    private data class IsolatedClassLoaderCacheKey(val classpath: Set<File>)

    // A separate map for each build for automatic cleaning the daemon after the build have finished.
    private val isolatedClassLoaders = isolatedClassLoadersService.isolatedClassLoaders

    private fun getIsolatedClassLoader(): URLClassLoader = isolatedClassLoaders.computeIfAbsent(IsolatedClassLoaderCacheKey(classpath)) {
        val arrayOfURLs = classpath.map { File(it.absolutePath).toURI().toURL() }.toTypedArray()
        URLClassLoader(arrayOfURLs, null).apply {
            setDefaultAssertionStatus(true)
        }
    }

    protected open val mustRunViaExec get() = false.also { System.setProperty(runFromDaemonPropertyName, "true") }
    protected open fun transformArgs(args: List<String>) = listOf(toolName) + args

    private val jvmArgs: List<String> by lazy {
        mutableListOf<String>().apply {
            add("-ea")
            add("-Xmx3G")

            // Disable C2 compiler for HotSpot VM to improve compilation speed.
            System.getProperty("java.vm.name")?.let { vmName ->
                if (vmName.contains("HotSpot", true)) add("-XX:TieredStopAtLevel=1")
            }
        }
    }

    fun run(args: List<String>) {
        check(classpath.isNotEmpty()) {
            """
                Classpath of the tool is empty: $toolName
                Probably the 'kotlin.native.home' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
        }

        if (mustRunViaExec) runViaExec(args) else runInProcess(args)
    }

    private fun runViaExec(args: List<String>) {
        val transformedArgs = transformArgs(args)
        val systemProperties = System.getProperties()
                /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
                .snapshot()
                .asSequence()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k !in execSystemPropertiesBlacklist }
                .escapeQuotesForWindows()
                .toMap() + mapOf("konan.home" to konanHome)


        logger.log(
                LogLevel.INFO,
                """|Run "$toolName" tool in a separate JVM process
                   |Main class = $mainClass
                   |Arguments = ${args.toPrettyString()}
                   |Transformed arguments = ${if (transformedArgs == args) "same as arguments" else transformedArgs.toPrettyString()}
                   |Classpath = ${classpath.map { it.absolutePath }.toPrettyString()}
                   |JVM options = ${jvmArgs.toPrettyString()}
                   |Java system properties = ${systemProperties.toPrettyString()}
                   |Suppressed ENV variables = ${execEnvironmentBlacklist.toPrettyString()}
                   |Custom ENV variables = ${execEnvironment.toPrettyString()}
                """.trimMargin()
        )

        execOperations.javaexec {
            this.mainClass.set(this@KonanCliRunner.mainClass)
            this.classpath(this@KonanCliRunner.classpath)
            this.jvmArgs(this@KonanCliRunner.jvmArgs)
            this.systemProperties(systemProperties)
            execEnvironmentBlacklist.forEach { this.environment.remove(it) }
            this.environment(execEnvironment)
            this.args(transformedArgs)
        }
    }

    private fun runInProcess(args: List<String>) {
        val transformedArgs = transformArgs(args)
        val isolatedClassLoader = getIsolatedClassLoader()

        logger.log(
                LogLevel.INFO,
                """|Run in-process tool "$toolName"
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
                    any { it == '"' || it.isWhitespace() } -> '"' + escapeStringCharacters() + '"'
                    else -> this
                }

        private fun Properties.snapshot(): Properties = clone() as Properties

        private fun String.escapeStringCharacters(): String {
            val buffer = StringBuilder(length)
            escapeStringCharacters(length, "\"", true, true, buffer)
            return buffer.toString()
        }

        private fun String.escapeStringCharacters(
                length: Int,
                additionalChars: String?,
                escapeSlash: Boolean,
                escapeUnicode: Boolean,
                buffer: StringBuilder
        ): StringBuilder {
            var prev = 0.toChar()
            for (idx in 0..<length) {
                val ch = this[idx]
                when (ch) {
                    '\b' -> buffer.append("\\b")
                    '\t' -> buffer.append("\\t")
                    '\n' -> buffer.append("\\n")
                    '\u000c' -> buffer.append("\\f")
                    '\r' -> buffer.append("\\r")
                    else -> if (escapeSlash && ch == '\\') {
                        buffer.append("\\\\")
                    } else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
                        buffer.append("\\").append(ch)
                    } else if (escapeUnicode && !isPrintableUnicode(ch)) {
                        val hexCode: CharSequence = Integer.toHexString(ch.code).uppercase()
                        buffer.append("\\u")
                        var paddingCount = 4 - hexCode.length
                        while (paddingCount-- > 0) {
                            buffer.append(0)
                        }
                        buffer.append(hexCode)
                    } else {
                        buffer.append(ch)
                    }
                }
                prev = ch
            }
            return buffer
        }

        private fun isPrintableUnicode(c: Char): Boolean {
            val t = Character.getType(c)
            return t != Character.UNASSIGNED.toInt() &&
                    t != Character.LINE_SEPARATOR.toInt() &&
                    t != Character.PARAGRAPH_SEPARATOR.toInt() &&
                    t != Character.CONTROL.toInt() &&
                    t != Character.FORMAT.toInt() &&
                    t != Character.PRIVATE_USE.toInt() &&
                    t != Character.SURROGATE.toInt()
        }
    }
}
