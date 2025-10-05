/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.sourceSets

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertFileInProjectExists
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.source
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalKotlinGradlePluginApi::class)
@DisplayName("Kotlin: generated sources")
class GeneratedSourcesIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @GradleTest
    @DisplayName("JVM: sources are added into compilation")
    fun addedIntoCompilation(gradleVersion: GradleVersion) {
        project("base-kotlin-jvm-library", gradleVersion) {
            buildScriptInjection {
                val generatorTask = project.tasks.register("generator") {
                    val outputDirectory = it.project.layout.projectDirectory.dir("src/main/gen")
                    it.outputs.dir(outputDirectory)
                    it.doLast {
                        outputDirectory.file("BaseClass.kt").asFile.writeText(
                            //language=kotlin
                            """
                            abstract class BaseClass {
                                 fun printHello() {
                                     println("hello")
                                 }
                            }
                            """.trimIndent()
                        )
                    }
                }
                kotlinJvm.sourceSets.getByName("main").generatedKotlin.srcDir(generatorTask)
            }

            kotlinSourcesDir().source("main.kt") {
                //language=kotlin
                """
                class FinalClass : BaseClass()
                
                fun main() {
                     FinalClass().printHello()
                }
                """.trimIndent()
            }

            build("compileKotlin") {
                assertTasksExecuted(":generator")
            }
        }
    }

    @JvmGradlePluginTests
    @GradleTest
    @DisplayName("JVM: generator task could analyze non-generated sources")
    fun generatorTaskAnalyzeSources(gradleVersion: GradleVersion) {
        project(
            "base-kotlin-jvm-library",
            gradleVersion,
        ) {
            buildScriptInjection {
                abstract class GeneratorTask : DefaultTask() {
                    @get:InputFiles
                    abstract val sourcesToAnalyze: ConfigurableFileCollection

                    @get:OutputDirectory
                    abstract val outputDirectory: DirectoryProperty

                    @TaskAction
                    fun generateSources() {
                        sourcesToAnalyze.asFileTree.files.forEach { sourceFile ->
                            outputDirectory.get().file("${sourceFile.nameWithoutExtension}_generated.kt").asFile.writeText(
                                //language=kotlin
                                """
                                fun printOne${sourceFile.nameWithoutExtension}Test() {
                                    println("one - ${sourceFile.nameWithoutExtension}")
                                }
                                """.trimIndent()
                            )
                        }
                    }
                }

                val generatorTask = project.tasks.register("generator", GeneratorTask::class.java) {
                    val mainKotlinSourceSet = kotlinJvm.sourceSets.getByName("main")
                    it.sourcesToAnalyze.from(mainKotlinSourceSet.kotlin)
                    it.outputDirectory.set(it.project.layout.projectDirectory.dir("src/main/gen"))
                }
                kotlinJvm.sourceSets.getByName("main").generatedKotlin.srcDir(generatorTask.flatMap { it.outputDirectory })
            }

            kotlinSourcesDir().source("main.kt") {
                //language=kotlin
                """
                fun main() {
                     printOnemainTest()
                }
                """.trimIndent()
            }

            build("compileKotlin") {
                assertTasksExecuted(":generator")
                assertFileInProjectExists("src/main/gen/main_generated.kt")
            }
        }
    }

    @MppGradlePluginTests
    @GradleTest
    @DisplayName("KMP: generator task could analyze non-generated common sources")
    fun kmpGeneratedSourcesAreAddedIntoCompilation(gradleVersion: GradleVersion) {
        project("base-kotlin-multiplatform-library", gradleVersion) {
            buildScriptInjection {
                abstract class GeneratorTask : DefaultTask() {
                    @get:InputFiles
                    abstract val sourcesToAnalyze: ConfigurableFileCollection

                    @get:OutputDirectory
                    abstract val outputDirectory: DirectoryProperty

                    @TaskAction
                    fun generateSources() {
                        sourcesToAnalyze.asFileTree.files.forEach { sourceFile ->
                            outputDirectory.get().file("${sourceFile.nameWithoutExtension}_generated.kt").asFile.writeText(
                                //language=kotlin
                                """
                                fun printOne${sourceFile.nameWithoutExtension}Test() {
                                    println("one - ${sourceFile.nameWithoutExtension}")
                                }
                                """.trimIndent()
                            )
                        }
                    }
                }

                kotlinMultiplatform.jvm()
                kotlinMultiplatform.linuxX64()

                val commonMainKotlinSourceSet = kotlinMultiplatform.sourceSets.getByName("commonMain")
                val generatorTask = project.tasks.register("generator", GeneratorTask::class.java) {
                    it.sourcesToAnalyze.from(commonMainKotlinSourceSet.kotlin)
                    it.outputDirectory.set(it.project.layout.projectDirectory.dir("src/commonMainGen/kotlin"))
                }
                commonMainKotlinSourceSet.generatedKotlin.srcDir(generatorTask.flatMap { it.outputDirectory })
            }

            kotlinSourcesDir("commonMain").source("shared.kt") {
                //language=kotlin
                """
                fun printHelloOne() {
                     println("Hello,")
                     printOnesharedTest()
                }
                """.trimIndent()
            }
            kotlinSourcesDir("jvmMain").source("main.kt") {
                //language=kotlin
                """
                fun main() {
                    printHelloOne()
                    printOnesharedTest()
                }
                """.trimIndent()
            }

            build("compileKotlinJvm") {
                assertTasksExecuted(":generator")
                assertFileInProjectExists("src/commonMainGen/kotlin/shared_generated.kt")
            }
        }
    }
}