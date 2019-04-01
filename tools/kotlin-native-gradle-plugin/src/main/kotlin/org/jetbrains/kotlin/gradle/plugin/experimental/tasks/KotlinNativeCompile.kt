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

package org.jetbrains.kotlin.gradle.plugin.experimental.tasks

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeFramework
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.AbstractKotlinNativeBinary
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

open class KotlinNativeCompile @Inject constructor(internal val binary: AbstractKotlinNativeBinary) : AbstractCompile()
{
    init {
        super.dependsOn(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)
    }

    // Inputs and outputs

    val sources: FileCollection
        @InputFiles get() = binary.sources

    override fun getSource(): FileTree = sources.asFileTree

    private val commonSources: FileCollection
        get() = binary.commonSources

    val libraries: Configuration
        @InputFiles get() = binary.klibs

    @get:InputFiles
    val exportLibraries: FileCollection
        get() = if (binary is KotlinNativeFramework) {
            binary.export
        } else {
            project.files()
        }

    override fun getClasspath(): FileCollection = libraries

    override fun setClasspath(configuration: FileCollection?) {
        throw UnsupportedOperationException("Use klibs to set compile classpath in Kotlin/Native")
    }

    val optimized:  Boolean @Input get() = binary.optimized
    val debuggable: Boolean @Input get() = binary.debuggable

    val kind: CompilerOutputKind @Input get() = binary.kind

    val target: String @Input get() = binary.konanTarget.name

    val additionalCompilerOptions: Collection<String> @Input get() = binary.additionalCompilerOptions

    val linkerOpts: List<String>
        @Input get() = binary.linkerOpts

    val entryPoint: String?
        @Optional @Input get() = binary.component.entryPoint

    val compilerPluginOptions = CompilerPluginOptions()

    var compilerPluginClasspath: FileCollection? = null

    val outputFile: File
        get() = outputLocationProvider.get().asFile

    @get:Input
    val embedBitcode: BitcodeEmbeddingMode
        get() = if (binary is KotlinNativeFramework) {
            binary.embedBitcode
        } else {
            BitcodeEmbeddingMode.DISABLE
        }

    val konanVersion: String
        @Input get() = project.konanVersion.toString(true, true)

    private val outputPathProvider: Provider<String> = project.provider {
        with(binary) {
            val root = outputRootName
            val prefix = kind.prefix(konanTarget)
            val suffix = kind.suffix(konanTarget)
            val baseName = getBaseName().get()

            var fileName = "${prefix}${baseName}${suffix}"
            if (kind == FRAMEWORK ||
                kind == STATIC ||
                kind == DYNAMIC ||
                kind == PROGRAM && konanTarget == KonanTarget.WASM32
            ) {
                fileName = fileName.replace('-', '_')
            }

            "$root/${binary.names.dirName}/$fileName"
        }
    }

    val outputLocationProvider: Provider<out FileSystemLocation> = with(project.layout) {
        if (kind == CompilerOutputKind.FRAMEWORK) {
            newOutputDirectory().apply {
                set(buildDirectory.dir(outputPathProvider))
                outputs.dir(this)
            }
        } else {
            newOutputFile().apply {
                set(buildDirectory.file(outputPathProvider))
                outputs.file(this)
            }
        }
    }

    // Initializing AbstractCompile properties.
    init {
        this.setDestinationDir(project.provider {
            if (kind == FRAMEWORK) outputFile else outputFile.parentFile
        })
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }

    // Task action.

    @TaskAction
    override fun compile() {
        outputFile.parentFile.mkdirs()

        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)
            addKey("-opt", optimized)
            addKey("-g", debuggable)
            addKey("-ea", debuggable)

            addArg("-target", target)
            addArg("-p", kind.name.toLowerCase())

            add("-Xmulti-platform")

            addArgIfNotNull("-entry", entryPoint)

            compilerPluginClasspath?.let { pluginClasspath ->
                pluginClasspath.map { it.canonicalPath }.sorted().forEach { path ->
                    add("-Xplugin=$path")
                }
                compilerPluginOptions.arguments.forEach {
                    add("-P$it")
                }
            }

            addAll(additionalCompilerOptions)

            fun Set<File>.filterKlibs() = filter {
                it.extension == "klib"
            }

            libraries.files.filterKlibs().forEach {
                addArg("-l", it.absolutePath)
            }

            // There is no need to check that all exported dependencies are passed with -l option
            // because export configuration extends the libraries one.
            exportLibraries.files.filterKlibs().forEach {
                add("-Xexport-library=${it.absolutePath}")
            }

            when (embedBitcode) {
                BitcodeEmbeddingMode.MARKER -> add("-Xembed-bitcode-marker")
                BitcodeEmbeddingMode.BITCODE -> add("-Xembed-bitcode")
                else -> { /* Do nothing. */ }
            }

            linkerOpts.forEach {
                addArg("-linker-option", it)
            }

            addAll(sources.files.map { it.absolutePath })
            commonSources.files.mapTo(this) { "-Xcommon-sources=${it.absolutePath}" }
        }

        KonanCompilerRunner(project).run(args)
    }
}
