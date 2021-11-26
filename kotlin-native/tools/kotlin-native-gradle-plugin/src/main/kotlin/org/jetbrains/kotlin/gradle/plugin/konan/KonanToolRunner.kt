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

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin.ProjectProperty.KONAN_HOME
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import java.nio.file.Files
import org.jetbrains.kotlin.*
import java.io.ByteArrayOutputStream

internal interface KonanToolRunner : Named {
    val mainClass: String
    val classpath: FileCollection
    val jvmArgs: List<String>
    val environment: Map<String, Any>

    fun run(args: List<String>)
    fun run(vararg args: String) = run(args.toList())
}

internal abstract class KonanCliRunner(
        val toolName: String,
        val fullName: String,
        val project: Project,
        private val additionalJvmArgs: List<String>,
        private val konanHome: String
) : KonanToolRunner {
    override val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"

    override fun getName() = toolName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    protected val blacklistEnvironment: List<String> by lazy {
        KonanPlugin::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
            stream.reader().use { it.readLines() }
        } ?: emptyList<String>()
    }

    protected val blacklistProperties: Set<String> =
            setOf(
                    "java.endorsed.dirs", // Fix for KT-25887
                    "user.dir"            // Don't propagate the working dir of the current Gradle process
            )

    override val classpath: FileCollection =
            project.fileTree("$konanHome/konan/lib/")
                    .apply { include("*.jar") }

    override val jvmArgs = HostManager.defaultJvmArgs.toMutableList().apply {
        if (additionalJvmArgs.none { it.startsWith("-Xmx") } &&
                project.jvmArgs.none { it.startsWith("-Xmx") }) {
            add("-Xmx3G")
        }
        addAll(additionalJvmArgs)
        addAll(project.jvmArgs)
    }

    override val environment = mutableMapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun Sequence<Pair<String, String>>.escapeQuotesForWindows() =
            if (HostManager.hostIsMingw) {
                map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() }
            } else {
                this
            }

    open protected fun transformArgs(args: List<String>): List<String> = args

    override fun run(args: List<String>) {
        project.logger.info("Run tool: $toolName with args: ${args.joinToString(separator = " ")}")
        if (classpath.isEmpty) {
            throw IllegalStateException("Classpath of the tool is empty: $toolName\n" +
                    "Probably the '${KONAN_HOME.propertyName}' project property contains an incorrect path.\n" +
                    "Please change it to the compiler root directory and rerun the build.")
        }

        @Suppress("UNCHECKED_CAST")
        val launcher = project.getProperty(KonanPlugin.ProjectProperty.KONAN_JVM_LAUNCHER) as? Provider<JavaLauncher>
                ?: throw IllegalStateException("Missing property: ${KonanPlugin.ProjectProperty.KONAN_JVM_LAUNCHER}")

        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val execResult = project.exec(object : Action<ExecSpec> {
            override fun execute(exec: ExecSpec) {
                exec.executable = launcher.get().executablePath.toString()
                val properties = System.getProperties().asSequence()
                        .map { (k, v) -> k.toString() to v.toString() }
                        .filter { (k, _) -> k !in this@KonanCliRunner.blacklistProperties }
                        .filter { (k, _) -> !k.startsWith("sun") && !k.startsWith("java") }
                        .escapeQuotesForWindows()
                        .toMap()
                        .toMutableMap()
                properties.put("konan.home", project.kotlinNativeDist.absolutePath)

                exec.args(mutableListOf<String>().apply {
                    addAll(jvmArgs)
                    addAll(properties.entries.map { "-D${it.key}=${it.value}" })
                    add("-cp")
                    add(classpath.joinToString(separator = System.getProperty("path.separator")))
                    add(mainClass)
                    addAll(listOf(toolName) + transformArgs(args))
                })
                blacklistEnvironment.forEach { environment.remove(it) }
                exec.environment(environment)
                exec.errorOutput = err
                exec.standardOutput = out
                exec.isIgnoreExitValue = true
            }
        })

        check(execResult.exitValue == 0) {
            """
                stdout:$out
                stderr:$err
            """.trimIndent()
        }
    }
}

internal class KonanInteropRunner(
        project: Project,
        additionalJvmArgs: List<String> = emptyList(),
        konanHome: String = project.konanHome
) : KonanCliRunner("cinterop", "Kotlin/Native cinterop tool", project, additionalJvmArgs, konanHome) {
    init {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            //TODO: Oh-ho-ho fix it in more convinient way.
            environment.put("PATH", DependencyProcessor.defaultDependenciesRoot.absolutePath +
                    "\\llvm-11.1.0-windows-x64" +
                    "\\bin;${environment.get("PATH")}")
        }
    }
}

internal class KonanCompilerRunner(
        project: Project,
        additionalJvmArgs: List<String> = emptyList(),
        val useArgFile: Boolean = true,
        konanHome: String = project.konanHome
) : KonanCliRunner("konanc", "Kotlin/Native compiler", project, additionalJvmArgs, konanHome) {
    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) {
            return args
        }

        val argFile = Files.createTempFile("konancArgs", ".lst").toAbsolutePath().apply {
            toFile().deleteOnExit()
        }
        Files.write(argFile, args)

        return listOf("@${argFile}")
    }
}

internal class KonanKlibRunner(
        project: Project,
        additionalJvmArgs: List<String> = emptyList(),
        konanHome: String = project.konanHome
) : KonanCliRunner("klib", "Klib management tool", project, additionalJvmArgs, konanHome)
