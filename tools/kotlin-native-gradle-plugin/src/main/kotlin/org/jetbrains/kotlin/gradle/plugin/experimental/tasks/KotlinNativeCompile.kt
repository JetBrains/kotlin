package org.jetbrains.kotlin.gradle.plugin.experimental.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KonanCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.KonanPlugin
import org.jetbrains.kotlin.gradle.plugin.addArg
import org.jetbrains.kotlin.gradle.plugin.addKey
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.AbstractKotlinNativeBinary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

open class KotlinNativeCompile: DefaultTask() {

    init {
        super.dependsOn(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)
    }

    // TODO: May be replace with Gradle's property
    internal lateinit var binary: AbstractKotlinNativeBinary

    // Inputs and outputs

    val sources: FileCollection
        @InputFiles get() = binary.sources

    val libraries: Configuration
        @InputFiles get() = binary.klibraries

    val optimized:  Boolean @Input get() = binary.optimized
    val debuggable: Boolean @Input get() = binary.debuggable

    val kind: CompilerOutputKind @Input get() = binary.kind

    val target: String @Input get() = binary.konanTarget.name

    val additionalCompilerOptions: Collection<String> @Input get() = binary.additionalCompilerOptions

    @OutputFile
    val outputFile: RegularFileProperty = newOutputFile()

    // Task action

    @TaskAction
    fun compile() {
        val output = outputFile.asFile.get()
        output.parentFile.mkdirs()

        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.get().asFile.absolutePath)
            addKey("-opt", optimized)
            addKey("-g", debuggable)
            addKey("-ea", debuggable)

            addArg("-target", target)
            addArg("-p", kind.name.toLowerCase())

            add("-Xmulti-platform")

            addAll(additionalCompilerOptions)

            libraries.files.forEach {library ->
                library.parent?.let { addArg("-r", it) }
                addArg("-l", library.nameWithoutExtension)
            }

            addAll(sources.files.map { it.absolutePath })
        }

        KonanCompilerRunner(project).run(args)
    }
}
