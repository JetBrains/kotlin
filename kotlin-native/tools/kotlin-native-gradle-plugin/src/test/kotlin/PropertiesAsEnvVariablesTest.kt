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

package org.jetbrains.kotlin.gradle.plugin.test

import org.jetbrains.kotlin.gradle.plugin.test.KonanProject.escapeBackSlashes
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.tools4j.spockito.Spockito
import java.io.File
import kotlin.test.Test

@RunWith(Spockito::class)
open class PropertiesAsEnvVariablesTest {

    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    private fun artifactFileName(baseName: String, type: ArtifactType, target: KonanTarget = HostManager.host): String {
        var suffix = ""
        var prefix = ""
        when (type) {
            ArtifactType.PROGRAM -> suffix = target.family.exeSuffix
            ArtifactType.INTEROP,
            ArtifactType.LIBRARY -> suffix = "klib"
            ArtifactType.BITCODE -> suffix = "bc"
            ArtifactType.FRAMEWORK -> suffix = "framework"
            ArtifactType.DYNAMIC -> {
                prefix = target.family.dynamicPrefix
                suffix = target.family.dynamicSuffix
            }
            ArtifactType.STATIC -> {
                prefix = target.family.staticPrefix
                suffix = target.family.staticSuffix
            }

        }
        return "$prefix${baseName}.$suffix"
    }

    private fun assertFileExists(directory: File, filename: String) = assert(directory.list().contains(filename)) {
        "No such file: $filename in directory: ${directory.absolutePath}"
    }

    @Test
    @Spockito.Unroll(
        "|property                   |value |assertion               |message                  |",
        "|konan.debugging.symbols    |YES   |it.enableDebug          |Debug should be enabled  |",
        "|konan.debugging.symbols    |true  |it.enableDebug          |Debug should be enabled  |",
        "|konan.debugging.symbols    |NO    |!it.enableDebug         |Debug should be disabled |",
        "|konan.debugging.symbols    |false |!it.enableDebug         |Debug should be disabled |",
        "|konan.optimizations.enable |YES   |it.enableOptimizations  |Opts should be enabled   |",
        "|konan.optimizations.enable |true  |it.enableOptimizations  |Opts should be enabled   |",
        "|konan.optimizations.enable |NO    |!it.enableOptimizations |Opts should be disabled  |",
        "|konan.optimizations.enable |false |!it.enableOptimizations |Opts should be disabled  |"
    )
    @Spockito.Name("[{row}]: {variable}={value}")
    fun `Plugin should support enabling and disabling debug and opt options via a project property`(
            property: String,
            value: String,
            assertion: String,
            message: String
    ) {
        val project = KonanProject.createEmpty(projectDirectory)
        project.buildFile.appendText("""

            apply plugin: 'konan'
            konanArtifacts {
                library('main')
            }

            task assertEnableDebug {
                doLast {
                    konanArtifacts.main.forEach {
                        if (!($assertion)) throw new AssertionError("$message for ${'$'}it.name")
                    }
                }
            }
        """.trimIndent())
        project.createRunner()
                .withArguments("assertEnableDebug", "-P${property}=${value}")
                .build()
    }

    @Test
    fun `Plugin should support setting destination directory via a project property`() {
        val project = KonanProject.createEmpty(projectDirectory)
        val newDestinationDir = project.createSubDir("newDestination")
        val newDestinationPath = newDestinationDir.absolutePath
        project.buildFile.appendText("""
            apply plugin: 'konan'
            konanArtifacts {
                program('program')
                library('library')
                dynamic('dynamic')
                framework('framework')
            }

            task assertDestinationDir {
                doLast {
                    konanArtifacts.forEach { artifact ->
                        artifact.forEach {
                            if (it.destinationDir.absolutePath != '${escapeBackSlashes(newDestinationPath)}'){
                                throw new AssertionError("Unexpected destination dir for ${'$'}it.name\\n" +
                                                         "expected: ${escapeBackSlashes(newDestinationPath)}\\n" +
                                                         "actual: ${'$'}it.destinationDir")
                            }
                        }
                    }
                }
            }
        """.trimIndent())
        project.generateSrcFile("main.kt")
        project.createRunner()
                .withArguments("assertDestinationDir", "build", "-Pkonan.configuration.build.dir=$newDestinationPath")
                .build()

        assertFileExists(newDestinationDir, artifactFileName("program", ArtifactType.PROGRAM))
        assertFileExists(newDestinationDir, artifactFileName("library", ArtifactType.LIBRARY))
        assertFileExists(newDestinationDir, artifactFileName("dynamic", ArtifactType.DYNAMIC))
        if (HostManager.hostIsMac) {
            assertFileExists(newDestinationDir, artifactFileName("framework", ArtifactType.FRAMEWORK))
        }
    }

    @Test
    fun `Plugin should rerun tasks if konan_configuration_build_dir has been changed`() {
        val project = KonanProject.createEmpty(projectDirectory)
        val destination1 = project.createSubDir("destination1", "subdir")
        val destination2 = project.createSubDir("destination2", "subdir")

        project.buildFile.appendText("""
            apply plugin: 'konan'
            konanArtifacts {
                library('main')
            }
        """.trimIndent())
        project.generateSrcFile("main.kt")

        project.createRunner()
                .withArguments("build", "-Pkonan.configuration.build.dir=${destination1.absolutePath}")
                .build()
        project.createRunner()
                .withArguments("build", "-Pkonan.configuration.build.dir=${destination2.absolutePath}")
                .build()

        assertFileExists(destination1, artifactFileName("main", ArtifactType.LIBRARY))
        assertFileExists(destination2, artifactFileName("main", ArtifactType.LIBRARY))
    }

    @Test
    fun `Up-to-date checks should work with different directories for different targets`() {
        val project = KonanProject.createEmpty(projectDirectory)
        val fooDir = project.createSubDir("foo")
        val barDir = project.createSubDir("bar")
        project.buildFile.appendText("""
            apply plugin: 'konan'

            konanArtifacts {
                library('foo')
                library('bar')
            }

            task assertUpToDate {
                dependsOn 'compileKonanFoo'
                doLast {
                    if (!konanArtifacts.foo.getByTarget('host').state.upToDate) {
                        throw new AssertionError("Compilation task is not up-to-date")
                    }
                }
            }
        """.trimIndent())
        project.generateSrcFile("main.kt")

        project.createRunner()
                .withArguments("compileKonanFoo", "-Pkonan.configuration.build.dir=${fooDir.absolutePath}")
                .build()
        project.createRunner()
                .withArguments("compileKonanBar", "-Pkonan.configuration.build.dir=${barDir.absolutePath}")
                .build()
        project.createRunner()
                .withArguments("assertUpToDate", "-Pkonan.configuration.build.dir=${fooDir.absolutePath}")
                .build()
    }
}