/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.noarg.gradle.NoArgExtension
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@MppGradlePluginTests
class MppCompilerPluginsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testMppBuildWithCompilerPluginsJvm(gradleVersion: GradleVersion) {
        testMppBuildWithCompilerPlugins(
            gradleVersion = gradleVersion,
            enablePlugins = true,
            targetSourceSet = "jvm6Main",
            compileTask = ":compileKotlinJvm6",
            sourceSets = listOf("commonMain", "jvm6Main"),
            additionalAssertions = {
                assertFileInProjectExists("build/classes/kotlin/jvm6/main/com/example/Annotated.class")
                assertFileInProjectExists("build/classes/kotlin/jvm6/main/com/example/Override.class")
            }
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testMppBuildWithCompilerPluginsJs(gradleVersion: GradleVersion) {
        testMppBuildWithCompilerPlugins(
            gradleVersion = gradleVersion,
            enablePlugins = true,
            targetSourceSet = "nodeJsMain",
            compileTask = ":compileKotlinNodeJs",
            sourceSets = listOf("commonMain", "nodeJsMain")
        )
    }

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testMppBuildWithoutCompilerPluginsNative(gradleVersion: GradleVersion) {
        testMppBuildWithCompilerPlugins(
            gradleVersion = gradleVersion,
            enablePlugins = false,
            targetSourceSet = null, // No platform-specific Override.kt for native
            compileTask = ":compileKotlinLinux64",
            sourceSets = listOf("commonMain", "linux64Main", "nativeMain")
        )
    }

    private fun testMppBuildWithCompilerPlugins(
        gradleVersion: GradleVersion,
        enablePlugins: Boolean,
        targetSourceSet: String?,
        compileTask: String,
        sourceSets: List<String>,
        additionalAssertions: TestProject.() -> Unit = {}
    ) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899(),
        ) {

            if (enablePlugins) {
                setupCompilerPlugins()
            }

            setupBuildScript(enablePlugins)
            setupSourceFiles()

            // Create platform-specific Override.kt if needed
            targetSourceSet?.let { sourceSet ->
                createOverrideClass(sourceSet)
            }

            build("assemble", "printCompilerPluginOptions") {
                assertTasksExecuted(compileTask)
                additionalAssertions()
                validateCompilerPlugins(sourceSets, enablePlugins)
            }
        }
    }

    private fun TestProject.setupCompilerPlugins() {
        buildGradle.replaceText(
            """//id("org.jetbrains.kotlin.plugin.allopen")""",
            """id("org.jetbrains.kotlin.plugin.allopen")""",
        )
        buildGradle.replaceText(
            """//id("org.jetbrains.kotlin.plugin.noarg")""",
            """id("org.jetbrains.kotlin.plugin.noarg")""",
        )
    }

    private fun TestProject.setupBuildScript(enablePlugins: Boolean) {
        buildScriptInjection {
            val printOptionsTaskName = "printCompilerPluginOptions"
            val argsMarker = "=args=>"
            val classpathMarker = "=cp=>"

            if (enablePlugins) {
                project.extensions.configure(AllOpenExtension::class.java) {
                    it.annotation("com.example.Annotation")
                }
                project.extensions.configure(NoArgExtension::class.java) {
                    it.annotation("com.example.Annotation")
                }
            }

            project.tasks.register(printOptionsTaskName) { task ->
                val extension = project.multiplatformExtension
                extension.targets.all { target ->
                    target.compilations.all { compilation ->
                        /*force to configure the*/ compilation.compileTaskProvider.get()
                    }
                }
                val arguments = project.provider {
                    extension.sourceSets.associate { sourceSet ->
                        val languageSettings = sourceSet.languageSettings as DefaultLanguageSettingsBuilder
                        val args = languageSettings.compilerPluginArguments
                        val cp = languageSettings.compilerPluginClasspath?.files
                        sourceSet.name to (args to cp)
                    }
                }
                task.doFirst {
                    arguments.get().forEach { sourceSetName, (args, cp) ->
                        println("$sourceSetName$argsMarker$args")
                        println("$sourceSetName$classpathMarker$cp")
                    }
                }
            }
        }
    }

    private fun TestProject.setupSourceFiles() {
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
    }

    private fun TestProject.createOverrideClass(sourceSet: String) {
        projectPath.resolve("src/$sourceSet/kotlin/Override.kt").writeText(
            """
            package com.example

            @Annotation
            class Override : Annotated(0) {
                override var x = 3
            }
            """.trimIndent()
        )
    }

    private fun BuildResult.validateCompilerPlugins(sourceSets: List<String>, expectPlugins: Boolean) {
        val argsMarker = "=args=>"
        val classpathMarker = "=cp=>"
        val compilerPluginArgsRegex = "(\\w+)${Regex.escape(argsMarker)}(.*)".toRegex()
        val compilerPluginClasspathRegex = "(\\w+)${Regex.escape(classpathMarker)}(.*)".toRegex()

        val (compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet) =
            listOf(compilerPluginArgsRegex, compilerPluginClasspathRegex)
                .map { marker ->
                    marker.findAll(output).associate { it.groupValues[1] to it.groupValues[2] }
                }

        sourceSets.forEach { sourceSet ->
            if (expectPlugins) {
                validatePluginsPresent(sourceSet, compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet)
            } else {
                validatePluginsAbsent(sourceSet, compilerPluginArgsBySourceSet, compilerPluginClasspathBySourceSet)
            }
        }
    }

    private fun validatePluginsPresent(
        sourceSet: String,
        argsBySourceSet: Map<String, String>,
        classpathBySourceSet: Map<String, String>
    ) {
        val expectedArgs = listOf(
            "plugin:org.jetbrains.kotlin.allopen:annotation=com.example.Annotation",
            "plugin:org.jetbrains.kotlin.noarg:annotation=com.example.Annotation",
        ).joinToString(", ", prefix = "[", postfix = "]")

        val actualPluginArgs = argsBySourceSet[sourceSet]
        assertEquals(expectedArgs, actualPluginArgs, "Expected $expectedArgs as plugin args for $sourceSet")

        val actualPluginClasspath = classpathBySourceSet[sourceSet]
        assertNotNull(actualPluginClasspath, "Could not find compiler plugin classpath for $sourceSet")
        assertContains(actualPluginClasspath, "kotlin-allopen")
        assertContains(actualPluginClasspath, "kotlin-noarg")
    }

    private fun validatePluginsAbsent(
        sourceSet: String,
        argsBySourceSet: Map<String, String>,
        classpathBySourceSet: Map<String, String>
    ) {
        val actualPluginArgs = argsBySourceSet[sourceSet]
        val expectedArgs = "[]" // Empty args list
        assertEquals(expectedArgs, actualPluginArgs ?: "[]", "Expected no plugin args for native $sourceSet")

        val actualPluginClasspath = classpathBySourceSet[sourceSet]
        actualPluginClasspath?.let { classpath ->
            assertNotContains(classpath, "kotlin-allopen", "Native should not have allopen plugin")
            assertNotContains(classpath, "kotlin-noarg", "Native should not have noarg plugin")
        }
    }

    private fun assertNotContains(actual: String, expected: String, message: String) {
        if (actual.contains(expected)) {
            throw AssertionError("$message: Expected '$actual' to not contain '$expected'")
        }
    }
}