/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Project level compiler options DSL")
class CompilerOptionsProjectIT : KGPBaseTest() {

    @DisplayName("Jvm project compiler options are passed to compilation")
    @JvmGradlePluginTests
    @GradleTest
    fun jvmProjectLevelOptions(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    compilerOptions {
                |         javaParameters = true
                |         verbose = false
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-java-parameters")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-progressive': $compilationArgs"
                }

                // '-verbose' by default will be set to 'true' by debug log level
                assert(!compilationArgs.contains("-verbose")) {
                    printBuildOutput()
                    "Compiler arguments contains '-verbose': $compilationArgs"
                }
            }
        }
    }

    @DisplayName("languageSettings should not override project options when not configured")
    @JvmGradlePluginTests
    @GradleTest
    fun nonConfiguredLanguageSettings(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    compilerOptions {
                |         languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
                |         apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
                |         progressiveMode = true
                |         optIn.add("my.custom.OptInAnnotation")
                |         freeCompilerArgs.add("-Xuse-ir")
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-language-version 2.0")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-language-version 2.0': $compilationArgs"
                }

                assert(compilationArgs.contains("-api-version 2.0")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-api-version 2.0': $compilationArgs"
                }

                assert(compilationArgs.contains("-progressive")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-progressive': $compilationArgs"
                }

                assert(compilationArgs.contains("-opt-in my.custom.OptInAnnotation")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-opt-in my.custom.OptInAnnotation': $compilationArgs"
                }

                assert(compilationArgs.contains("-Xuse-ir")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-Xuse-ir': $compilationArgs"
                }
            }
        }
    }

    @DisplayName("languageSettings override project options when configured")
    @JvmGradlePluginTests
    @GradleTest
    fun configuredLanguageSettings(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    compilerOptions {
                |         languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
                |         apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
                |         progressiveMode = true
                |         optIn.add("my.custom.OptInAnnotation")
                |         freeCompilerArgs.add("-Xuse-ir")
                |    }
                |    
                |    sourceSets.all {
                |        languageSettings {
                |            languageVersion = '1.9'
                |            apiVersion = '1.9'
                |            progressiveMode = false
                |            optInAnnotationsInUse.add("another.CustomOptInAnnotation")
                |            enableLanguageFeature("UnitConversionsOnArbitraryExpressions")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-language-version 1.9")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-language-version 1.9': $compilationArgs"
                }

                assert(compilationArgs.contains("-api-version 1.9")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-api-version 2.0': $compilationArgs"
                }

                assert(!compilationArgs.contains("-progressive")) {
                    printBuildOutput()
                    "Compiler arguments contains '-progressive': $compilationArgs"
                }

                assert(compilationArgs.contains("-opt-in another.CustomOptInAnnotation")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-opt-in another.CustomOptInAnnotation': $compilationArgs"
                }

                assert(compilationArgs.contains("-XXLanguage:+UnitConversionsOnArbitraryExpressions")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-XXLanguage:+UnitConversionsOnArbitraryExpressions': $compilationArgs"
                }
            }
        }
    }

    @DisplayName("moduleName overrides compilation moduleName")
    @JvmGradlePluginTests
    @GradleTest
    fun moduleNameProject(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    compilerOptions {
                |         moduleName = "customModule"
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-module-name customModule")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-module-name customModule': $compilationArgs"
                }
            }
        }
    }

    @DisplayName("Project level DSL is available in android project")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun androidProject(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildJdk = jdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |   compilerOptions {
                |       javaParameters = true
                |   }
                |}
                """.trimMargin()
            )

            build("compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-java-parameters")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-java-parameters': $compilationArgs"
                }
            }
        }
    }
}