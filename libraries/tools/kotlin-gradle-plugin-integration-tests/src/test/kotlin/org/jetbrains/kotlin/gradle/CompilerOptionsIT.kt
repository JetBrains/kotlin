/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.parseCompilerArguments
import org.jetbrains.kotlin.gradle.util.parseCompilerArgumentsFromBuildOutput
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.test.assertEquals
import kotlin.test.fail

internal class CompilerOptionsIT : KGPBaseTest() {

    // In Gradle 7.3-8.0 'kotlin-dsl' plugin tries to set up freeCompilerArgs in doFirst task action
    // Related issue: https://github.com/gradle/gradle/issues/22091
    @DisplayName("Allows to set kotlinOptions.freeCompilerArgs on task execution with warning")
    @JvmGradlePluginTests
    @GradleTestVersions(
        minVersion = TestVersions.Gradle.G_7_3,
        // In Gradle 8.0 there is logic to filter logger messages that contain compiler options configured by `kotlin-dsl` plugin
        // https://github.com/gradle/gradle/blob/master/subprojects/kotlin-dsl-plugins/src/main/kotlin/org/gradle/kotlin/dsl/plugins/dsl/KotlinDslCompilerPlugins.kt#L70-L73
        maxVersion = TestVersions.Gradle.G_7_6,
    )
    @GradleTest
    internal fun compatibleWithKotlinDsl(gradleVersion: GradleVersion) {
        project("buildSrcWithKotlinDslAndKgp", gradleVersion) {
            gradleProperties
                .appendText(
                    """
                |
                |systemProp.org.gradle.kotlin.dsl.precompiled.accessors.strict=true
                """.trimMargin()
                )

            build("tasks") {
                assertOutputContains("kotlinOptions.freeCompilerArgs were changed on task :compileKotlin execution phase:")
            }
        }
    }

    @DisplayName("Allow to suppress kotlinOptions.freeCompilerArgs on task execution modification warning")
    @JvmGradlePluginTests
    @GradleTest
    internal fun suppressFreeArgsModification(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                """
                |
                |tasks.named("compileKotlin") {
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-module-name=java"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                """.trimMargin()
            )

