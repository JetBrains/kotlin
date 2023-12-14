/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.incapt.IncrementalAggregatingProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalBinaryIsolatingProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalIsolatingProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessor
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Kapt incremental tests with aggregating apt")
open class KaptIncrementalWithAggregatingApt : KaptIncrementalIT() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        incremental = true,
        kaptOptions = super.defaultBuildOptions.kaptOptions!!.copy(
            verbose = true,
            incrementalKapt = true,
            includeCompileClasspath = false
        )
    )

    override fun KGPBaseTest.kaptProject(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions,
        buildJdk: File?,
        test: TestProject.() -> Unit
    ): TestProject =
        project(
            PROJECT_NAME,
            gradleVersion,
            buildOptions,
            buildJdk = buildJdk
        ) {
            setupIncrementalAptProject("AGGREGATING")
            test(this)
        }

    @DisplayName("On incremental changes")
    @GradleTest
    fun testIncrementalChanges(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("clean", "assemble")

            javaSourcesDir()
                .resolve("bar/useB.kt")
                .modify { current -> "$current\nfun otherFunction() {}" }

            build("assemble") {
                assertEquals(
                    listOf(
                        "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                        "$KAPT3_STUBS_PATH/bar/B.java",
                        "$KAPT3_STUBS_PATH/baz/UtilKt.java",
                        "$KAPT3_STUBS_PATH/foo/A.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }

            javaSourcesDir().resolve("foo/JavaClass.java").modify { current ->
                val lastBrace = current.lastIndexOf("}")
                current.substring(0, lastBrace) + "private void anotherFun() {}\n }"
            }
            build("assemble") {
                assertEquals(
                    setOf(
                        "src/main/java/foo/JavaClass.java",
                        "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                        "$KAPT3_STUBS_PATH/bar/B.java",
                        "$KAPT3_STUBS_PATH/baz/UtilKt.java",
                        "$KAPT3_STUBS_PATH/foo/A.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }
        }
    }

    @DisplayName("Incremental changes in JDK9+")
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    @GradleWithJdkTest
    fun testIncrementalChangesWithJdk9(gradleVersion: GradleVersion, jdk: JdkVersions.ProvidedJdk) {
        kaptProject(gradleVersion, buildJdk = jdk.location) {
            build("clean", "assemble")

            javaSourcesDir().resolve("bar/useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
            build("assemble") {
                assertEquals(
                    setOf(
                        "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                        "$KAPT3_STUBS_PATH/bar/B.java",
                        "$KAPT3_STUBS_PATH/baz/UtilKt.java",
                        "$KAPT3_STUBS_PATH/foo/A.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }
        }
    }

    @DisplayName("Changes in classpath triggers incremental compilation")
    @GradleTest
    fun testClasspathChanges(gradleVersion: GradleVersion) {
        project("incrementalMultiproject", gradleVersion) {
            val processorPath = generateProcessor("AGGREGATING" to IncrementalProcessor::class.java)

            subProject("app").buildGradle.modify {
                it
                    .replace("// kapt-plugin marker", "id 'org.jetbrains.kotlin.kapt'")
                    .plus(
                        """
                        
                        dependencies {
                            implementation "org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version"
                            kapt files("${processorPath.invariantSeparatorsPath}")
                        }
                        """.trimIndent()
                    )
            }

            subProject("lib").buildGradle.append(
                """

                dependencies {
                    implementation "org.jetbrains.kotlin:kotlin-stdlib:${'$'}kotlin_version"
                }
                """.trimIndent()
            )

            build("clean", ":app:assemble")

            val aKtSourceFile = subProject("lib").kotlinSourcesDir().resolve("bar/A.kt")
            aKtSourceFile.modify { current ->
                val lastBrace = current.lastIndexOf("}")
                current.substring(0, lastBrace) + "fun anotherFun() {}\n }"
            }

            build("assemble") {
                assertEquals(
                    listOf(
                        "app/$KAPT3_STUBS_PATH/foo/AA.java",
                        "app/$KAPT3_STUBS_PATH/foo/AAA.java",
                        "app/$KAPT3_STUBS_PATH/foo/BB.java",
                        "app/$KAPT3_STUBS_PATH/foo/FooUseAKt.java",
                        "app/$KAPT3_STUBS_PATH/foo/FooUseBKt.java",
                        "app/$KAPT3_STUBS_PATH/foo/FooUseAAKt.java",
                        "app/$KAPT3_STUBS_PATH/foo/FooUseBBKt.java",
                        "app/$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }

            aKtSourceFile.modify { current ->
                val lastBrace = current.lastIndexOf("}")
                current.substring(0, lastBrace) + "private fun privateFunction() {}\n }"
            }
            build("assemble") {
                assertOutputContains("Skipping annotation processing as all sources are up-to-date.")
            }
        }
    }

    @DisplayName("Should clean previously generated files on incompatible classpath changes")
    @GradleTest
    fun testIncompatibleClasspathChanges(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            val useBSourceFile = javaSourcesDir().resolve("bar/useB.kt")
            useBSourceFile.modify { current ->
                current + """

                @example.ExampleAnnotation
                fun addedFunctionB() = ""
                """.trimIndent()
            }

            build("clean", "assemble")

            useBSourceFile.modify { current ->
                current.replace("fun addedFunctionB", "fun renamedFunctionB")
            }

            buildGradle.append(
                """

                dependencies {
                    implementation 'com.google.guava:guava:12.0'
                }
                """.trimIndent()
            )
            build("assemble") {
                // Generated file should be deleted for renamed function when classpath changes
                assertFileInProjectNotExists("build/generated/source/kapt/main/bar/AddedFunctionBGenerated.java")
            }
        }
    }

    @DisplayName("Incremental aggregating changes")
    @JdkVersions(versions = [JavaVersion.VERSION_1_8, JavaVersion.VERSION_11])
    @GradleWithJdkTest
    fun testIncrementalAggregatingChanges(gradleVersion: GradleVersion, jdk: JdkVersions.ProvidedJdk) {
        doIncrementalAggregatingChanges(gradleVersion, jdk)
    }

    @DisplayName("Incremental binary aggregating changes")
    @JdkVersions(versions = [JavaVersion.VERSION_1_8, JavaVersion.VERSION_11])
    @GradleWithJdkTest
    fun testIncrementalBinaryAggregatingChanges(gradleVersion: GradleVersion, jdk: JdkVersions.ProvidedJdk) {
        doIncrementalAggregatingChanges(
            gradleVersion,
            jdk,
            true
        )
    }

    private fun TestProject.checkAggregatingResource(check: (List<String>) -> Unit) {
        val aggregatingResource = "build/tmp/kapt3/classes/main/generated.txt"
        assertFileInProjectExists(aggregatingResource)
        val lines = projectPath.resolve(aggregatingResource).readLines()
        check(lines)
    }

    private fun doIncrementalAggregatingChanges(
        gradleVersion: GradleVersion,
        jdk: JdkVersions.ProvidedJdk,
        isBinary: Boolean = false,
    ) {
        project(
            "kaptIncrementalAggregatingProcessorProject",
            gradleVersion,
            buildJdk = jdk.location
        ) {
            setupIncrementalAptProject(
                "ISOLATING" to if (isBinary) IncrementalBinaryIsolatingProcessor::class.java else IncrementalIsolatingProcessor::class.java,
                "AGGREGATING" to IncrementalAggregatingProcessor::class.java
            )

            build("clean", "assemble") {
                assertEquals(
                    listOf(
                        "$KAPT3_STUBS_PATH/bar/WithAnnotation.java",
                        "$KAPT3_STUBS_PATH/bar/noAnnotations.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(1, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                }
            }

            //change file without annotations
            val noAnnotationsSourceFile = javaSourcesDir().resolve("bar/noAnnotations.kt")
            noAnnotationsSourceFile.modify { current -> "$current\nfun otherFunction() {}" }

            build("assemble") {
                assertEquals(
                    listOfNotNull(
                        "$KAPT3_STUBS_PATH/bar/NoAnnotationsKt.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                        "build/generated/source/kapt/main/bar/WithAnnotationGenerated.java".takeUnless { isBinary },
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(1, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                }
            }

            //add new file with annotations
            val newBazClass = javaSourcesDir().resolve("baz/BazClass.kt").also {
                it.parent.createDirectories()
            }
            newBazClass.writeText(
                """
                package baz

                @example.ExampleAnnotation
                class BazClass() {}
                """.trimIndent()
            )

            build("assemble") {
                assertEquals(
                    listOfNotNull(
                        "$KAPT3_STUBS_PATH/baz/BazClass.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                        "build/generated/source/kapt/main/bar/WithAnnotationGenerated.java".takeUnless { isBinary },
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(2, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                    assertTrue(lines.contains("BazClassGenerated"))
                }
            }

            //move annotation to nested class
            newBazClass.modify {
                """
                package baz

                class BazClass() {
                    @example.ExampleAnnotation
                    class BazNested {}
                }
                """.trimIndent()
            }

            build("assemble") {
                assertEquals(
                    listOfNotNull(
                        "$KAPT3_STUBS_PATH/baz/BazClass.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                        "build/generated/source/kapt/main/bar/WithAnnotationGenerated.java".takeUnless { isBinary },
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(2, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                    assertTrue(lines.contains("BazNestedGenerated"))
                }
            }

            //change file without annotations to check that nested class is aggregated
            noAnnotationsSourceFile.modify { current -> "$current\nfun otherFunction2() {}" }

            build("assemble") {
                assertEquals(
                    listOfNotNull(
                        "$KAPT3_STUBS_PATH/bar/NoAnnotationsKt.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                        "build/generated/source/kapt/main/bar/WithAnnotationGenerated.java".takeUnless { isBinary },
                        "build/generated/source/kapt/main/BazClass/BazNestedGenerated.java".takeUnless { isBinary },
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(2, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                    assertTrue(lines.contains("BazNestedGenerated"))
                }
            }

            // make sure that changing the origin of isolating that produced
            javaSourcesDir().resolve("bar/withAnnotation.kt")
                .modify { current -> current.substringBeforeLast("}") + "\nfun otherFunction() {} }" }
            build("assemble") {
                assertEquals(
                    listOfNotNull(
                        "$KAPT3_STUBS_PATH/bar/WithAnnotation.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                        "build/generated/source/kapt/main/BazClass/BazNestedGenerated.java".takeUnless { isBinary },
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )

                checkAggregatingResource { lines ->
                    assertEquals(2, lines.size)
                    assertTrue(lines.contains("WithAnnotationGenerated"))
                    assertTrue(lines.contains("BazNestedGenerated"))
                }
            }
        }
    }
}


@DisplayName("Kapt incremental tests with aggregating apt with disabled precise compilation outputs backup")
class KaptIncrementalWithAggregatingAptAndWithoutPreciseBackup : KaptIncrementalWithAggregatingApt() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}