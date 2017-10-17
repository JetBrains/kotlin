package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.tasks.host

internal interface KonanToolRunner: Named {
    val mainClass: String
    val classpath: FileCollection
    val jvmArgs: List<String>
    val environment: Map<String, Any>

    fun run(args: List<String>)
    fun run(vararg args: String) = run(args.toList())
}

internal abstract class KonanCliRunner(val toolName: String, val fullName: String, val project: Project): KonanToolRunner {
    override val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"

    override fun getName() = toolName

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    protected val blacklistEnvironment: List<String> by lazy {
        KonanPlugin::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
            stream.reader().use { it.readLines() }
        } ?: emptyList<String>()
    }

    override val classpath: FileCollection =
            project.fileTree("${project.konanHome}/konan/lib/")
            .apply { include("*.jar")  }

    override val jvmArgs = mutableListOf(
            "-ea",
            "-Xmx3G",
            "-Dkonan.home=${project.konanHome}",
            "-Djava.library.path=${project.konanHome}/konan/nativelib")

    override val environment = mutableMapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1")

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
            spec.args(listOf(toolName) + args)
            blacklistEnvironment.forEach { spec.environment.remove(it) }
            spec.environment(environment)
        }
    }
}

internal class KonanInteropRunner(project: Project)
    : KonanCliRunner("cinterop", "Kotlin/Native cinterop tool", project)
{
    init {
        if (project.host == "mingw") {
            environment.put("PATH", "${project.konanHome}\\dependencies" +
                    "\\msys2-mingw-w64-x86_64-gcc-6.3.0-clang-llvm-3.9.1-windows-x86-64" +
                    "\\bin;${System.getenv("PATH")}")
        }
    }
}

internal class KonanCompilerRunner(project: Project) : KonanCliRunner("konanc", "Kotlin/Native compiler", project)
internal class KonanKlibRunner(project: Project) : KonanCliRunner("klib", "Klib management tool", project)
