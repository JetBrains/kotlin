/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@MppGradlePluginTests
class MppCompilerPluginsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testMppBuildWithCompilerPlugins(gradleVersion: GradleVersion) {

        val printOptionsTaskName = "printCompilerPluginOptions"
        val argsMarker = "=args=>"
        val classpathMarker = "=cp=>"
        val compilerPluginArgsRegex = "(\\w+)${Regex.escape(argsMarker)}(.*)".toRegex()
        val compilerPluginClasspathRegex = "(\\w+)${Regex.escape(classpathMarker)}(.*)".toRegex()

        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {

            buildGradle.append(
                """
                buildscript {
                    dependencies {
                        classpath "org.jetbrains.kotlin:kotlin-allopen:${'$'}kotlin_version"
                        classpath "org.jetbrains.kotlin:kotlin-noarg:${'$'}kotlin_version"
                    }
                }
                apply plugin: 'kotlin-allopen'
                apply plugin: 'kotlin-noarg'
    
                allOpen { annotation 'com.example.Annotation' }
                noArg { annotation 'com.example.Annotation' }

                task $printOptionsTaskName {
                    // if the tasks are not configured during evaluation phase, configuring them during execution
                    // leads to new dependencies unsuccessfully added to the resolved compilerPluginsClasspath configuration
                    kotlin.targets.all { compilations.all { /*force to configure the*/ compileTaskProvider.get() } }
                    doFirst {
                        kotlin.sourceSets.each { sourceSet ->
                            def args = sourceSet.languageSettings.compilerPluginArguments
                            def cp = sourceSet.languageSettings.compilerPluginClasspath.files
                            println sourceSet.name + '$argsMarker' + args
                            println sourceSet.name + '$classpathMarker' + cp
                        }
                    }
                }
                """.trimIndent()
            )

            projectPath.resolve("src/commonMain/kotlin/Annotation.kt").writeText(
                """
                package com.example
                annotation class Annotation
                """.trimIndent()
            )
            projectPath.resolve("src/commonMain/kotlin/Annotated.kt").writeText(
                """
                package com.example
                @Annotation
                open class Annotated(var y: Int) { var x = 2 }
                """.trimIndent()
            )
            // TODO KT-27683 once Kotlin/Native properly supports compiler plugins, move this class to src/commonMain
            listOf("jvm6", "nodeJs").forEach {
                projectPath.resolve("src/${it}Main/kotlin/Override.kt").writeText(
                    """
                    package com.example

                    @Annotation
                    class Override : Annotated(0) {
                        override var x = 3
                    }
                    """.trimIndent()
                )
            }

            // Do not use embeddable compiler in Kotlin/Native, otherwise it would effectively enable allopen & noarg plugins for Native, and
            // we'd be testing that the latest versions of allopen/noarg work with the fixed version of Kotlin/Native (defined in the root
            // build.gradle.kts), which is generally not guaranteed.
            build("assemble", "-Pkotlin.native.useEmbeddableCompilerJar=false", printOptionsTaskName) {
                assertTasksExecuted(
                    ":compileKotlinJvm6",
                    ":compileKotlinNodeJs",
                    ":compileKotlinLinux64",
                )

                assertFileInProjectExists("build/classes/kotlin/jvm6/main/com/example/Annotated.class")
                assertFileInProjectExists("build/classes/kotlin/jvm6/main/com/example/Override.class")

                val (compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet) =
                    listOf(compilerPluginArgsRegex, compilerPluginClasspathRegex)
                        .map { marker ->
                            marker.findAll(output).associate { it.groupValues[1] to it.groupValues[2] }
                        }

                // TODO KT-27683 once Kotlin/Native properly supports compiler plugins, delete excludedSourceSets:
                val allSourceSets = projectPath.resolve("src").listDirectoryEntries().map { it.name }
                val excludedSourceSets = setOf("linux64Main", "macos64Main", "macosArm64Main", "mingw64Main", "nativeMain")

                val expectedArgs = listOf(
                    "plugin:org.jetbrains.kotlin.allopen:annotation=com.example.Annotation",
                    "plugin:org.jetbrains.kotlin.noarg:annotation=com.example.Annotation",
                ).joinToString(", ", prefix = "[", postfix = "]")

                allSourceSets
                    .filter { it !in excludedSourceSets }
                    .forEach { sourceSet ->
                        val actualPluginArgs = compilerPluginArgsBySourceSet[sourceSet]
                        assertEquals(expectedArgs, actualPluginArgs, "Expected $expectedArgs as plugin args for $sourceSet")

                        val actualPluginClasspath = compilerPluginClasspathBySourceSet[sourceSet]
                        assertNotNull(
                            actualPluginClasspath,
                            "Could not find compiler plugin classpath for $sourceSet (all available sourceSets: ${compilerPluginClasspathBySourceSet.keys}",
                        )
                        assertContains(actualPluginClasspath, "kotlin-allopen")
                        assertContains(actualPluginClasspath, "kotlin-noarg")
                    }
            }
        }
    }
}
