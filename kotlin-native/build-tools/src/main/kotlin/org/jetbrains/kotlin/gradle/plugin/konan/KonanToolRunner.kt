/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import kotlinBuildProperties
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin.ProjectProperty.KONAN_HOME
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.target.AbstractToolConfig
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal interface KonanToolRunner {
    fun run(args: List<String>)
}

internal fun KonanToolRunner.run(vararg args: String) = run(args.toList())

private const val runFromDaemonPropertyName = "kotlin.native.tool.runFromDaemon"

internal abstract class KonanCliRunner(
        protected val toolName: String,
        project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        val additionalJvmArgs: List<String> = emptyList(),
        val konanHome: String = project.konanHome
) : KotlinToolRunner(project), KonanToolRunner {
    final override val displayName get() = toolName

    final override val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    final override val daemonEntryPoint get() = "daemonMain"

    override val mustRunViaExec get() = false.also { System.setProperty(runFromDaemonPropertyName, "true") }

    final override val execSystemPropertiesBlacklist: Set<String>
        get() = super.execSystemPropertiesBlacklist + runFromDaemonPropertyName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    final override val execEnvironmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KonanPlugin::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    final override val execSystemProperties by lazy { mapOf("konan.home" to konanHome) }

    final override val classpath by lazy {
        project.fileTree("$konanHome/konan/lib/").apply {
            include("trove4j.jar")
            include("kotlin-native-compiler-embeddable.jar")
        }.files
    }

    final override fun checkClasspath() =
            check(classpath.isNotEmpty()) {
                """
                Classpath of the tool is empty: $toolName
                Probably the '${KONAN_HOME.propertyName}' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
            }

    data class IsolatedClassLoaderCacheKey(val classpath: Set<File>)

    // TODO: can't we use this for other implementations too?
    final override val isolatedClassLoaderCacheKey get() = IsolatedClassLoaderCacheKey(classpath)

    // A separate map for each build for automatic cleaning the daemon after the build have finished.
    final override val isolatedClassLoaders = isolatedClassLoadersService.isolatedClassLoaders

    override fun transformArgs(args: List<String>) = listOf(toolName) + args

    final override fun getCustomJvmArgs() = additionalJvmArgs
}

/** Kotlin/Native compiler runner */
internal class KonanCliCompilerRunner(
        project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        additionalJvmArgs: List<String> = emptyList(),
        val useArgFile: Boolean = true,
        konanHome: String = project.konanHome
) : KonanCliRunner("konanc", project, isolatedClassLoadersService, additionalJvmArgs, konanHome) {
    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) return super.transformArgs(args)

        val argFile = Files.createTempFile(/* prefix = */ "konancArgs", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            for (arg in args) {
                val escapedArg = arg
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return listOf(toolName, "@${argFile.absolutePath}")
    }
}

private val load0 = Runtime::class.java.getDeclaredMethod("load0", Class::class.java, String::class.java).also {
    it.isAccessible = true
}

internal class CliToolConfig(konanHome: String, target: String) : AbstractToolConfig(konanHome, target, emptyMap()) {
    override fun loadLibclang() {
        // Load libclang into the system class loader. This is needed to allow developers to make changes
        // in the tooling infrastructure without having to stop the daemon (otherwise libclang might end up
        // loaded in two different class loaders which is not allowed by the JVM).
        load0.invoke(Runtime.getRuntime(), String::class.java, libclang)
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KonanCliInteropRunner(
        private val project: Project,
        isolatedClassLoadersService: KonanCliRunnerIsolatedClassLoadersService,
        additionalJvmArgs: List<String> = emptyList(),
        konanHome: String = project.konanHome
) : KonanCliRunner("cinterop", project, isolatedClassLoadersService, additionalJvmArgs, konanHome) {
    private val projectDir = project.projectDir.toString()

    override val mustRunViaExec: Boolean
        get() = if (project.kotlinBuildProperties.getBoolean("kotlin.native.allowRunningCinteropInProcess")) {
            super.mustRunViaExec
        } else {
            true
        }

    override fun transformArgs(args: List<String>): List<String> {
        return super.transformArgs(args) + listOf("-Xproject-dir", projectDir)
    }

    override val execEnvironment by lazy {
        val result = mutableMapOf<String, String>()
        result.putAll(super.execEnvironment)
        result["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
        llvmExecutablesPath?.let {
            result["PATH"] = "$it;${System.getenv("PATH")}"
        }
        result
    }

    fun init(target: String) {
        CliToolConfig(konanHome, target).prepare()
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                project.file("$konanHome/konan/konan.properties").inputStream().use(::load)
            }

            konanProperties.resolvablePropertyString("llvmHome.mingw_x64")?.let { toolchainDir ->
                DependencyDirectories.defaultDependenciesRoot
                        .resolve("$toolchainDir/bin")
                        .absolutePath
            }
        } else
            null
    }
}

abstract class KotlinToolRunner(
        private val objectsFactory: ObjectFactory,
        private val javaexec: ((JavaExecSpec) -> Unit) -> ExecResult,
        private val logger: Logger,
) {
    constructor(project: Project) : this(project.objects, { spec -> project.javaexec(spec) }, project.logger)

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

    open fun run(args: List<String>) {
        checkClasspath()

        if (mustRunViaExec) runViaExec(args) else runInProcess(args)
    }


    private fun runViaExec(args: List<String>) {
        val transformedArgs = transformArgs(args)
        val classpath = objectsFactory.fileCollection().from(classpath)
        val systemProperties = System.getProperties()
                /* Capture 'System.getProperties()' current state to avoid potential 'ConcurrentModificationException' */
                .snapshot()
                .asSequence()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k !in execSystemPropertiesBlacklist }
                .escapeQuotesForWindows()
                .toMap() + execSystemProperties


        logger.log(
                LogLevel.INFO,
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

        javaexec { spec ->
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

        logger.log(
                LogLevel.INFO,
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
