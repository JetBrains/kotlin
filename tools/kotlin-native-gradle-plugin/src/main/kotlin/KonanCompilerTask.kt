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

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

// TODO: form groups for tasks
// TODO: Make the task class nested for config with properties accessible for outer users.
open class KonanCompileTask: DefaultTask() {

    companion object {
        const val COMPILER_MAIN = "org.jetbrains.kotlin.cli.bc.K2NativeKt"
    }

    val COMPILER_JVM_ARGS: List<String>
        get() = listOf("-Dkonan.home=${project.konanHome}", "-Djava.library.path=${project.konanHome}/konan/nativelib")
    val COMPILER_CLASSPATH: String
        get() = "${project.konanHome}/konan/lib/"

    // Output artifact --------------------------------------------------------

    protected lateinit var artifactName: String

    @OutputDirectory
    lateinit var outputDir: File
        internal set

    internal fun initialize(artifactName: String) {
        dependsOn(project.konanCompilerDownloadTask)
        this.artifactName = artifactName
        outputDir = project.file("${project.konanCompilerOutputDir}/$artifactName")
    }

    val artifactPath: String
        get() = "${outputDir.absolutePath}/$artifactName.${ if (noLink) "bc" else "kexe" }"

    // Other compilation parameters -------------------------------------------

    @InputFiles val inputFiles      = mutableSetOf<FileCollection>()

    @InputFiles val libraries       = mutableSetOf<FileCollection>()
    @InputFiles val nativeLibraries = mutableSetOf<FileCollection>()

    @Input var linkerOpts = mutableListOf<String>()
        internal set

    @Input var noStdLib           = false
        internal set
    @Input var noLink             = false
        internal set
    @Input var noMain             = false
        internal set
    @Input var enableOptimization = false
        internal set
    @Input var enableAssertions   = false
        internal set

    @Optional @Input var target          : String? = null
        internal set
    @Optional @Input var languageVersion : String? = null
        internal set
    @Optional @Input var apiVersion      : String? = null
        internal set

    // TODO: Is there a better way to rerun tasks when the compiler version changes?
    @Input val konanVersion = project.konanVersion

    // Task action ------------------------------------------------------------

    protected fun buildArgs() = mutableListOf<String>().apply {
        addArg("-output", artifactPath)

        addFileArgs("-library", libraries)
        addFileArgs("-nativelibrary", nativeLibraries)

        addListArg("-linkerArgs", linkerOpts)

        addArgIfNotNull("-target", target)
        addArgIfNotNull("-language-version", languageVersion)
        addArgIfNotNull("-api-version", apiVersion)

        addKey("-nostdlib", noStdLib)
        addKey("-nolink", noLink)
        addKey("-nomain", noMain)
        addKey("-opt", enableOptimization)
        addKey("-ea", enableAssertions)

        inputFiles.forEach {
            it.files.filter { it.name.endsWith(".kt") }.mapTo(this) { it.canonicalPath }
        }
    }

    @TaskAction
    fun compile() {
        project.file(outputDir).mkdirs()

        // TODO: Use compiler service.
        project.javaexec {
            with(it) {
                main = COMPILER_MAIN
                classpath = project.fileTree(COMPILER_CLASSPATH).apply { include("*.jar") }
                jvmArgs(COMPILER_JVM_ARGS)
                args(buildArgs().apply { logger.info("Compiler args: ${this.joinToString(separator = " ")}") })
            }
        }
    }

}
