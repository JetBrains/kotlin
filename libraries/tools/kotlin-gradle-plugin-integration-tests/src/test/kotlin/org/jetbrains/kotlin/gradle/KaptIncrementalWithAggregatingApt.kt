/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incapt.*
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

    private fun jdk9Options(javaHome: File): BuildOptions {
        Assume.assumeTrue("JDK 9 isn't available", javaHome.isDirectory)
        return defaultBuildOptions().copy(javaHome = javaHome)
    }

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
        val options = jdk9Options(javaHome)
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
            val processorPath = generateProcessor("AGGREGATING" to IncrementalProcessor::class.java)

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
            assertTrue(output.contains("Skipping annotation processing as all sources are up-to-date."))
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
                implementation 'com.google.guava:guava:12.0'
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

    @Test
    fun testIncrementalAggregatingChanges() {
        doIncrementalAggregatingChanges()
    }

    @Test
    fun testIncrementalAggregatingChangesWithJdk9() {
        val javaHome = File(System.getProperty("jdk9Home")!!)
        doIncrementalAggregatingChanges(buildOptions = jdk9Options(javaHome))
    }

    @Test
    fun testIncrementalBinaryAggregatingChanges() {
        doIncrementalAggregatingChanges(true)
    }

    @Test
    fun testIncrementalBinaryAggregatingChangesWithJdk9() {
        val javaHome = File(System.getProperty("jdk9Home")!!)
        val options = jdk9Options(javaHome)
        doIncrementalAggregatingChanges(true, options)
    }

    private fun CompiledProject.checkAggregatingResource(check: (List<String>) -> Unit) {
        val aggregatingResource = "build/tmp/kapt3/classes/main/generated.txt"
        assertFileExists(aggregatingResource)
        val lines = fileInWorkingDir(aggregatingResource).readLines()
        check(lines)
    }

    fun doIncrementalAggregatingChanges(isBinary: Boolean = false, buildOptions: BuildOptions = defaultBuildOptions()) {
        val project = Project(
            "kaptIncrementalAggregatingProcessorProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject(
                "ISOLATING" to if (isBinary) IncrementalBinaryIsolatingProcessor::class.java else IncrementalIsolatingProcessor::class.java,
                "AGGREGATING" to IncrementalAggregatingProcessor::class.java
            )
        }

        project.build("clean", "build", options = buildOptions) {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/WithAnnotation.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/noAnnotations.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(1, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
            }
        }

        //change file without annotations
        project.projectFile("noAnnotations.kt").modify { current -> "$current\nfun otherFunction() {}" }

        project.build("build", options = buildOptions) {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/NoAnnotationsKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath,
                    fileInWorkingDir("build/generated/source/kapt/main/bar/WithAnnotationGenerated.java").canonicalPath.takeUnless { isBinary },
                ).filterNotNull().toSet(), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(1, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
            }
        }

        //add new file with annotations
        val newFile = File(project.projectDir, "src/main/java/baz/BazClass.kt")
        newFile.parentFile.mkdirs()
        newFile.createNewFile()
        project.projectFile("BazClass.kt").modify {
            """
                package baz

                @example.ExampleAnnotation
                class BazClass() {}
            """.trimIndent()
        }
        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/BazClass.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath,
                    fileInWorkingDir("build/generated/source/kapt/main/bar/WithAnnotationGenerated.java").canonicalPath.takeUnless { isBinary },
                ).filterNotNull().toSet(), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(2, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
                assertTrue(lines.contains("BazClassGenerated"))
            }
        }

        //move annotation to nested class
        project.projectFile("BazClass.kt").modify {
            """
                package baz

                class BazClass() {
                    @example.ExampleAnnotation
                    class BazNested {}                 
                }
            """.trimIndent()
        }
        project.build("build", options = buildOptions) {
            assertSuccessful()
            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/baz/BazClass.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath,
                    fileInWorkingDir("build/generated/source/kapt/main/bar/WithAnnotationGenerated.java").canonicalPath.takeUnless { isBinary },
                ).filterNotNull().toSet(), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(2, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
                assertTrue(lines.contains("BazNestedGenerated"))
            }
        }

        //change file without annotations to check that nested class is aggregated
        project.projectFile("noAnnotations.kt").modify { current -> "$current\nfun otherFunction2() {}" }

        project.build("build", options = buildOptions) {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/NoAnnotationsKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath,
                    fileInWorkingDir("build/generated/source/kapt/main/bar/WithAnnotationGenerated.java").canonicalPath.takeUnless { isBinary },
                    fileInWorkingDir("build/generated/source/kapt/main/BazClass/BazNestedGenerated.java").canonicalPath.takeUnless { isBinary },
                ).filterNotNull().toSet(), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(2, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
                assertTrue(lines.contains("BazNestedGenerated"))
            }
        }

        // make sure that changing the origin of isolating that produced
        project.projectFile("withAnnotation.kt").modify { current -> current.substringBeforeLast("}") + "\nfun otherFunction() {} }" }
        project.build("build", options = buildOptions) {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/WithAnnotation.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath,
                    fileInWorkingDir("build/generated/source/kapt/main/BazClass/BazNestedGenerated.java").canonicalPath.takeUnless { isBinary },
                ).filterNotNull().toSet(), getProcessedSources(output)
            )

            checkAggregatingResource { lines ->
                assertEquals(2, lines.size)
                assertTrue(lines.contains("WithAnnotationGenerated"))
                assertTrue(lines.contains("BazNestedGenerated"))
            }
        }
    }

}