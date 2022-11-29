/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Tasks configuration avoidance")
class ConfigurationAvoidanceIT : KGPBaseTest() {

    @JvmGradlePluginTests
    @DisplayName("JVM unrelated tasks are not configured")
    @GradleTest
    fun testUnrelatedTaskNotConfigured(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            createTaskWithExpensiveConfiguration()

            build("compileKotlin")
        }
    }

    @AndroidGradlePluginTests
    @DisplayName("Android unrelated tasks are not configured")
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_42)
    @GradleAndroidTest
    fun testAndroidUnrelatedTaskNotConfigured(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = providedJdk.location
        ) {

            listOf("Android", "Test").forEach { subproject ->
                subProject(subproject)
                    .buildGradle
                    .append(
                        //language=Groovy
                        """
                        
                        android {
                            applicationVariants.all {
                                it.getAidlCompileProvider().configure {
                                    throw new RuntimeException("Task should not be configured.")
                                }
                            }
                        }
                        """.trimIndent()
                    )
            }

            subProject("Lib")
                .buildGradle
                .append(
                    //language=Groovy
                    """
                    
                    android {
                        libraryVariants.all {
                            it.getAidlCompileProvider().configure {
                                throw new RuntimeException("Task should not be configured.")
                            }
                        }
                    }
                    """.trimIndent()
                )

            build("help")
        }
    }

    @JsGradlePluginTests
    @DisplayName("JS unrelated tasks are not configured")
    @GradleTest
    fun jsNoTasksConfigured(gradleVersion: GradleVersion) {
        project("kotlin2JsNoOutputFileProject", gradleVersion) {
            createTaskWithExpensiveConfiguration()

            build("help")
        }
    }

    @MppGradlePluginTests
    @DisplayName("MPP unrelated tasks are not configured")
    @GradleTest
    fun mppNoTasksConfigured(gradleVersion: GradleVersion) {
        project("new-mpp-lib-and-app/sample-app", gradleVersion) {
            createTaskWithExpensiveConfiguration()

            build("help")
        }
    }

    private fun TestProject.createTaskWithExpensiveConfiguration(
        expensivelyConfiguredTaskName: String = "expensivelyConfiguredTask"
    ): String {
        @Suppress("GroovyAssignabilityCheck")
        buildGradle.append(
            //language=Groovy
            """
                    
                tasks.register("$expensivelyConfiguredTaskName") {
                    throw new GradleException("Should not configure expensive task!")
                }
                """.trimIndent()
        )

        return expensivelyConfiguredTaskName
    }

    @JvmGradlePluginTests
    @DisplayName("JVM early configuration resolution")
    @GradleTest
    fun testEarlyConfigurationsResolutionKotlin(gradleVersion: GradleVersion) {
        testEarlyConfigurationsResolution("kotlinProject", gradleVersion, kts = false)
    }

    @JsGradlePluginTests
    @DisplayName("JS early configuration resolution")
    @GradleTest
    fun testEarlyConfigurationsResolutionKotlinJs(gradleVersion: GradleVersion) {
        testEarlyConfigurationsResolution(
            "kotlin-js-browser-project",
            gradleVersion,
            kts = true,
            buildOptions = defaultBuildOptions.copy(
                // bug in Gradle: https://github.com/gradle/gradle/issues/15796
                warningMode = if (gradleVersion < GradleVersion.version("7.0")) WarningMode.Summary else defaultBuildOptions.warningMode
            )
        )
    }

    private fun testEarlyConfigurationsResolution(
        projectName: String,
        gradleVersion: GradleVersion,
        kts: Boolean,
        buildOptions: BuildOptions = defaultBuildOptions
    ) = project(projectName, gradleVersion, buildOptions = buildOptions) {
        (if (kts) buildGradleKts else buildGradle).appendText(
            //language=Gradle
            """${'\n'}
            // KT-45834 start
            ${if (kts) "var" else "def"} ready = false
            gradle.taskGraph.whenReady {
                println("Task Graph Ready")
                ready = true
            }

            allprojects {
                configurations.forEach { configuration ->
                    configuration.incoming.beforeResolve {
                        println("Resolving ${'$'}configuration")
                        if (!ready) {
                            throw ${if (kts) "" else "new"} GradleException("${'$'}configuration is being resolved at configuration time")
                        }
                    }
                }
            }
            // KT-45834 end
            """.trimIndent()
        )

        build(
            "assemble",
            "-m"
        )
    }
}