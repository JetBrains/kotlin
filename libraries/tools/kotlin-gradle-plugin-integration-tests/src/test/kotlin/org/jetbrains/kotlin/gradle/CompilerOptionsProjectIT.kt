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

    @GradleTest
    @DisplayName("Jvm project target compiler options DSL override project level options")
    @JvmGradlePluginTests
    fun jvmOptionTarget(gradleVersion: GradleVersion) {
        project(
            "simpleProject",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    target.compilerOptions {
                |        javaParameters = true
                |        verbose = false
                |    }
                |    
                |    compilerOptions {
                |        javaParameters = false
                |        verbose = false
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
                |         freeCompilerArgs.add("-Xdebug")
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

                assert(compilationArgs.contains("-Xdebug")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-Xdebug': $compilationArgs"
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
                |         freeCompilerArgs.add("-Xdebug")
                |    }
                |    
                |    sourceSets.all {
                |        languageSettings {
                |            languageVersion = '1.9'
                |            apiVersion = '1.9'
                |            progressiveMode = false
                |            optIn("another.CustomOptInAnnotation")
                |            enableLanguageFeature("UnitConversionsOnArbitraryExpressions")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertCompilerArguments(
                    ":compileKotlin",
                    "-language-version 1.9",
                    "-api-version 1.9",
                    "-Xdebug",
                    "-opt-in my.custom.OptInAnnotation,another.CustomOptInAnnotation",
                    "-XXLanguage:+UnitConversionsOnArbitraryExpressions"
                )

                assertNoCompilerArgument(":compileKotlin", "-progressive")
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

                assertOutputDoesNotContain(
                    "w: :compileKotlin 'KotlinJvmCompile.moduleName' is deprecated, please migrate to 'compilerOptions.moduleName'!"
                )

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

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-java-parameters")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-java-parameters': $compilationArgs"
                }

                assert(compilationArgs.contains("-module-name my_app_debug")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-module-name my_app_debug': $compilationArgs"
                }
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
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG)
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
                    "-module-name my_app_debug"
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
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                logLevel = LogLevel.DEBUG
            ),
            buildJdk = jdk.location
        ) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |subprojects {
                |    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile.class).configureEach {
                |        compilerOptions {
                |            freeCompilerArgs.addAll("-progressive")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            subProject("libAndroid").buildGradle.appendText(
                //language=groovy
                """
                |
                |android {
                |    kotlinOptions {
                |        freeCompilerArgs += ["-opt-in=com.example.roo.requiresOpt.FunTests"]
                |    }
                |}
                """.trimMargin()
            )

            build(":libAndroid:compileDebugKotlin") {
                assertTasksExecuted(":libAndroid:compileDebugKotlin")

                assertCompilerArgument(":libAndroid:compileDebugKotlin", "-progressive")
                assertCompilerArgument(":libAndroid:compileDebugKotlin", "-opt-in=com.example.roo.requiresOpt.FunTests")
            }
        }
    }

    @DisplayName("KT-57688: task moduleName input overrides project level moduleName")
    @JvmGradlePluginTests
    @GradleTest
    fun moduleNameTaskOverrideProject(gradleVersion: GradleVersion) {
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
                |
                |tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile.class).configure {
                |    moduleName = "otherCustomModuleName"
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                assertTasksExecuted(":compileKotlin")

                assertOutputContains(
                    "w: :compileKotlin 'KotlinJvmCompile.moduleName' is deprecated, please migrate to 'compilerOptions.moduleName'!"
                )

                val compilationArgs = output.lineSequence().first { it.contains("Kotlin compiler args:") }

                assert(compilationArgs.contains("-module-name otherCustomModuleName")) {
                    printBuildOutput()
                    "Compiler arguments does not contain '-module-name otherCustomModuleName': $compilationArgs"
                }
            }
        }
    }

    @DisplayName("KT-57959: should be possible to configure module name in MPP/android")
    @GradleAndroidTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_70)
    @AndroidGradlePluginTests
    fun mppAndroidModuleName(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "multiplatformAndroidSourceSetLayout2",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion, logLevel = LogLevel.DEBUG),
            buildJdk = jdk.location
        ) {
            buildGradleKts.appendText(
                //language=kotlin
                """
                |
                |kotlin {
                |    android {
                |        compilations.all {
                |            compilerOptions.options.moduleName.set("last-chance")
                |        }
                |    }
                |}
                """.trimMargin()
            )

            build(":compileGermanFreeDebugKotlinAndroid") {
                assertTasksExecuted(":compileGermanFreeDebugKotlinAndroid")

                assertCompilerArgument(":compileGermanFreeDebugKotlinAndroid", "-module-name last-chance")
            }
        }
    }

    @GradleTest
    @DisplayName("Multiplatform compiler option DSL hierarchy")
    @JvmGradlePluginTests
    fun mppCompilerOptionsDsl(gradleVersion: GradleVersion) {
        project(
            projectName = "mpp-default-hierarchy",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.modify {
                it.substringBefore("kotlin {") +
                        //language=Groovy
                        """
                        |
                        |import org.jetbrains.kotlin.gradle.dsl.JvmTarget
                        |import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
                        |
                        |kotlin {
                        |    jvm {
                        |        compilerOptions {
                        |            languageVersion = KotlinVersion.KOTLIN_1_8
                        |            apiVersion = KotlinVersion.KOTLIN_1_8
                        |            jvmTarget.value(JvmTarget.JVM_11).disallowChanges()
                        |            javaParameters = true
                        |        }
                        |    }
                        |    
                        |    js {
                        |        compilerOptions {
                        |            languageVersion = KotlinVersion.KOTLIN_2_0
                        |            apiVersion = KotlinVersion.KOTLIN_2_0
                        |            friendModulesDisabled = true
                        |        }
                        |    }
                        |    
                        |    linuxX64 {
                        |        compilerOptions {
                        |            progressiveMode = true
                        |        }
                        |    }
                        |    
                        |    compilerOptions {
                        |         languageVersion = KotlinVersion.KOTLIN_1_7
                        |         apiVersion = KotlinVersion.KOTLIN_1_7
                        |    }    
                        |}
                        """.trimMargin()
            }

            build(":compileCommonMainKotlinMetadata") {
                assertCompilerArguments(":compileCommonMainKotlinMetadata", "-language-version 1.7", "-api-version 1.7")
            }

            build(":compileKotlinJvm") {
                assertCompilerArguments(
                    ":compileKotlinJvm",
                    "-language-version 1.8",
                    "-api-version 1.8",
                    "-java-parameters",
                    "-jvm-target 11"
                )
            }

            build(":compileKotlinJs") {
                assertCompilerArguments(":compileKotlinJs", "-language-version 2.0", "-api-version 2.0", "-Xfriend-modules-disabled")
            }

            build(":compileKotlinLinuxX64") {
                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlinLinuxX64", logLevel = LogLevel.DEBUG) {
                    assertCommandLineArgumentsContain("-language-version", "1.7")
                    assertCommandLineArgumentsContain("-api-version", "1.7")
                    assertCommandLineArgumentsContain("-progressive")
                }
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
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG , androidVersion = agpVersion),
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
                    "-module-name my-custom-module_germanFreeDebug"
                )
            }
        }
    }
}