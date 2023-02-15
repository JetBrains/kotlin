/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Compiler arguments related tests")
@JvmGradlePluginTests
class CompilerArgumentsIT : KGPBaseTest() {

    @DisplayName("Classpath is included when requested")
    @GradleTest
    fun testClasspathIsIncludedWhenRequested(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Groovy
                """ 
                tasks.register("checkArgsWithClasspath") {
                        def task = project.tasks.named("compileKotlin").get()
                        def args = task.createCompilerArgs()
                        task.setupCompilerArgs(args, 
                            /* defaultsOnly=*/ false, 
                            /* ignoreClasspathResolutionErrors=*/ false, 
                            /* includeClasspath=*/ true)
                        println("$CLASSPATH_MARKER" + args.classpath)
                }
                """.trimIndent()
            )

            verifyClasspathIsPresentInOutput()

        }
    }

    @DisplayName("Classpath is included by default")
    @GradleTest
    fun testClasspathIsIncludedByDefault(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Groovy
                """ 
                tasks.register("checkArgsWithClasspath") {
                        def task = project.tasks.named("compileKotlin").get()
                        def args = task.createCompilerArgs()
                        task.setupCompilerArgs(args, 
                            /* defaultsOnly=*/ false, 
                            /* ignoreClasspathResolutionErrors=*/ false)
                        println("$CLASSPATH_MARKER" + args.classpath)
                }
                """.trimIndent()
            )

            verifyClasspathIsPresentInOutput()

        }
    }

    @DisplayName("Classpath is included by default in the Kotlin compile task")
    @GradleTest
    fun testClasspathIsIncludedByDefaultInKotlinCompileTask(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Groovy
                """ 
                tasks.register("checkArgsWithClasspath") {
                        def task = project.tasks.named("compileKotlin").get()
                        println("$CLASSPATH_MARKER" + String.join(":", task.classpath.asList()*.toString()))
                }
                """.trimIndent()

            )

            verifyClasspathIsPresentInOutput()

        }
    }

    private fun TestProject.verifyClasspathIsPresentInOutput() {
        build("checkArgsWithClasspath") {
            println(output)
            val classpath = output.lines()
                .firstOrNull { it.startsWith(CLASSPATH_MARKER) }
                ?.removePrefix(CLASSPATH_MARKER)
                ?.split(":")
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            Assertions.assertTrue(classpath.isNotEmpty())

        }
    }


    companion object {
        const val CLASSPATH_MARKER = "CLASSPATH>"
    }
}