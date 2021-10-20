/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.lang.Boolean as RefBoolean

/**
 * It is important to run both pre-7.0 and post-7.0 tests, as Gradle 7.0 + AGP 7 introduces a new attribute to distinguish JVM vs Android
 */
class PureAndroidAndJavaConsumeMppLibPreGradle7IT : PureAndroidAndJavaConsumeMppLibIT() {
    override val agpVersion: AGPVersion
        get() = AGPVersion.v4_2_0

    override val gradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.Exact("6.9")
}

/**
 * It is important to run both pre-7.0 and post-7.0 tests, as Gradle 7.0 + AGP 7 introduces a new attribute to distinguish JVM vs Android
 */
class PureAndroidAndJavaConsumeMppLibGradle7PlusIT : PureAndroidAndJavaConsumeMppLibIT() {
    override val agpVersion: AGPVersion
        get() = AGPVersion.v7_0_0

    override val gradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("7.0")
}

@RunWith(Parameterized::class)
abstract class PureAndroidAndJavaConsumeMppLibIT : BaseGradleIT() {
    abstract val agpVersion: AGPVersion
    abstract val gradleVersion: GradleVersionRequired

    @field:Rule
    @JvmField
    var collector: ErrorCollector = ErrorCollector()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useFlavors: {0}, isAndroidPublishDebugOnly: {1}, isPublishedLibrary: {2}")
        fun testCases(): List<Array<Boolean>> =
            listOf(
                /* useFlavors, isAndroidPublishDebugOnly, isPublishedLibrary */
                arrayOf(false, false, false),
                arrayOf(false, true, false),
                arrayOf(true, false, false),
                arrayOf(true, true, false),
                arrayOf(false, false, true),
                arrayOf(false, true, true),
                arrayOf(true, false, true),
                arrayOf(true, true, true),
            )
    }

    @Parameterized.Parameter(0)
    lateinit var useFlavorsParameter: RefBoolean

    @Parameterized.Parameter(1)
    lateinit var isAndroidPublishDebugOnlyParameter: RefBoolean

    lateinit var buildOptions: BuildOptions

    @Before
    override fun setUp() {
        val jdk11Home = File(System.getProperty("jdk11Home"))
        Assume.assumeTrue("This test requires JDK11 for AGP7", jdk11Home.isDirectory)

        buildOptions = defaultBuildOptions().copy(
            javaHome = jdk11Home,
            androidHome = KtTestUtil.findAndroidSdk(),
            androidGradlePluginVersion = agpVersion
        )
        buildOptions.androidHome?.let { acceptAndroidSdkLicenses(it) }

        super.setUp()
    }

    @Parameterized.Parameter(2)
    lateinit var isPublishedLibraryParameter: RefBoolean

    @Test
    fun test() {
        val isAndroidPublishDebugOnly = isAndroidPublishDebugOnlyParameter.booleanValue()
        val useFlavors = useFlavorsParameter.booleanValue()
        val isPublishedLibrary = isPublishedLibraryParameter.booleanValue()

        val dependencyProject = Project("new-mpp-android", gradleVersion).apply {
            projectDir.deleteRecursively()
            setupWorkingDir()
            // Don't need custom attributes here
            gradleBuildScript("lib").modify { text ->
                text.lines().filterNot { it.trimStart().startsWith("attribute(") }.joinToString("\n")
                    .let {
                        if (isAndroidPublishDebugOnly) it.checkedReplace(
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
                    }.let {
                        // Simulate the behavior with user-defined consumable configuration added with no proper attributes:
                        it + "\n" + """
                            configurations.create("legacyConfiguration") {
                                def bundlingAttribute = Attribute.of("org.gradle.dependency.bundling", String)
                                attributes.attribute(bundlingAttribute, "external")
                            }
                        """.trimIndent()
                    }
            }
        }

        if (isPublishedLibrary) {
            dependencyProject.build(":lib:publish", options = buildOptions) {
                assertSuccessful()
            }
        }

        val repositoryLinesIfNeeded = if (isPublishedLibrary) """
                repositories {
                    maven { setUrl("${dependencyProject.projectDir.resolve("lib/build/repo").toURI()}") }                    
                }
            """.trimIndent() else ""

        val dependencyNotation =
            if (isPublishedLibrary)
                """"com.example:lib:1.0""""
            else "project(\":${dependencyProject.projectName}:lib\")"

        val variantNamePublishedSuffix = if (isPublishedLibrary) "-published" else ""

        val dependencyInsightModuleName =
            if (isPublishedLibrary)
                "com.example:lib"
            else ":${dependencyProject.projectName}:lib"

        val consumerProject = Project("AndroidProject", gradleVersion).apply {
            projectDir.deleteRecursively()
            if (!isPublishedLibrary) {
                embedProject(dependencyProject)
                gradleSettingsScript().appendText(
                    "\ninclude(\":${dependencyProject.projectName}:lib\")"
                )
            }
            setupWorkingDir()

            gradleBuildScript("Lib").apply {
                writeText(
                    // Remove the Kotlin plugin from the consumer project to check how pure-AGP Kotlin-less consumers resolve the dependency
                    readText().checkedReplace("id 'org.jetbrains.kotlin.android'", "//").let { text ->
                        // If the test case doesn't assume flavors, remove the flavor setup lines:
                        if (useFlavors) text else text.lines().filter { !it.trim().startsWith("flavor") }.joinToString("\n")
                    } + "\n" + """
                    android {
                        buildTypes {
                            // We create a build type that is missing in the library in order to check how it resolves such an MPP lib
                            create("staging") { initWith(getByName("debug")) }
                        }
                    }
                    $repositoryLinesIfNeeded
                    dependencies {
                        implementation($dependencyNotation)
                    }
                """.trimIndent()
                )
            }
        }
        val variantForReleaseAndStaging = if (isAndroidPublishDebugOnly && isPublishedLibrary)
            "debugApiElements$variantNamePublishedSuffix"
        else "releaseApiElements$variantNamePublishedSuffix"

        fun nameWithFlavorIfNeeded(name: String) = if (useFlavors) "flavor1${name.capitalize()}" else name

        val configurationToExpectedVariant = listOf(
            nameWithFlavorIfNeeded("debugCompileClasspath") to nameWithFlavorIfNeeded("debugApiElements$variantNamePublishedSuffix"),
            nameWithFlavorIfNeeded("releaseCompileClasspath") to nameWithFlavorIfNeeded(variantForReleaseAndStaging),
            nameWithFlavorIfNeeded("stagingCompileClasspath") to
                    if (isPublishedLibrary)
                        nameWithFlavorIfNeeded(variantForReleaseAndStaging)
                    // NB: unlike published library, both the release and debug variants provide the build type attribute
                    //     and therefore are not compatible with the "staging" consumer. So it can only use the JVM variant
                    else "jvmLibApiElements"
        )
        configurationToExpectedVariant.forEach { (configuration, expected) ->
            consumerProject.build(
                ":Lib:dependencyInsight",
                "--configuration", configuration,
                "--dependency", dependencyInsightModuleName,
                options = buildOptions
            ) {
                assertSuccessful()
                if (project.testGradleVersionBelow("7.0") && !isPublishedLibrary) {
                    /* TODO: the issue KT-30961 is only fixed for Gradle 7.0+ and AGP 7+. Older versions still reproduce the issue;
                     *  This test asserts the existing incorrect behavior for older Gradle versions in order to detect unintentional changes
                     */
                    assertVariantInDependencyInsight("jvmLibApiElements")
                } else
                    assertVariantInDependencyInsight(expected)
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
                    $repositoryLinesIfNeeded
                    dependencies {
                        implementation($dependencyNotation)
                    }
                    """.trimIndent()
            )

            build(
                ":pure-java:dependencyInsight",
                "--configuration", "compileClasspath",
                "--dependency", dependencyInsightModuleName,
                options = buildOptions
            ) {
                assertSuccessful()
                assertVariantInDependencyInsight("jvmLibApiElements$variantNamePublishedSuffix")
            }
        }
    }

    private fun CompiledProject.assertVariantInDependencyInsight(variantName: String) {
        try {
            assertContains("variant \"$variantName\" [")
        } catch (originalError: AssertionError) {
            val matchedVariants = Regex("variant \"(.*?)\" \\[").findAll(output).toList()
            val failure = AssertionError(
                "Expected variant $variantName. " + if (matchedVariants.isNotEmpty()) "Matched instead: " + matchedVariants
                    .joinToString { it.groupValues[1] } else "No match.",
                originalError
            )
            this.output
            collector.addError(failure)
        }
    }
}