            build("assemble") {
                assertOutputDoesNotContain("kotlinOptions.freeCompilerArgs were changed on task")
            }
        }
    }

    @DisplayName("compiler plugin arguments set via kotlinOptions.freeCompilerArgs on task execution applied properly")
    @JvmGradlePluginTests
    @GradleTest
    internal fun freeArgsModifiedAtExecutionTimeCorrectly(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            buildGradle.appendText(
                //language=Gradle
                """
                |
                |tasks.named("compileKotlin") {
                |    kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah1=1"]
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah2=1", "-P", "plugin:blah-blah:blah-blah3=1"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                //language=properties
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                """.trimMargin()
            )

            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContainsExactlyTimes("-P plugin:blah-blah:", 3)
            }
        }
    }

    @DisplayName("compiler plugin arguments set via kotlinOptions.freeCompilerArgs on task execution applied properly in MPP")
    @MppGradlePluginTests
    @GradleTest
    internal fun freeArgsModifiedAtExecutionTimeCorrectlyMpp(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
            buildGradle.appendText(
                //language=Gradle
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile).configureEach {
                |    kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah1=1"]
                |    doFirst {
                |        kotlinOptions.freeCompilerArgs += ["-P", "plugin:blah-blah:blah-blah2=1", "-P", "plugin:blah-blah:blah-blah3=1"]
                |    }
                |}
                """.trimMargin()
            )

            gradleProperties.appendText(
                //language=properties
                """
                |
                |kotlin.options.suppressFreeCompilerArgsModificationWarning=true
                """.trimMargin()
            )

            val compileTasks = listOf(
                "compileCommonMainKotlinMetadata",
                "compileKotlinJvmWithoutJava",
                "compileKotlinJs",
                // we do not allow modifying free args for K/N at execution time
            )
            build(*compileTasks.toTypedArray(), buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertOutputContainsExactlyTimes("-P plugin:blah-blah:", 3 * compileTasks.size) // 3 times per task
            }
        }
    }

    @DisplayName("Should pass -opt-in from compiler options DSL")
    @JvmGradlePluginTests
    @GradleTest
    fun passesOptInAnnotation(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.optIn.addAll("kotlin.RequiresOptIn", "my.CustomOptIn")
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                val compilerArgs = output
                    .lineSequence()
                    .first {
                        it.contains("Kotlin compiler args:")
                    }
                    .substringAfter("Kotlin compiler args:")

                val expectedOptIn = "-opt-in kotlin.RequiresOptIn,my.CustomOptIn"
                assert(compilerArgs.contains(expectedOptIn)) {
                    printBuildOutput()
                    "compiler arguments does not contain '$expectedOptIn' - actual value: $compilerArgs"
                }
            }
        }
    }

    @DisplayName("Should combine -opt-in arguments from languageSettings DSL")
    @MppGradlePluginTests
    @GradleTest
    fun combinesOptInFromLanguageSettings(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    sourceSets {
                |        jvm6Main {
                |            languageSettings.optIn("my.custom.OptInAnnotation")
                |        }
                |    }
                |}
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.optIn.add("another.custom.UnderOptIn")
                |}
                """.trimMargin()
            )

            build("compileKotlinJvm6") {
                assertTasksExecuted(":compileKotlinJvm6")
                assert(output.contains("-opt-in my.custom.OptInAnnotation,another.custom.UnderOptIn")) {
                    printBuildOutput()
                    "Output does not contain '-opt-in my.custom.OptInAnnotation,another.custom.UnderOptIn'!"
                }
            }
        }
    }

    @DisplayName("Should combine -opt-in arguments from languageSettings DSL for Native")
    @MppGradlePluginTests
    @GradleTest
    fun combinesOptInFromLanguageSettingsNative(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            // We need to get specific task output as commonizer may run first producing
            // arguments as well in output
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    sourceSets {
                |        nativeMain {
                |            languageSettings.optIn("my.custom.OptInAnnotation")
                |        }
                |        linux64Main {
                |            languageSettings.optIn("my.custom.OptInAnnotation")
                |        }
                |        macos64Main {
                |            languageSettings.optIn("my.custom.OptInAnnotation")
                |        }
                |        macosArm64Main {
                |            languageSettings.optIn("my.custom.OptInAnnotation")
                |        }
                |    }
                |}
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.optIn.add("another.custom.UnderOptIn")
                |}
                """.trimMargin()
            )

            build("compileNativeMainKotlinMetadata") {
                assertTasksExecuted(":compileNativeMainKotlinMetadata")
                val taskOutput = getOutputForTask(":compileNativeMainKotlinMetadata")
                val arguments = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, taskOutput)
                assertEquals(
                    setOf("another.custom.UnderOptIn", "my.custom.OptInAnnotation"), arguments.optIn?.toSet(),
                    "Arguments optIn does not match '-opt-in=another.custom.UnderOptIn, -opt-in=my.custom.OptInAnnotation'"
                )
            }

            build("compileKotlinLinux64") {
                assertTasksExecuted(":compileKotlinLinux64")
                val taskOutput = getOutputForTask(":compileKotlinLinux64")
                val arguments = parseCompilerArgumentsFromBuildOutput(K2NativeCompilerArguments::class, taskOutput)
                assertEquals(
                    setOf("another.custom.UnderOptIn", "my.custom.OptInAnnotation"), arguments.optIn?.toSet(),
                    "Arguments optIn does not match '-opt-in=another.custom.UnderOptIn, -opt-in=my.custom.OptInAnnotation'"
                )
            }
        }
    }

    @DisplayName("Should pass -opt-in from compiler options DSL in native project")
    @NativeGradlePluginTests
    @GradleTest
    fun passesOptInAnnotationNative(gradleVersion: GradleVersion) {
        nativeProject(
            projectName = "native-link-simple",
            gradleVersion = gradleVersion,
        ) {
            buildGradle.appendText(
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.optIn.addAll("kotlin.RequiresOptIn", "my.CustomOptIn")
                |}
                """.trimMargin()
            )

            build("compileKotlinHost", forceOutput = true) {
                val expectedOptIn = listOf("kotlin.RequiresOptIn", "my.CustomOptIn")
                val arguments = parseCompilerArguments<K2NativeCompilerArguments>()
                if (arguments.optIn?.toList() != listOf("kotlin.RequiresOptIn", "my.CustomOptIn")) {
                    fail(
                        "compiler arguments does not contain expected optIns'${expectedOptIn.joinToString()}': ${arguments.optIn}"
                    )
                }
            }
        }
    }

    @DisplayName("Should pass -progressive from compiler options DSL")
    @JvmGradlePluginTests
    @GradleTest
    fun passesProgressive(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.progressiveMode.set(true)
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                val compilerArgs = output
                    .lineSequence()
                    .first {
                        it.contains("Kotlin compiler args:")
                    }
                    .substringAfter("Kotlin compiler args:")

                val expectedArg = "-progressive"
                assert(compilerArgs.contains(expectedArg)) {
                    printBuildOutput()
                    "compiler arguments does not contain '$expectedArg' - actual value: $compilerArgs"
                }
            }
        }
    }

    @DisplayName("Should not pass -progressive by default from compiler options DSL")
    @JvmGradlePluginTests
    @GradleTest
    fun notPassesDefaultProgressive(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            build("compileKotlin") {
                val compilerArgs = output
                    .lineSequence()
                    .first {
                        it.contains("Kotlin compiler args:")
                    }
                    .substringAfter("Kotlin compiler args:")

                val expectedArg = "-progressive"
                assert(!compilerArgs.contains(expectedArg)) {
                    printBuildOutput()
                    "compiler arguments contains '$expectedArg' - actual value: $compilerArgs"
                }
            }
        }
    }

    @DisplayName("Should pass -progressive from languageSettings if compiler options DSL is not configured")
    @JvmGradlePluginTests
    @GradleTest
    fun passesProgressiveFromLanguageSettings(gradleVersion: GradleVersion) {
        project(
            projectName = "simpleProject",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin.sourceSets.all {
                |    languageSettings {
                |        progressiveMode = true
                |    }
                |}
                """.trimMargin()
            )

            build("compileKotlin") {
                val compilerArgs = output
                    .lineSequence()
                    .first {
                        it.contains("Kotlin compiler args:")
                    }
                    .substringAfter("Kotlin compiler args:")

                val expectedArg = "-progressive"
                assert(compilerArgs.contains(expectedArg)) {
                    printBuildOutput()
                    "compiler arguments does not contain '$expectedArg' - actual value: $compilerArgs"
                }
            }
        }
    }

    @DisplayName("Should pass -progressive from compiler options DSL in native project")
    @NativeGradlePluginTests
    @GradleTest
    fun passesProgressiveModeNative(gradleVersion: GradleVersion) {
        nativeProject(
            projectName = "native-link-simple",
            gradleVersion = gradleVersion,
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).configureEach {
                |    compilerOptions.progressiveMode.set(true)
                |}
                """.trimMargin()
            )

            build("compileKotlinHost") {
                val expectedArg = "-progressive"
                val compilerArgs = output
                    .substringAfter("Arguments = [")
                    .substringBefore("]")
                    .lines()
                val progressiveArg = compilerArgs.find { it.trim() == expectedArg }

                assert(progressiveArg != null) {
                    printBuildOutput()
                    "compiler arguments does not contain '$expectedArg': ${compilerArgs.joinToString()}"
                }
            }
        }
    }

    @DisplayName("KT-57823: should be possible to configure native module name via compilation")
    @NativeGradlePluginTests
    @GradleTest
    fun passesModuleNameFromNativeCompilation(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            // We need to get specific task output as commonizer may run first producing
            // arguments as well in output
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.appendText(
                //language=Groovy
                """
                |
                |kotlin {
                |    targets {
                |        named("linux64", org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget.class) {
                |            compilations.all {
                |                compilerOptions.options.moduleName.set("i-am-your-module-name")
                |            }
                |        }
                |    }
                |}
                |
                """.trimMargin()
            )

            build(":compileNativeMainKotlinMetadata") {
                assertTasksExecuted(":compileNativeMainKotlinMetadata")

                extractNativeTasksCommandLineArgumentsFromOutput(":compileNativeMainKotlinMetadata") {
                    assertCommandLineArgumentsContain("-module-name", "com.example:sample-lib_nativeMain")
                }
            }

            build(":compileKotlinLinux64") {
                assertTasksExecuted(":compileKotlinLinux64")

                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlinLinux64") {
                    assertCommandLineArgumentsContain("-module-name", "i-am-your-module-name")
                }
            }
        }
    }

    @DisplayName("KT-57823: uses archivesName value for native compilation module name convention")
    @NativeGradlePluginTests
    @GradleTest
    fun nativeCompilationModuleNameConvention(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            // We need to get specific task output as commonizer may run first producing
            // arguments as well in output
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            buildGradle.modify {
                val buildScript = """
                |${it.substringBefore("apply plugin:")}
                |apply plugin: 'base'
                |apply plugin: ${it.substringAfter("apply plugin:")}
                |
                """.trimMargin()
                if (gradleVersion < GradleVersion.version("7.1")) {
                    "$buildScript\narchivesBaseName = \"myNativeLib\""
                } else {
                    "$buildScript\nbase.archivesName.set(\"myNativeLib\")"
                }
            }

            build(":compileNativeMainKotlinMetadata") {
                assertTasksExecuted(":compileNativeMainKotlinMetadata")

                extractNativeTasksCommandLineArgumentsFromOutput(":compileNativeMainKotlinMetadata") {
                    assertCommandLineArgumentsContain("-module-name", "com.example:myNativeLib_nativeMain")
                }
            }

            build(":compileKotlinLinux64") {
                assertTasksExecuted(":compileKotlinLinux64")

                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlinLinux64") {
                    assertCommandLineArgumentsContain("-module-name", "com.example:myNativeLib")
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Syncs languageSettings changes to the related compiler options")
    @MppGradlePluginTests
    fun syncLanguageSettingsToCompilerOptions(gradleVersion: GradleVersion) {
        project("mpp-default-hierarchy", gradleVersion) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |kotlin.sourceSets.configureEach {
                |    languageSettings.apiVersion = "1.7"
                |    languageSettings.languageVersion = "1.8"
                |}
                |
                |tasks.register("printCompilerOptions") {
                |    dependsOn(kotlinTaskToCheck)
                |    def tasksContainer = project.tasks
                |    doLast {
                |        def kotlinTask = tasks.getByName(kotlinTaskToCheck) as org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<?>
                |        logger.warn("###AV:${'$'}{kotlinTask.compilerOptions.apiVersion.getOrNull()}")
                |        logger.warn("###LV:${'$'}{kotlinTask.compilerOptions.languageVersion.getOrNull()}")
                |    }
                |}
                """.trimMargin()
            )

            listOf(
                "compileCommonMainKotlinMetadata",
                "compileKotlinJvm",
                "compileNativeMainKotlinMetadata",
                "compileLinuxMainKotlinMetadata",
                "compileAppleMainKotlinMetadata",
                "compileIosMainKotlinMetadata",
                "compileKotlinLinuxX64",
                "compileKotlinLinuxArm64",
                "compileKotlinIosX64",
                "compileKotlinIosArm64"
            ).forEach { task ->
                build("printCompilerOptions", "-PkotlinTaskToCheck=$task") {
                    assertOutputContains("###AV:KOTLIN_1_7")
                    assertOutputContains("###LV:KOTLIN_1_8")
                }
            }
        }
    }

    @GradleTest
    @DisplayName("Syncs compiler option changes to the related language settings")
    @MppGradlePluginTests
    fun syncCompilerOptionsToLanguageSettings(gradleVersion: GradleVersion) {
        project("mpp-default-hierarchy", gradleVersion) {
            buildGradle.appendText(
                //language=groovy
                """
                |
                |tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask.class).all {
                |    compilerOptions {
                |        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7
                |        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_8
                |    }
                |}
                |
                |tasks.register("printLanguageSettingsOptions") {
                |    doLast {
                |        def languageSettings = kotlin.sourceSets.getByName(kotlinSourceSet).languageSettings
                |        logger.warn("")
                |        logger.warn("###AV:${'$'}{languageSettings.apiVersion}")
                |        logger.warn("###LV:${'$'}{languageSettings.languageVersion}")
                |    }
                |}
                """.trimMargin()
            )

            listOf(
                "commonMain",
                "jvmMain",
                "nativeMain",
                "linuxMain",
                "appleMain",
                "iosMain",
                "linuxX64Main",
                "linuxArm64Main",
                "iosX64Main",
                "iosArm64Main",
            ).forEach { sourceSet ->
                build("printLanguageSettingsOptions", "-PkotlinSourceSet=${sourceSet}") {
                    assertOutputContains("###AV:1.7")
                    assertOutputContains("###LV:1.8")
                }
            }
        }
    }
}
