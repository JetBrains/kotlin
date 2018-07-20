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

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KonanCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.KonanPlugin
import org.jetbrains.kotlin.gradle.plugin.addArg
import org.jetbrains.kotlin.gradle.plugin.addKey
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.AbstractKotlinNativeBinary
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import java.io.File
import javax.inject.Inject

open class KotlinNativeCompile @Inject constructor(internal val binary: AbstractKotlinNativeBinary)
    : DefaultTask()
{
    init {
        super.dependsOn(KonanPlugin.KONAN_DOWNLOAD_TASK_NAME)
    }

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

    val outputFile: File
        get() = outputLocationProvider.get().asFile

    private val outputPathProvider: Provider<String> = project.provider {
        with(binary) {
            val root = outputRootName
            val prefix = kind.prefix(konanTarget)
            val suffix = kind.suffix(konanTarget)
            val baseName = getBaseName().get()

            var fileName = "${prefix}${baseName}${suffix}"
            if (kind == CompilerOutputKind.FRAMEWORK) {
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

    // Task action

    @TaskAction
    fun compile() {
        outputFile.parentFile.mkdirs()

        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)
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
