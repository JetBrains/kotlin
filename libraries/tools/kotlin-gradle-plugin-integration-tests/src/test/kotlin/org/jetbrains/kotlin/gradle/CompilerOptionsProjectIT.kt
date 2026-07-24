/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinAndroidExtensionOrNull
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

/**
 * The single-platform JVM project-level compiler-options scenarios live in the functional test
 * `CompilerOptionsProjectTest` (configuration-phase argument checks); the multiplatform DSL hierarchy is covered by
 * `ProjectCompilerOptionsTests`. What remains here is Android-specific (needs AGP).
 */
@DisplayName("Project level compiler options DSL")
class CompilerOptionsProjectIT : KGPBaseTest() {

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
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |   compilerOptions {
                |       javaParameters = true
                |       moduleName = "my_app"
                |   }
                |}
                """.trimMargin()
            )

            build("compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")

                assertOutputDoesNotContain(
                    "w: :compileKotlin 'KotlinJvmCompile.moduleName' is deprecated, please migrate to 'compilerOptions.moduleName'!"
                )
                assertCompilerArguments(
                    ":compileDebugKotlin",
                    "-java-parameters", "-module-name my_app_debug",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("Android target compiler options override project level compiler options")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun androidProjectTargetOverrideProjectOptions(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidSimpleApp",
            gradleVersion,
            buildJdk = jdk.location,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |   target.compilerOptions {
                |       javaParameters = true
                |       moduleName = "my_app"
                |   }
                |   
                |   compilerOptions {
                |       javaParameters = false
                |       moduleName = "other_app"
                |   }
                |}
                """.trimMargin()
            )

            build("compileDebugKotlin") {
                assertTasksExecuted(":compileDebugKotlin")

                assertOutputDoesNotContain(
                    "w: :compileKotlin 'KotlinJvmCompile.moduleName' is deprecated, please migrate to 'compilerOptions.moduleName'!"
                )

                assertCompilerArguments(
                    ":compileDebugKotlin",
                    "-java-parameters",
                    "-module-name my_app_debug",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("KT-59056: freeCompilerArgs are combined with android.kotlinOptions.freeCompilerArgs")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun kotlinOptionsFreeCompilerArgs(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "AndroidIncrementalMultiModule",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdk.location
        ) {
            subprojects("app", "libAndroid", "libAndroidClassesOnly", "libJvmClassesOnly").buildScriptInjection {
                project.tasks.withType<KotlinCompile>().configureEach { task ->
                    task.compilerOptions.freeCompilerArgs.add("-progressive")
                }
                if (project.name == "libAndroid") {
                    project.kotlinAndroidExtensionOrNull?.compilerOptions?.freeCompilerArgs?.add(
                        "-opt-in=com.example.roo.requiresOpt.FunTests"
                    )
                }
            }

            build(":libAndroid:compileDebugKotlin") {
                assertTasksExecuted(":libAndroid:compileDebugKotlin")

                assertCompilerArgument(
                    ":libAndroid:compileDebugKotlin",
                    "-progressive",
                    logLevel = LogLevel.INFO
                )
                assertCompilerArgument(
                    ":libAndroid:compileDebugKotlin",
                    "-opt-in=com.example.roo.requiresOpt.FunTests",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("KT-57959: should be possible to configure module name in MPP/android")
    @GradleAndroidTest
    @AndroidGradlePluginTests
    fun mppAndroidModuleName(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "multiplatformAndroidSourceSetLayout2",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdk.location
        ) {
            buildGradleKts.appendText(
                //language=kotlin
                """
                |
                |kotlin {
                |    androidTarget {
                |        compilations.all {
                |            compilerOptions.options.moduleName.set("last-chance")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build(":compileGermanFreeDebugKotlinAndroid") {
                assertTasksExecuted(":compileGermanFreeDebugKotlinAndroid")

                assertCompilerArgument(
                    ":compileGermanFreeDebugKotlinAndroid",
                    "-module-name last-chance",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }

    @DisplayName("KT-61303: Multiplatform/Android module name is changed")
    @AndroidGradlePluginTests
    @GradleAndroidTest
    fun mppAndroidModuleNameCompilerOptionsDsl(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "multiplatformAndroidSourceSetLayout2",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = jdk.location
        ) {
            buildGradleKts.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    androidTarget {
                |        compilerOptions {
                |            moduleName.set("my-custom-module")
                |        }
                |    }
                |}
                |
                """.trimMargin()
            )

            build(":compileGermanFreeDebugKotlinAndroid") {
                assertCompilerArguments(
                    ":compileGermanFreeDebugKotlinAndroid",
                    "-module-name my-custom-module_germanFreeDebug",
                    logLevel = LogLevel.INFO
                )
            }
        }
    }
}
