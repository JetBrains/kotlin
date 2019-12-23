/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_HOME
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/** Copied from Kotlin/Native repository. */

private val Project.jvmArgs
    get() = PropertiesProvider(this).nativeJvmArgs?.split("\\s+".toRegex()).orEmpty()

internal val Project.konanHome: String
    get() = PropertiesProvider(this).nativeHome?.let {
        file(it).absolutePath
    } ?: NativeCompilerDownloader(project).compilerDirectory.absolutePath

internal val Project.konanVersion: CompilerVersion
    get() = PropertiesProvider(this).nativeVersion?.let {
        CompilerVersion.fromString(it.toString())
    } ?: NativeCompilerDownloader.DEFAULT_KONAN_VERSION

internal val Project.disableKonanDaemon: Boolean
    get() = PropertiesProvider(this).nativeDisableCompilerDaemon

internal val Project.konanCacheKind: NativeCacheKind
    get() = PropertiesProvider(this).nativeCacheKind

internal interface KonanToolRunner : Named {
    val mainClass: String
    val classpath: FileCollection
    val jvmArgs: List<String>
    val environment: Map<String, Any>
    val additionalSystemProperties: Map<String, String>

    fun run(args: List<String>)
    fun run(vararg args: String) = run(args.toList())
}

internal abstract class KonanCliRunner(
    val toolName: String,
    val fullName: String,
    val project: Project,
    private val additionalJvmArgs: List<String>
) : KonanToolRunner {
    override val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"

    override fun getName() = toolName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    protected val blacklistEnvironment: List<String> by lazy {
        KonanToolRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
            stream.reader().use { it.readLines() }
        } ?: emptyList<String>()
    }

    protected val blacklistProperties: Set<String> =
        setOf("java.endorsed.dirs")

    override val classpath: FileCollection =
        project.fileTree("${project.konanHome}/konan/lib/")
            .apply { include("*.jar") }

    override val jvmArgs = mutableListOf("-ea").apply {
        if (additionalJvmArgs.none { it.startsWith("-Xmx") } &&
            project.jvmArgs.none { it.startsWith("-Xmx") }) {
            add("-Xmx3G")
        }
        // Disable C2 compiler for HotSpot VM to improve compilation speed.
        System.getProperty("java.vm.name")?.let {
            if (it.contains("HotSpot", true)) {
                add("-XX:TieredStopAtLevel=1")
            }
        }
        addAll(additionalJvmArgs)
        addAll(project.jvmArgs)
    }

    override val additionalSystemProperties = mutableMapOf(
        "konan.home" to project.konanHome,
        MessageRenderer.PROPERTY_KEY to MessageRenderer.GRADLE_STYLE.name,
        "java.library.path" to "${project.konanHome}/konan/nativelib"
    )

    override val environment = mutableMapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
        if (HostManager.hostIsMingw) {
            map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() }
        } else {
            this
        }

    protected open fun transformArgs(args: List<String>): List<String> = args

    override fun run(args: List<String>) {
        project.logger.info("Run tool: $toolName with args: ${args.joinToString(separator = " ")}")
        if (classpath.isEmpty) {
            throw IllegalStateException(
                "Classpath of the tool is empty: $toolName\n" +
                        "Probably the '$KOTLIN_NATIVE_HOME' project property contains an incorrect path.\n" +
                        "Please change it to the compiler root directory and rerun the build."
            )
        }

        if (project.disableKonanDaemon || toolName == "cinterop") {
            project.javaexec { spec ->
                spec.main = mainClass
                spec.classpath = classpath
                spec.jvmArgs(jvmArgs)
                spec.systemProperties(
                    System.getProperties().asSequence()
                        .map { (k, v) -> k.toString() to v.toString() }
                        .filter { (k, _) -> k !in blacklistProperties }
                        .escapeQuotesForWindows()
                        .toMap()
                )
                spec.systemProperties(additionalSystemProperties)
                spec.args(listOf(toolName) + transformArgs(args))
                blacklistEnvironment.forEach { spec.environment.remove(it) }
                spec.environment(environment)
            }
        } else {
            val oldProperties = mutableMapOf<String, String?>()
            additionalSystemProperties.forEach {
                oldProperties[it.key] = System.getProperty(it.key)
            }
            System.getProperties().toList()
                .map { (k, v) -> k.toString() to v.toString() }
                .filter { (k, _) -> k in blacklistProperties }
                .forEach { (k, v) ->
                    oldProperties[k] = v
                    System.clearProperty(k)
                }

            additionalSystemProperties.forEach { System.setProperty(it.key, it.value) }

            val konanCompilerClassLoader = konanCompilerClassLoadersMap.computeIfAbsent(project.konanHome) {
                val arrayOfURLs = classpath.map { File(it.absolutePath).toURI().toURL() }.toTypedArray()
                URLClassLoader(arrayOfURLs, null)
            }

            val mainClass = konanCompilerClassLoader.loadClass(mainClass)
            val mainMethod = mainClass.methods.single { it.name == "daemonMain" }

            try {
                mainMethod.invoke(null, (listOf(toolName) + transformArgs(args)).toTypedArray())
            } catch (t: InvocationTargetException) {
                throw t.targetException
            } finally {
                oldProperties.forEach {
                    val value = it.value
                    if (value == null)
                        System.clearProperty(it.key)
                    else System.setProperty(it.key, value)
                }
            }
        }
    }
}

private val konanCompilerClassLoadersMap = ConcurrentHashMap<String, ClassLoader>()

internal class KonanInteropRunner(project: Project, additionalJvmArgs: List<String> = emptyList()) :
    KonanCliRunner("cinterop", "Kotlin/Native cinterop tool", project, additionalJvmArgs) {
    init {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                project.file("${project.konanHome}/konan/konan.properties").inputStream().use(::load)
            }
            val toolchainDir = konanProperties.getProperty("llvmHome.mingw_x64")
            if (toolchainDir != null) {
                environment.put(
                    "PATH",
                    DependencyDirectories.defaultDependenciesRoot
                        .resolve("$toolchainDir/bin")
                        .absolutePath + ";${System.getenv("PATH")}"
                )
            }
        }
    }
}

internal class KonanCompilerRunner(
    project: Project,
    additionalJvmArgs: List<String> = emptyList(),
    val useArgFile: Boolean = true
) : KonanCliRunner("konanc", "Kotlin/Native compiler", project, additionalJvmArgs) {
    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) {
            return args
        }

        val argFile = createTempFile(prefix = "konancArgs", suffix = ".lst").apply {
            deleteOnExit()
        }
        argFile.printWriter().use { writer ->
            args.forEach {
                writer.println(it)
            }
        }

        return listOf("@${argFile.absolutePath}")
    }
}

internal class KonanKlibRunner(project: Project, additionalJvmArgs: List<String> = emptyList()) :
    KonanCliRunner("klib", "Klib management tool", project, additionalJvmArgs)
