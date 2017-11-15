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

package org.jetbrains.kotlin.gradle.plugin.tasks

import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * A task executing cinterop tool with the given args and compiling the stubs produced by this tool.
 */
open class KonanInteropTask: KonanBuildingTask(), KonanInteropSpec {

    @Internal override val toolRunner: KonanToolRunner = KonanInteropRunner(project)

    override fun init(destinationDir: File, artifactName: String, target: KonanTarget) {
        super.init(destinationDir, artifactName, target)
        this.defFile = project.konanDefaultDefFile(artifactName)
    }

    // Output directories -----------------------------------------------------

    override val artifactSuffix: String
        @Internal get() = ".klib"

    // Interop stub generator parameters -------------------------------------

    @InputFile lateinit var defFile: File

    @Optional @Input var packageName: String? = null

    @Input val compilerOpts   = mutableListOf<String>()
    @Input val linkerOpts     = mutableListOf<String>()

    @InputFiles val headers   = mutableSetOf<FileCollection>()
    @InputFiles val linkFiles = mutableSetOf<FileCollection>()

    @Input val headerFilterAdditionalSearchDirectories = mutableSetOf<File>()

    override fun buildArgs() = mutableListOf<String>().apply {
        addArg("-properties", "${project.konanHome}/konan/konan.properties")

        addArg("-o", artifact.canonicalPath)

        addArgIfNotNull("-target", konanTarget.userName)
        addArgIfNotNull("-def", defFile.canonicalPath)
        addArgIfNotNull("-pkg", packageName)

        addFileArgs("-h", headers)

        compilerOpts.forEach {
            addArg("-copt", it)
        }

        val linkerOpts = mutableListOf<String>().apply { addAll(linkerOpts) }
        linkFiles.forEach {
            linkerOpts.addAll(it.files.map { it.canonicalPath })
        }
        linkerOpts.forEach {
            addArg("-lopt", it)
        }

        addArgs("-repo", libraries.repos.map { it.canonicalPath })

        addFileArgs("-library", libraries.files)
        addArgs("-library", libraries.namedKlibs)
        addArgs("-library", libraries.artifacts.map { it.artifact.canonicalPath })

        addKey("-nodefaultlibs", noDefaultLibs)

        addArgs("-headerFilterAdditionalSearchPrefix",
                headerFilterAdditionalSearchDirectories.map {
                    if (!it.isDirectory) {
                        throw InvalidUserDataException("headerFilterAdditionalSearchPrefix must be a directory: $it")
                    }
                    it.absolutePath
                }
        )

        addAll(extraOpts)
    }

    // region DSL.

    override fun defFile(file: Any) {
        defFile = project.file(file)
    }

    override fun packageName(value: String) {
        packageName = value
    }

    override fun compilerOpts(vararg values: String) {
        compilerOpts.addAll(values)
    }

    override fun header(file: Any) = headers(file)
    override fun headers(vararg files: Any) {
        headers.add(project.files(files))
    }
    override fun headers(files: FileCollection) {
        headers.add(files)
    }

    override fun includeDirs(vararg values: Any) {
        compilerOpts.addAll(values.map { "-I${project.file(it).canonicalPath}" })
    }

    override fun linkerOpts(vararg values: String) = linkerOpts(values.toList())
    override fun linkerOpts(values: List<String>) {
        linkerOpts.addAll(values)
    }

    override fun link(vararg files: Any) {
        linkFiles.add(project.files(files))
    }
    override fun link(files: FileCollection) {
        linkFiles.add(files)
    }

    override fun headerFilterAdditionalSearchPrefix(path: Any) {
        headerFilterAdditionalSearchDirectories.add(project.file(path))
    }

    override fun headerFilterAdditionalSearchPrefixes(vararg paths: Any) =
            headerFilterAdditionalSearchPrefixes(paths.toList())

    override fun headerFilterAdditionalSearchPrefixes(paths: Collection<Any>) = paths.forEach {
        headerFilterAdditionalSearchPrefix(it)
    }

    // endregion
}

