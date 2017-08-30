package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

// Consider using JavaExecSPec
internal interface KonanToolRunner {
    val mainClass: String
    val classpath: FileCollection
    val jvmArgs: List<String>
    val environment: Map<String, Any>

    fun run(args: List<String>)
    fun run(vararg args: String) = run(args.toList())
}

internal abstract class KonanBaseRunner(val project: Project): KonanToolRunner {
    abstract val toolName: String

    override val classpath: FileCollection
        get() = project.fileTree("${project.konanHome}/konan/lib/").apply { include("*.jar")  }

    override val jvmArgs: List<String>
        get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")

    override val environment: Map<String, Any>
        get() = emptyMap()

    override fun run(args: List<String>) {
        if (classpath.isEmpty) {
            throw IllegalStateException("Classpath if the tool is empty: $toolName\n" +
                    "Probably the 'konan.home' project property contains an incorrect path.\n" +
                    "Please change it to the compiler root directory and rerun the build.")
        }

        project.javaexec {
            it.main = mainClass
            it.classpath = classpath
            it.jvmArgs(jvmArgs)
            it.args(args.apply {
                project.logger.info("Run tool: $toolName with args: ${this.joinToString(separator = " ")}")
            })
            it.environment(environment)
        }
    }
}

internal class KonanInteropRunner(project: Project) : KonanBaseRunner(project){
    internal companion object {
        const val INTEROP_MAIN = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
    }

    override val toolName   get() = "Kotlin/Native cinterop tool"
    override val mainClass  get() = INTEROP_MAIN

    override val environment = mutableMapOf("LIBCLANG_DISABLE_CRASH_RECOVERY" to "1").apply {
        if (project.host == "mingw") {
            put("PATH", "${project.konanHome}\\dependencies" +
                    "\\msys2-mingw-w64-x86_64-gcc-6.3.0-clang-llvm-3.9.1-windows-x86-64" +
                    "\\bin;${System.getenv("PATH")}")
        }
    }
}

internal class KonanCompilerRunner(project: Project) : KonanBaseRunner(project) {
    internal companion object {
        const val COMPILER_MAIN = "org.jetbrains.kotlin.cli.bc.K2NativeKt"
    }

    override val toolName   get() =  "Kotlin/Native compiler"
    override val mainClass  get() = COMPILER_MAIN
}
