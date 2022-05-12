/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.ExecClang
import java.io.File
import javax.inject.Inject
import kotlinBuildProperties
import isNativeRuntimeDebugInfoEnabled
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.target.*

interface CompileToBitcodeParameters : WorkParameters {
    var objDir: File
    var target: String
    var compilerExecutable: String
    var compilerArgs: List<String>
    var llvmLinkArgs: List<String>

    var konanHome: File
    var llvmDir: File
    var experimentalDistribution: Boolean
}

abstract class CompileToBitcodeJob : WorkAction<CompileToBitcodeParameters> {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        with(parameters) {
            objDir.mkdirs()

            val platformManager = PlatformManager(buildDistribution(konanHome.absolutePath), experimentalDistribution)
            val execClang = ExecClang.create(objects, platformManager, llvmDir)

            execClang.execKonanClang(target) {
                workingDir = objDir
                executable = compilerExecutable
                args = compilerArgs
            }

            execOperations.exec {
                executable = "${llvmDir.absolutePath}/bin/llvm-link"
                args = llvmLinkArgs
            }
        }
    }
}

abstract class CompileToBitcode @Inject constructor(
        @Input val folderName: String,
        @Input val target: String,
        @Input val outputGroup: String
) : DefaultTask() {

    enum class Language {
        C, CPP
    }

    // Compiler args are part of compilerFlags so we don't register them as an input.
    @Internal
    val compilerArgs = mutableListOf<String>()
    @Input
    val linkerArgs = mutableListOf<String>()
    @Input
    var excludeFiles: List<String> = listOf(
            "**/*Test.cpp",
            "**/*TestSupport.cpp",
            "**/*Test.mm",
            "**/*TestSupport.mm"
    )
    @Input
    var includeFiles: List<String> = listOf(
            "**/*.cpp",
            "**/*.mm"
    )

    // Source files and headers are registered as inputs by the `inputFiles` and `headers` properties.
    @Internal
    var srcDirs: FileCollection = project.files()
    @Internal
    var headersDirs: FileCollection = project.files()

    @Input
    var language = Language.CPP

    @Input @Optional
    var sanitizer: SanitizerKind? = null

    @Input @Optional
    val extraSanitizerArgs = mutableMapOf<SanitizerKind, List<String>>()

    private val targetDir: File
        get() {
            val sanitizerSuffix = when (sanitizer) {
                null -> ""
                SanitizerKind.ADDRESS -> "-asan"
                SanitizerKind.THREAD -> "-tsan"
            }
            return project.buildDir.resolve("bitcode/$outputGroup/$target$sanitizerSuffix")
        }

    @get:Internal
    val objDir
        get() = File(targetDir, folderName)

    private val KonanTarget.isMINGW
        get() = this.family == Family.MINGW

    @get:Internal
    val executable
        get() = when (language) {
            Language.C -> "clang"
            Language.CPP -> "clang++"
        }

    @get:Input
    val compilerFlags: List<String>
        get() {
            val commonFlags = listOfNotNull(
                    "-gdwarf-2".takeIf { project.kotlinBuildProperties.isNativeRuntimeDebugInfoEnabled },
                    "-c", "-emit-llvm") + headersDirs.map { "-I$it" }
            val sanitizerFlags = when (sanitizer) {
                null -> listOf()
                SanitizerKind.ADDRESS -> listOf("-fsanitize=address")
                SanitizerKind.THREAD -> listOf("-fsanitize=thread")
            } + (extraSanitizerArgs[sanitizer] ?: emptyList())
            val languageFlags = when (language) {
                Language.C -> {
                    listOf("-std=gnu11", "-Wall", "-Wextra", "-Werror") +
                    if (sanitizer != SanitizerKind.THREAD) {
                        // Used flags provided by original build of allocator C code.
                        listOf("-O3")
                    } else {
                        // Building with TSAN needs turning off extra optimizations.
                        listOf("-O1")
                    }
                }
                Language.CPP ->
                    listOfNotNull("-std=c++17", "-Werror", "-O2",
                            "-fno-aligned-allocation", // TODO: Remove when all targets support aligned allocation in C++ runtime.
                            "-Wall", "-Wextra",
                            "-Wno-unused-parameter"  // False positives with polymorphic functions.
                    )
            }
            return commonFlags + sanitizerFlags + languageFlags + compilerArgs
        }

    @get:SkipWhenEmpty
    @get:InputFiles
    val inputFiles: Iterable<File>
        get() {
            return srcDirs.flatMap { srcDir ->
                project.fileTree(srcDir) {
                    include(includeFiles)
                    exclude(excludeFiles)
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
                    include(*includePatterns)
                }.files
            }
        }

    @Input
    var outputName = "${folderName}.bc"

    @get:OutputFile
    val outFile: File
        get() = File(targetDir, outputName)

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @TaskAction
    fun compile() {
        val workQueue = workerExecutor.noIsolation()

        val parameters = { it: CompileToBitcodeParameters ->
            it.objDir = objDir
            it.target = target
            it.compilerExecutable = executable
            it.compilerArgs = compilerFlags + inputFiles.map { it.absolutePath }
            it.llvmLinkArgs = listOf("-o", outFile.absolutePath) + linkerArgs +
                    inputFiles.map {
                        bitcodeFileForInputFile(it).absolutePath
                    }

            it.konanHome = project.project(":kotlin-native").projectDir
            it.llvmDir = project.file(project.findProperty("llvmDir")!!)
        }

        workQueue.submit(CompileToBitcodeJob::class.java, parameters)
    }
}
