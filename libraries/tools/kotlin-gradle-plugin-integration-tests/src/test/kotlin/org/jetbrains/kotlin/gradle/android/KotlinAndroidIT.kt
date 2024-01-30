/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import kotlin.io.path.appendText
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.assertContains

@DisplayName("base kotlin-android tests")
@AndroidGradlePluginTests
class KotlinAndroidIT : KGPBaseTest() {

    @DisplayName("android project compilation smoke test")
    @GradleAndroidTest
    fun testSimpleCompile(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        fun BuildResult.assertKotlinGradleBuildServicesAreInitialized() {
            assertOutputContainsExactTimes("Initialized KotlinGradleBuildServices", expectedRepetitionTimes = 1)
            assertOutputContainsExactTimes("Disposed KotlinGradleBuildServices", expectedRepetitionTimes = 1)
        }

        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG),
            buildJdk = jdkVersion.location
        ) {
            val modules = listOf("Android", "Lib")
            val flavors = listOf("Flavor1", "Flavor2")
            val buildTypes = listOf("Debug")

            val expectedTasks = mutableListOf<String>()
            for (module in modules) {
                for (flavor in flavors) {
                    for (buildType in buildTypes) {
                        expectedTasks.add(":$module:compile$flavor${buildType}Kotlin")
                    }
                }
            }

            build("assembleDebug", ":Android:test") {
                val pattern = ":Android:compile[\\w\\d]+Kotlin".toRegex()
                assertTasksExecuted(expectedTasks + tasks.map { it.path }.filter { it.matches(pattern) })
                assertOutputContains("InternalDummyTest PASSED")
                assertKotlinGradleBuildServicesAreInitialized()
            }

            // Run the a build second time, assert everything is up-to-date
            build("assembleDebug") {
                assertTasksUpToDate(expectedTasks)
                assertKotlinGradleBuildServicesAreInitialized()
            }
        }
    }

    @DisplayName("KT-16897: the `assembleAndroidTest` task builds fine without the `build` task")
    @GradleAndroidTest
    fun testAssembleAndroidTest(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleAndroidTest")
        }
    }

    @DisplayName("KT-12303: compilation works fine with icepick")
    @GradleAndroidTest
    fun testAndroidIcepickProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidIcepickProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location,
            dependencyManagement = DependencyManagement.DefaultDependencyManagement(
                setOf("https://clojars.org/repo/")
            )
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("compilation works fine with parcelize")
    @GradleAndroidTest
    fun testParcelize(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidParcelizeProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("KT-46626: kotlin-android produces clear error message when no android plugin is applied")
    @GradleAndroidTest
    fun shouldAllowToApplyPluginWhenAndroidPluginIsMissing(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            fun replaceKotlinJvmByKotlinAndroid(specifyPluginVersion: Boolean): (String) -> CharSequence = { line ->
                if (line.contains("id \"org.jetbrains.kotlin.jvm\"")) {
                    "    id \"org.jetbrains.kotlin.android\"" + if (specifyPluginVersion) " version \"${'$'}kotlin_version\"" else ""
                } else {
                    line
                }
            }

            buildGradle.modify {
                it.lines().joinToString(
                    separator = "\n",
                    transform = replaceKotlinJvmByKotlinAndroid(false)
                )
            }
            settingsGradle.modify {
                it.lines().joinToString(
                    separator = "\n",
                    transform = replaceKotlinJvmByKotlinAndroid(true)
                )
            }

            buildAndFail("tasks") {
                assertOutputContains("'kotlin-android' plugin requires one of the Android Gradle plugins.")
            }
        }
    }

    @DisplayName("KT-49483: lint dependencies are resolved properly")
    @GradleAndroidTest
    fun testLintDependencyResolutionKt49483(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        disabledOnWindowsWhenAgpVersionIsLowerThan(agpVersion, "7.4.0", "Lint leaves opened file descriptors")
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            buildGradle.modify {
                it.replace(
                    "plugins {",
                    """
                    plugins {
                       id("com.android.lint")

                    """.trimIndent()
                )
            }

            subProject("Lib").buildGradle.appendText(
                //language=Gradle
                """
                
                android {
                    lintOptions.checkDependencies = true
                }
                dependencies {
                    implementation(project(":java-lib"))
                }
                """.trimIndent()
            )

            settingsGradle.appendText(
                //language=Gradle
                """
                
                include("java-lib")
                """.trimIndent()
            )

            subProject("java-lib").buildGradle.apply {
                Files.createDirectories(parent)
                writeText(
                    //language=Gradle
                    """
                    plugins {
                        id("java-library")
                        id("com.android.lint")
                    }
                    """.trimIndent()
                )
            }

            // Gradle 6 + AGP 4 produce a deprecation warning on parallel executions about resolving a configuration from another project
            build(":Lib:lintFlavor1Debug", buildOptions = buildOptions.copy(parallel = gradleVersion >= GradleVersion.version("7.0"))) {
                assertOutputDoesNotContain("as an external dependency and not analyze it.")
            }
        }
    }

    @DisplayName("publish with omitted stdlib version sets proper version")
    @GradleAndroidTest
    fun testOmittedStdlibVersion(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdkVersion.location
        ) {
            subProject("Lib").buildGradle.modify {
                it.checkedReplace(
                    "kotlin-stdlib:\$kotlin_version",
                    "kotlin-stdlib"
                ) +
                        //language=Gradle
                        """

                        apply plugin: 'maven-publish'
        
                        android {
                            defaultPublishConfig 'flavor1Debug'
                        }
                        
                        afterEvaluate {
                            publishing {
                                publications {
                                    flavorDebug(MavenPublication) {
                                        from components.flavor1Debug
                                        
                                        group = 'com.example'
                                        artifactId = 'flavor1Debug'
                                        version = '1.0'
                                    }
                                }
                                repositories {
                                    maven {
                                        url = "file://${'$'}buildDir/repo"
                                    }
                                }
                            }
                        }
                        """.trimIndent()
            }

            build(":Lib:assembleFlavor1Debug", ":Lib:publish") {
                assertTasksExecuted(":Lib:compileFlavor1DebugKotlin", ":Lib:publishFlavorDebugPublicationToMavenRepository")
                val pomLines =
                    subProject("Lib").projectPath.resolve("build/repo/com/example/flavor1Debug/1.0/flavor1Debug-1.0.pom").readLines()
                val stdlibVersionLineNumber = pomLines.indexOfFirst { "<artifactId>kotlin-stdlib</artifactId>" in it } + 1
                val versionLine = pomLines[stdlibVersionLineNumber]
                assertContains(versionLine, "<version>${buildOptions.kotlinVersion}</version>")
            }
        }
    }
}