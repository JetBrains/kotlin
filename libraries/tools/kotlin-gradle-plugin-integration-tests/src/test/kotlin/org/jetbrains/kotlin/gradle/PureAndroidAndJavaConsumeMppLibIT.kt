/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.*
import java.lang.Boolean as RefBoolean

@RunWith(Parameterized::class)
class PureAndroidAndJavaConsumeMppLibIT : BaseGradleIT() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useFlavors: {0}, isAndroidDebugOnly: {1}")
        fun testCases(): List<Array<Boolean>> =
            listOf( /* useFlavors, isAndroidDebugOnly */
                arrayOf(false, false),
                arrayOf(false, true),
                arrayOf(true, false),
                arrayOf(true, true)
            )
    }

    @Parameterized.Parameter(0)
    lateinit var useFlavorsParameter: RefBoolean

    @Parameterized.Parameter(1)
    lateinit var isAndroidDebugOnlyParameter: RefBoolean

    lateinit var buildOptions: BuildOptions

    @Before
    override fun setUp() {
        val jdk11Home = File(System.getProperty("jdk11Home"))
        Assume.assumeTrue("This test requires JDK11 for AGP7", jdk11Home.isDirectory)

        buildOptions = defaultBuildOptions().copy(
            javaHome = jdk11Home,
            androidHome = KtTestUtil.findAndroidSdk(),
            androidGradlePluginVersion = AGPVersion.v7_0_0
        )
        buildOptions.acceptAndroidSdkLicenses()

        super.setUp()
    }

    @Test
    fun test() {
        val gradleVersionRequirement = GradleVersionRequired.AtLeast("7.0")

        val isAndroidDebugOnly = isAndroidDebugOnlyParameter.booleanValue()
        val useFlavors = useFlavorsParameter.booleanValue()

        val publishedProject = Project("new-mpp-android", gradleVersionRequirement).apply {
            projectDir.deleteRecursively()
            setupWorkingDir()
            // Don't need custom attributes here
            gradleBuildScript("lib").modify { text ->
                text.lines().filterNot { it.trimStart().startsWith("attribute(") }.joinToString("\n")
                    .let {
                        if (isAndroidDebugOnly) it.checkedReplace(
                            "publishAllLibraryVariants()",
                            "publishLibraryVariants(\"${if (useFlavors) "flavor1Debug" else "debug"}\")"
                        ) else it
                    }
                    .let {
                        if (useFlavors) {
                            it + "\n" + """
                                    android { 
                                        flavorDimensions "myFlavor"
                                        productFlavors {
                                            flavor1 { dimension "myFlavor" }
                                        }
                                    }
                                    """.trimIndent()
                        } else it
                    }
            }
        }

        publishedProject.build(":lib:publish", options = buildOptions) {
            assertSuccessful()
        }
        val repoDir = publishedProject.projectDir.resolve("lib/build/repo")

        val consumerProject = Project("AndroidProject", gradleVersionRequirement).apply {
            projectDir.deleteRecursively()
            setupWorkingDir()
            gradleBuildScript("Lib").apply {
                writeText(
                    // Remove the Kotlin plugin from the consumer project to check how pure-AGP Kotlin-less consumers resolve the dependency
                    readText().checkedReplace("apply plugin: 'kotlin-android'", "//").let { text ->
                        // If the test case doesn't assume flavors, remove the flavor setup lines:
                        if (useFlavors) text else text.lines().filter { !it.trim().startsWith("flavor") }.joinToString("\n")
                    } + "\n" + """
                    android {
                        buildTypes {
                            // We create a build type that is missing in the library in order to check how it resolves such an MPP lib
                            create("staging") { initWith(getByName("debug")) }
                        }
                    }
                    repositories {
                        maven { setUrl("${repoDir.absolutePath}") }                    
                    }
                    dependencies {
                        implementation("com.example:lib:1.0")
                    }
                """.trimIndent()
                )
            }
        }
        val variantForReleaseAndStaging =
            if (isAndroidDebugOnly) "debugApiElements-published" else "releaseApiElements-published"

        fun nameWithFlavorIfNeeded(name: String) = if (useFlavors) "flavor1${name.capitalize()}" else name

        val configurationToExpectedVariant = listOf(
            nameWithFlavorIfNeeded("debugCompileClasspath") to nameWithFlavorIfNeeded("debugApiElements-published"),
            nameWithFlavorIfNeeded("releaseCompileClasspath") to nameWithFlavorIfNeeded(variantForReleaseAndStaging),
            nameWithFlavorIfNeeded("stagingCompileClasspath") to nameWithFlavorIfNeeded(variantForReleaseAndStaging)
        )
        configurationToExpectedVariant.forEach { (configuration, expected) ->
            consumerProject.build(
                ":Lib:dependencyInsight",
                "--configuration", configuration,
                "--dependency", "com.example:lib",
                options = buildOptions
            ) {
                assertSuccessful()
                assertContains("variant \"$expected\" [")
            }
        }

        // Also test that a pure Java (Kotlin-less) project is able to resolve the MPP library dependency to the JVM variants:
        consumerProject.apply {
            gradleSettingsScript().appendText("\ninclude(\"pure-java\")")
            projectDir.resolve("pure-java/build.gradle.kts").also { it.parentFile.mkdirs() }.writeText(
                """
                    plugins {
                        java
                    }
                    repositories {
                        maven { setUrl("${repoDir.absolutePath}") }                    
                    }
                    dependencies {
                        implementation("com.example:lib:1.0")
                    }
                    """.trimIndent()
            )

            build(
                ":pure-java:dependencyInsight",
                "--configuration", "compileClasspath",
                "--dependency", "com.example:lib",
                options = buildOptions
            ) {
                assertSuccessful()
                assertContains("variant \"jvmLibApiElements-published\" [")
            }
        }
    }
}