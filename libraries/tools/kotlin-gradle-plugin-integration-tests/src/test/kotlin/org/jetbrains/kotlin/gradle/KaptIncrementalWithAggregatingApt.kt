/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse

class KaptIncrementalWithAggregatingApt : KaptIncrementalIT() {

    override fun getProject() =
        Project(
            "kaptIncrementalCompilationProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject("AGGREGATING")
        }

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            incremental = true,
            kaptOptions = KaptOptions(
                verbose = true,
                useWorkers = true,
                incrementalKapt = true,
                includeCompileClasspath = false
            )
        )

    @Test
    fun testIncrementalChanges() {
        val project = getProject()

        project.build("clean", "build") {
            assertSuccessful()
        }

        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
        project.build("build") {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/UtilKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/foo/A.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ), getProcessedSources(output)
            )
        }

        project.projectFile("JavaClass.java").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "private void anotherFun() {}\n }"
        }
        project.build("build") {
            assertSuccessful()
            assertEquals(
                setOf(
                    project.projectFile("JavaClass.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/UtilKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/foo/A.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ),
                getProcessedSources(output)
            )
        }
    }

    @Test
    fun testIncrementalChangesWithJdk9() {
        val javaHome = File(System.getProperty("jdk9Home")!!)
        Assume.assumeTrue("JDK 9 isn't available", javaHome.isDirectory)
        val options = defaultBuildOptions().copy(javaHome = javaHome)
        val project = getProject()

        project.build("clean", "build", options = options) {
            assertSuccessful()
        }

        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
        project.build("build", options = options) {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/UtilKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/foo/A.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ), getProcessedSources(output)
            )
        }
    }

    @Test
    fun testClasspathChanges() {
        val project = Project(
            "incrementalMultiproject",
            GradleVersionRequired.None
        ).apply {
            setupWorkingDir()
            val processorPath = generateProcessor("AGGREGATING")

            projectDir.resolve("app/build.gradle").appendText(
                """

                    apply plugin: "kotlin-kapt"
                dependencies {
                  implementation "org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version"
                  kapt files("${processorPath.invariantSeparatorsPath}")
                }
            """.trimIndent()
            )

            projectDir.resolve("lib/build.gradle").appendText(
                """

                dependencies {
                  implementation "org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version"
                }
            """.trimIndent()
            )
        }

        project.build("clean", ":app:build") {
            assertSuccessful()
        }

        project.projectFile("A.kt").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "fun anotherFun() {}\n }"
        }
        project.build("build") {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/AA.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/AAA.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/BB.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/FooUseAKt.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/FooUseBKt.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/FooUseAAKt.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/foo/FooUseBBKt.java").canonicalPath,
                    fileInWorkingDir("app/build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath

                ), getProcessedSources(output)
            )
        }

        project.projectFile("A.kt").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "private fun privateFunction() {}\n }"
        }
        project.build("build") {
            assertSuccessful()
            assertTrue(getProcessedSources(output).isEmpty())
        }
    }

    @Test
    fun testIncompatibleClasspathChanges() {
        val project = getProject()
        project.projectFile("useB.kt").modify { current ->
            current + """
                
                @example.ExampleAnnotation
                fun addedFunctionB() = ""
            """.trimIndent()
        }
        project.build("clean", "build") {
            assertSuccessful()
        }

        project.projectFile("useB.kt").modify { current ->
            current.replace("fun addedFunctionB", "fun renamedFunctionB")
        }
        project.gradleBuildScript().appendText("""
            
            dependencies {
                compile 'com.google.guava:guava:12.0'
            }
        """.trimIndent())
        project.build("build") {
            assertSuccessful()

            assertFalse(
                fileInWorkingDir("build/generated/source/kapt/main/bar/AddedFunctionBGenerated.java").exists(),
                "Generated file should be deleted for renamed function when classpath changes."
            )
        }
    }
}