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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KonanInteropLibrary
import org.jetbrains.kotlin.gradle.plugin.KonanLibrary
import org.jetbrains.kotlin.gradle.plugin.KonanProgram
import org.jetbrains.kotlin.gradle.plugin.konanArtifactsContainer
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

open class KonanGenerateCMakeTask : DefaultTask() {
    @Suppress("unused")
    @TaskAction
    fun generateCMake() {
        val interops = project.konanArtifactsContainer.toList().filterIsInstance<KonanInteropLibrary>()
        val libraries = project.konanArtifactsContainer.toList().filterIsInstance<KonanLibrary>()
        val programs = project.konanArtifactsContainer.toList().filterIsInstance<KonanProgram>()
        val cMakeLists = generateCMakeLists(
                project.name,
                interops,
                libraries,
                programs
        )
        File(project.projectDir, "CMakeLists.txt")
                .writeText(cMakeLists)

        // This directory is filled out by the IDE
        File(project.projectDir, "KotlinCMakeModule")
                .mkdir()
    }

    private val host = HostManager.host

    private fun generateCMakeLists(
            projectName: String,
            interops: List<KonanInteropLibrary>,
            libraries: List<KonanLibrary>,
            programs: List<KonanProgram>
    ): String {
        val cMakeCurrentListDir = "$" + "{CMAKE_CURRENT_LIST_DIR}"

        return buildString {
            appendln("""
                cmake_minimum_required(VERSION 3.8)

                set(CMAKE_MODULE_PATH $cMakeCurrentListDir/KotlinCMakeModule)

                project($projectName Kotlin)
            """.trimIndent())
            appendln()

            for (interop in interops) {
                val task = interop[host] ?: continue
                appendln(
                        Call("cinterop")
                                .arg("NAME", interop.name)
                                .arg("DEF_FILE", task.defFile.relativePath.toString().crossPlatformPath)
                                .arg("COMPILER_OPTS", task.cMakeCompilerOpts)
                )
            }

            for (library in libraries) {
                val task = library[host] ?: continue
                appendln(
                        Call("konanc_library")
                                .arg("NAME", library.name)
                                .arg("SOURCES", task.cMakeSources)
                                .arg("LIBRARIES", task.cMakeLibraries)
                                .arg("LINKER_OPTS", task.cMakeLinkerOpts))
            }

            for (program in programs) {
                val task = program[host] ?: continue
                appendln(
                        Call("konanc_executable")
                                .arg("NAME", program.name)
                                .arg("SOURCES", task.cMakeSources)
                                .arg("LIBRARIES", task.cMakeLibraries)
                                .arg("LINKER_OPTS", task.cMakeLinkerOpts))
            }
        }
    }

    private val File.relativePath get() = relativeTo(project.projectDir)

    private val String.crossPlatformPath get() =
        if (host.family == Family.MINGW) replace('\\', '/') else this

    private val FileCollection.asCMakeSourceList: List<String>
        get() = files.map { it.relativePath.toString().crossPlatformPath }

    private val KonanInteropTask.cMakeCompilerOpts: String
        get() = (compilerOpts + includeDirs.allHeadersDirs.map { "-I${it.absolutePath.crossPlatformPath}" })
                .joinToString(" ")

    private val KonanCompileTask.cMakeSources: String
        get() = srcFiles.flatMap { it.asCMakeSourceList }.joinToString(" ")

    private val KonanCompileTask.cMakeLibraries: String
        get() = mutableListOf<String>().apply {
            addAll(libraries.artifacts.map { it.artifactName })
            addAll(libraries.namedKlibs)
            addAll(libraries.files.flatMap { it.files }.map { it.canonicalPath.crossPlatformPath })
        }.joinToString(" ")

    private val KonanCompileTask.cMakeLinkerOpts: String
        get() = linkerOpts.joinToString(" ")
}

private class Call(val name: String) {
    private val args: MutableList<Pair<String, String>> = mutableListOf()

    fun arg(key: String, value: String?): Call {
        if (value != null && value.isNotBlank()) args += key to value
        return this
    }

    override fun toString(): String =
            buildString {
                append(name)
                append("(")
                for ((key, value) in args) {
                    appendln()
                    append("    $key $value")
                }
                appendln(")")
            }
}
