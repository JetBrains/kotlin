/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalAbiValidation::class)

package org.jetbrains.kotlin.gradle.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.abi.utils.*
import org.jetbrains.kotlin.gradle.abi.utils.AbiValidationTestDumps.assertDumpsEqual
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@JvmGradlePluginTests
class AbiValidationJvmIT : KGPBaseTest() {

    @GradleTest
    fun testForEnabledAbiValidation(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()
            // create the reference dumps to check
            build("updateKotlinAbi")

            build("check") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("Test ABI dump check with default settings")
    fun testSimpleCheck(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            kotlinSourcesDir().source("SimpleClass.kt") { AbiValidationTestDumps.SIMPLE_CLASS }
            createReferenceJvmDumpFile(AbiValidationTestDumps.SIMPLE_DUMP_JVM)

            abiValidation()

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("ABI validation should fail if the dump does not match the expected dump")
    fun testSimpleCheckFailure(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            kotlinSourcesDir().source("SimpleClass.kt") { AbiValidationTestDumps.SIMPLE_CLASS }
            createReferenceJvmDumpFile("An old ABI dump")

            abiValidation()

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }
        }
    }


    @GradleTest
    @DisplayName("checkKotlinAbi should fail, when there is no abi directory, even if there are no Kotlin sources")
    fun testNoDump(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("checkKotlinAbi should succeed, when abi file is empty, but where are no kotlin source files")
    fun testEmptyDump(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()

            createReferenceJvmDumpFile()

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @CaseSensitiveCondition
    @DisplayName("Check dump file name is case sensitive")
    fun testFileNameCaseSensitive(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()

            createReferenceJvmDumpFile(overriddenProjectName = projectName.uppercase())

            buildAndFail("checkKotlinAbi") {
                assertTasksFailed(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should create empty abi file when there are no Kotlin sources")
    fun testUpdateEmptyDump(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual("", referenceJvmDumpFile())
        }
    }

    @GradleTest
    @DisplayName("updateKotlinAbi should create abi file with the name of the project, respecting settings file")
    fun testProjectName(
        gradleVersion: GradleVersion,
    ) {
        val newProjectName = "new-project-name"

        jvmProject(gradleVersion) {
            abiValidation()

            settingsGradleKts.appendText(
                """
                |
                |rootProject.name = "$newProjectName"
                |
                """.trimMargin()
            )
            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            val referenceJvmDumpFile = referenceJvmDumpFile(overriddenProjectName = newProjectName)
            assertDumpsEqual("", referenceJvmDumpFile)
        }
    }

    @GradleTest
    @DisplayName("Test custom reference dir")
    fun testCustomReferenceDir(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            val customDir = "custom/abi"
            val text = "some text"
            kotlinSourcesDir().source("SimpleClass.kt") { AbiValidationTestDumps.SIMPLE_CLASS }

            val customReferenceDir = projectPath.resolve(customDir).toFile().canonicalFile

            // user writes custom text in file matching the default dump file
            createReferenceJvmDumpFile(text)

            // override reference dir not to match with custom file
            abiValidation {
                referenceDumpDir.set(customReferenceDir)
            }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(AbiValidationTestDumps.SIMPLE_DUMP_JVM, referenceJvmDumpFile(dir = customDir))
            assertDumpsEqual(text, referenceJvmDumpFile())

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

    @GradleTest
    fun testCustomWrongReferenceDir(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            val newDir = projectPath.parent.resolve("outside-dir").toFile().canonicalFile
            abiValidation {
                referenceDumpDir.set(newDir)
            }

            buildAndFail("updateKotlinAbi") {
                assertTasksFailed(":updateKotlinAbi")
                assertOutputContains("'referenceDir' must be a subdirectory of the build root directory")
            }
        }
    }

    /**
     * This test is not testing all cases of filtering it just checks that the filters are passed to ABI tools correctly.
     * All the other tests on filtering should be placed in ABI Tools separately.
     */
    @GradleTest
    @DisplayName("Test passing filters to ABI tools")
    fun testFilters(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            kotlinSourcesDir().source("Filters.kt") { AbiValidationTestDumps.FILE_FOR_FILTERS }
            abiValidation {
                filters {
                    it.exclude {
                        byNames.addAll("**.ExcludedByNameClass")
                        annotatedWith.addAll("**.Exclude")
                    }
                    it.include {
                        byNames.addAll("**.*Class", "**.IncludedByName")
                        annotatedWith.addAll("**.Include")
                    }
                }
            }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(FILTERED_DUMP, referenceJvmDumpFile())
        }
    }

    @GradleTest
    @DisplayName("Test ABI dumps for generated sources")
    fun testGenerate(
        gradleVersion: GradleVersion,
    ) {
        jvmProject(gradleVersion) {
            abiValidation()

            buildScriptInjection {
                abstract class GenerateSourcesTask : DefaultTask() {
                    @get:OutputDirectory
                    abstract val outputDirectory: DirectoryProperty

                    @TaskAction
                    fun generate() {
                        outputDirectory.asFile.get().mkdirs()
                        outputDirectory.file("Generated.kt").get().asFile.writeText(
                            """
                        public class Generated { public fun helloCreator(): Int = 42 }
                    """.trimIndent()
                        )
                    }
                }

                val srcgen = project.tasks.register("generateSources", GenerateSourcesTask::class.java)
                srcgen.configure {
                    it.outputDirectory.set(project.layout.buildDirectory.get().dir("generated").dir("kotlin"))
                }

                project.applyJvm {
                    sourceSets.getByName("main") {
                        @OptIn(ExperimentalKotlinGradlePluginApi::class)
                        it.generatedKotlin.srcDir(srcgen)
                    }
                }
            }

            build("updateKotlinAbi") {
                assertTasksExecuted(":updateKotlinAbi")
            }

            assertDumpsEqual(GENERATED_SOURCES_DUMP, referenceJvmDumpFile())

            build("checkKotlinAbi") {
                assertTasksExecuted(":checkKotlinAbi")
            }
        }
    }

}


private val GENERATED_SOURCES_DUMP = """
        public final class Generated {
        	public fun <init> ()V
        	public final fun helloCreator ()I
        }


    """.trimIndent()

private val FILTERED_DUMP = """
        public final class test/classes/FirstClass {
        	public fun <init> ()V
        }
        
        public final class test/classes/Foo {
        	public fun <init> ()V
        }
        
        public final class test/classes/IncludedByAnnotation {
        	public fun <init> ()V
        }
        
        public final class test/classes/IncludedByName {
        	public fun <init> ()V
        }
        
        public final class test/classes/NoAnnotationClass {
        	public fun <init> ()V
        }
        
        public final class test/classes/SecondClass {
        	public fun <init> ()V
        }


    """.trimIndent()
