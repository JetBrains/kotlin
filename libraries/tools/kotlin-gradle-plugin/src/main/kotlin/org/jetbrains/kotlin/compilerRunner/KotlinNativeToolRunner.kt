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
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories

/** Copied from Kotlin/Native repository. */

//  TODO: Rename to FQ names
internal enum class KotlinNativeProjectProperty(val propertyName: String) {
    KONAN_HOME                     ("konan.home"),
    KONAN_JVM_ARGS                 ("konan.jvmArgs"),
    KONAN_USE_ENVIRONMENT_VARIABLES("konan.useEnvironmentVariables"),
    DOWNLOAD_COMPILER              ("download.compiler"),

    // Properties used instead of env vars until https://github.com/gradle/gradle/issues/3468 is fixed.
    // TODO: Remove them when an API for env vars is provided.
    KONAN_CONFIGURATION_BUILD_DIR  ("konan.configuration.build.dir"),
    KONAN_DEBUGGING_SYMBOLS        ("konan.debugging.symbols"),
    KONAN_OPTIMIZATIONS_ENABLE     ("konan.optimizations.enable"),
    KONAN_PUBLICATION_ENABLED      ("konan.publication.enabled")
}

internal fun Project.hasProperty(property: KotlinNativeProjectProperty) = hasProperty(property.propertyName)
internal fun Project.findProperty(property: KotlinNativeProjectProperty): Any? = findProperty(property.propertyName)

internal fun Project.setProperty(property: KotlinNativeProjectProperty, value: Any?) =
    extensions.extraProperties.set(property.propertyName, value)

internal fun Project.getProperty(property: KotlinNativeProjectProperty) = findProperty(property)
    ?: throw IllegalArgumentException("No such property in the project: ${property.propertyName}")

internal val Project.jvmArgs
    get() = (findProperty(KotlinNativeProjectProperty.KONAN_JVM_ARGS) as String?)?.split("\\s+".toRegex()).orEmpty()

// konanHome extension is set by downloadKonanCompiler task.
internal val Project.konanHome: String
    get() {
        assert(hasProperty(KotlinNativeProjectProperty.KONAN_HOME))
        return project.file(getProperty(KotlinNativeProjectProperty.KONAN_HOME)).canonicalPath
    }

internal interface KonanToolRunner: Named {
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
): KonanToolRunner {
    override val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"

    override fun getName() = toolName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    protected val blacklistEnvironment: List<String> by lazy {
        KonanToolRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
            stream.reader().use { it.readLines() }
        } ?: emptyList<String>()
    }

    override val classpath: FileCollection =
        project.fileTree("${project.konanHome}/konan/lib/")
            .apply { include("*.jar")  }

    override val jvmArgs = mutableListOf("-ea").apply {
        if (additionalJvmArgs.none { it.startsWith("-Xmx") } &&
            project.jvmArgs.none { it.startsWith("-Xmx") }) {
            add("-Xmx3G")
        }
        addAll(additionalJvmArgs)
        addAll(project.jvmArgs)
    }

    override val additionalSystemProperties = mutableMapOf(
        "konan.home" to project.konanHome,
        "java.library.path" to "${project.konanHome}/konan/nativelib"
    )

    override val environment = mutableMapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")

    private fun String.escapeQuotes() = replace("\"", "\\\"")

    private fun List<Pair<String, String>>.escapeQuotesForWindows() =
        if (HostManager.hostIsMingw) {
            map { (key, value) -> key.escapeQuotes() to value.escapeQuotes() }
        } else {
            this
        }

    override fun run(args: List<String>) {
        project.logger.info("Run tool: $toolName with args: ${args.joinToString(separator = " ")}")
        if (classpath.isEmpty) {
            throw IllegalStateException("Classpath of the tool is empty: $toolName\n" +
                                        "Probably the 'konan.home' project property contains an incorrect path.\n" +
                                        "Please change it to the compiler root directory and rerun the build.")
        }

        project.javaexec { spec ->
            spec.main = mainClass
            spec.classpath = classpath
            spec.jvmArgs(jvmArgs)
            spec.systemProperties(
                System.getProperties()
                    .map { (k, v) -> k.toString() to v.toString() }
                    .escapeQuotesForWindows()
                    .toMap()
            )
            spec.systemProperties(additionalSystemProperties)
            spec.args(listOf(toolName) + args)
            blacklistEnvironment.forEach { spec.environment.remove(it) }
            spec.environment(environment)
        }
    }
}

internal class KonanInteropRunner(project: Project, additionalJvmArgs: List<String> = emptyList())
    : KonanCliRunner("cinterop", "Kotlin/Native cinterop tool", project, additionalJvmArgs)
{
    init {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            //TODO: Oh-ho-ho fix it in more convinient way.
            environment.put("PATH", DependencyDirectories.defaultDependenciesRoot.absolutePath +
                            "\\msys2-mingw-w64-x86_64-gcc-7.2.0-clang-llvm-5.0.0-windows-x86-64" +
                            "\\bin;${environment.get("PATH")}")
        }
    }
}

internal class KonanCompilerRunner(project: Project, additionalJvmArgs: List<String> = emptyList())
    : KonanCliRunner("konanc", "Kotlin/Native compiler", project, additionalJvmArgs)

internal class KonanKlibRunner(project: Project, additionalJvmArgs: List<String> = emptyList())
    : KonanCliRunner("klib", "Klib management tool", project, additionalJvmArgs)
