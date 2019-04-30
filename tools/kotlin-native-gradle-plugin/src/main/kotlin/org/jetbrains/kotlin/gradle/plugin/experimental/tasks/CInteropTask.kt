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
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.cinterop.CInteropSettingsImpl
import org.jetbrains.kotlin.gradle.plugin.konan.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

open class CInteropTask @Inject constructor(val settings: CInteropSettingsImpl): DefaultTask() {

    val konanTarget: KonanTarget
        @Internal get() = settings.konanTarget

    val outputFileName: String
        @Internal get() = with(CompilerOutputKind.LIBRARY) {
            val prefix = prefix(konanTarget)
            val suffix = suffix(konanTarget)
            return "$prefix$baseName$suffix"
        }

    val outputFile: File
        get() = outputFileProvider.get().asFile

    // Inputs and outputs.

    @OutputFile
    val outputFileProvider = newOutputFile().apply {
        set { project.buildDir.resolve("cinterop/$baseName/$targetName/$outputFileName") }
    }

    val baseName: String
        @Input get() = settings.baseName

    val targetName: String
        @Input get() = konanTarget.name

    val defFile: File
        @InputFile get() = settings.defFile

    val packageName: String?
        @Optional @Input get() = settings.packageName

    val compilerOpts: List<String>
        @Input get() = settings.compilerOpts

    val linkerOpts: List<String>
        @Input get() = settings.linkerOpts

    val headers: FileCollection
        @InputFiles get() = settings.headers

    val allHeadersDirs: Set<File>
        @Input get() = settings.includeDirs.allHeadersDirs.files

    val headerFilterDirs: Set<File>
        @Input get() = settings.includeDirs.headerFilterDirs.files

    val libraries: Configuration
        @InputFiles get() = settings.dependencies.implementationDependencies

    val extraOpts: List<String>
        @Input get() = settings.extraOpts

    val konanVersion: String
        @Input get() = project.konanVersion.toString(true, true)

    // Task action.
    @TaskAction
    fun processInterop() {
        val args = mutableListOf<String>().apply {
            addArg("-o", outputFile.absolutePath)

            addArgIfNotNull("-target", konanTarget.visibleName)
            addArgIfNotNull("-def", defFile.canonicalPath)
            addArgIfNotNull("-pkg", packageName)

            addFileArgs("-header", headers)

            compilerOpts.forEach {
                addArg("-compiler-option", it)
            }

            linkerOpts.forEach {
                addArg("-linker-option", it)
            }

            addArgs("-compiler-option", allHeadersDirs.map { "-I${it.absolutePath}" })
            addArgs("-headerFilterAdditionalSearchPrefix", headerFilterDirs.map { it.absolutePath })

            libraries.files.filter {
                it.extension == "klib"
            }.forEach {
                addArg("-l", it.absolutePath)
            }

            addAll(extraOpts)
        }

        outputFile.parentFile.mkdirs()
        KonanInteropRunner(project).run(args)
    }

}