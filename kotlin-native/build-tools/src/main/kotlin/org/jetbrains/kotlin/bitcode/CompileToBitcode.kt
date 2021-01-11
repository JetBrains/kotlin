/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.ExecClang
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

open class CompileToBitcode @Inject constructor(
        val srcRoot: File,
        val folderName: String,
        val target: String,
        val outputGroup: String
) : DefaultTask() {

    enum class Language {
        C, CPP
    }

    // Compiler args are part of compilerFlags so we don't register them as an input.
    val compilerArgs = mutableListOf<String>()
    @Input
    val linkerArgs = mutableListOf<String>()
    var excludeFiles: List<String> = listOf(
            "**/*Test.cpp",
            "**/*Test.mm",
    )
    var includeFiles: List<String> = listOf(
            "**/*.cpp",
            "**/*.mm"
    )

    // Source files and headers are registered as inputs by the `inputFiles` and `headers` properties.
    var srcDirs: FileCollection = project.files(srcRoot.resolve("cpp"))
    var headersDirs: FileCollection = project.files(srcRoot.resolve("headers"))

    @Input
    var language = Language.CPP

    private val targetDir by lazy { project.buildDir.resolve("bitcode/$outputGroup/$target") }

    val objDir by lazy { File(targetDir, folderName) }

    private val KonanTarget.isMINGW
        get() = this.family == Family.MINGW

    val executable
        get() = when (language) {
            Language.C -> "clang"
            Language.CPP -> "clang++"
        }

    @get:Input
    val compilerFlags: List<String>
        get() {
            val commonFlags = listOf("-c", "-emit-llvm") + headersDirs.map { "-I$it" }
            val languageFlags = when (language) {
                Language.C ->
                    // Used flags provided by original build of allocator C code.
                    listOf("-std=gnu11", "-O3", "-Wall", "-Wextra", "-Werror")
                Language.CPP ->
                    listOfNotNull("-std=c++14", "-Werror", "-O2",
                            "-Wall", "-Wextra",
                            "-Wno-unused-parameter",  // False positives with polymorphic functions.
                            "-fPIC".takeIf { !HostManager().targetByName(target).isMINGW })
            }
            return commonFlags + languageFlags + compilerArgs
        }

    @get:SkipWhenEmpty
    @get:InputFiles
    val inputFiles: Iterable<File>
        get() {
            return srcDirs.flatMap { srcDir ->
                project.fileTree(srcDir) {
                    it.include(includeFiles)
                    it.exclude(excludeFiles)
                }.files
            }
        }

    private fun outputFileForInputFile(file: File, extension: String) = objDir.resolve("${file.nameWithoutExtension}.${extension}")
    private fun bitcodeFileForInputFile(file: File) = outputFileForInputFile(file, "bc")

    @get:InputFiles
    protected val headers: Iterable<File>
        get() {
            // Not using clang's -M* flags because there's a problem with our current include system:
            // We allow includes relative to the current directory and also pass -I for each imported module
            // Given file tree:
            // a:
            //  header.hpp
            // b:
            //  impl.cpp
            // Assume module b adds a to its include path.
            // If b/impl.cpp has #include "header.hpp", it'll be included from a/header.hpp. If we add another file
            // header.hpp into b/, the next compilation of b/impl.cpp will include b/header.hpp. -M flags, however,
            // won't generate a dependency on b/header.hpp, so incremental compilation will be broken.
            // TODO: Apart from dependency generation this also makes it awkward to have two files with
            //       the same name (e.g. Utils.h) in directories a/ and b/: For the b/impl.cpp to include a/header.hpp
            //       it needs to have #include "../a/header.hpp"

            val dirs = mutableSetOf<File>()
            // First add dirs with sources, as clang by default adds directory with the source to the include path.
            inputFiles.forEach {
                dirs.add(it.parentFile)
            }
            // Now add manually given header dirs.
            dirs.addAll(headersDirs.files)
            return dirs.flatMap { dir ->
                project.fileTree(dir) {
                    val includePatterns = when (language) {
                        Language.C -> arrayOf("**/.h")
                        Language.CPP -> arrayOf("**/*.h", "**/*.hpp")
                    }
                    it.include(*includePatterns)
                }.files
            }
        }

    @OutputFile
    val outFile = File(targetDir, "${folderName}.bc")

    @TaskAction
    fun compile() {
        objDir.mkdirs()
        val plugin = project.convention.getPlugin(ExecClang::class.java)

        plugin.execKonanClang(target) {
            it.workingDir = objDir
            it.executable = executable
            it.args = compilerFlags + inputFiles.map { it.absolutePath }
        }

        project.exec {
            val llvmDir = project.findProperty("llvmDir")
            it.executable = "$llvmDir/bin/llvm-link"
            it.args = listOf("-o", outFile.absolutePath) + linkerArgs +
                    inputFiles.map {
                        bitcodeFileForInputFile(it).absolutePath
                    }
        }
    }
}
