/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Logging model in Gradle builds is not simple.
 *
 * On Gradle daemon level, there is GradleLogLevel.
 * On Kotlin compiler level, there is `versbose` flag.
 * Inside the compiler (particularly in the Incremental Compilation runner) there is CompilerLogLevel, generated based on the `verbose`.
 *
 * These tests assert some parts of the status quo. If you'd like to see it more streamlined,
 * consider voting for https://youtrack.jetbrains.com/issue/KT-64698
 *
 * P.S. this can be a functional test, but determining and using the "compiler log level" is part of the actual compile task.
 */

@DisplayName("Basic logging configuration - KMP")
@MppGradlePluginTests
class LoggingConfigurationMppIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("generic-kmp-app-plus-lib-with-tests")
    fun testBasicConfigurations(gradleVersion: GradleVersion) {
        nativeProject(
            "generic-kmp-app-plus-lib-with-tests",
            gradleVersion,
            configureSubProjects = true,
            // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
            buildOptions = defaultBuildOptions.disableIsolatedProjects(),
        ) {
            for (mainCompileTask in listOf(":lib:compileKotlinJvm", ":lib:compileKotlinJs")) {
                checkLoggingConfigurations("lib/build.gradle.kts", mainCompileTask, buildOptions)
            }
        }
    }
}

@DisplayName("Basic logging configuration - pure JVM project")
@JvmGradlePluginTests
class LoggingConfigurationJvmIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata("jvmTargetModernDsl")
    fun testBasicConfigurations(gradleVersion: GradleVersion) {
        project("jvmTargetModernDsl", gradleVersion) {
            checkLoggingConfigurations("build.gradle.kts", ":compileKotlin", defaultBuildOptions)
        }
    }
}

private fun TestProject.checkLoggingConfigurations(gradleKtsPath: String, mainCompileTask: String, defaultBuildOptions: BuildOptions) {
    val debugBuildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
    val infoBuildOptions = defaultBuildOptions.copy(logLevel = LogLevel.INFO)

    var currentVerboseSetting: String = "//insertable_at_kotlin_lvl"

    fun replaceVerboseSetting(newText: String) {
        projectPath.resolve(gradleKtsPath).replaceFirst(currentVerboseSetting, newText)
        currentVerboseSetting = newText
    }

    fun makeVerboseSetting(value: Boolean): String {
        return """tasks.named<KotlinCompilationTask<*>>("${
            mainCompileTask.split(":").last()
        }").configure { compilerOptions { verbose.set($value) } }"""
    }

    checkDebugLogPlusImplicitVerboseTrue(mainCompileTask, debugBuildOptions)

    checkInfoLogPlusImplicitVerboseFalse(mainCompileTask, infoBuildOptions)

    replaceVerboseSetting(makeVerboseSetting(false))
    checkDebugLogPlusExplicitVerboseFalse(mainCompileTask, debugBuildOptions)

    replaceVerboseSetting(makeVerboseSetting(true))
    checkInfoLogPlusExplicitVerboseTrue(mainCompileTask, infoBuildOptions)

    replaceVerboseSetting("//insertable_at_kotlin_lvl") // back to baseline with implicit "verbose" level
}

private fun TestProject.checkDebugLogPlusImplicitVerboseTrue(mainCompileTask: String, buildOptions: BuildOptions) {
    // KT-75850: we need to explicitly invalidate the CC here,as changing the logLevel doesn't trigger it
    build("clean", "-PinvalidateCC${generateIdentifier()}", mainCompileTask, buildOptions = buildOptions) {
        assertOutputContains("[DEBUG]")
        assertOutputContains("Kotlin compiler args:.*-verbose".toRegex())
        assertOutputContains("IncrementalCompilationOptions.*reportSeverity=3".toRegex())
    }
}

private fun TestProject.checkInfoLogPlusImplicitVerboseFalse(mainCompileTask: String, buildOptions: BuildOptions) {
    build("clean", mainCompileTask, buildOptions = buildOptions) {
        assertOutputDoesNotContain("[DEBUG]")
        assertOutputContains("Kotlin compiler args")
        assertOutputDoesNotContain("Kotlin compiler args:.*-verbose".toRegex())
        assertOutputContains("IncrementalCompilationOptions.*reportSeverity=2".toRegex())
    }
}

private fun TestProject.checkDebugLogPlusExplicitVerboseFalse(mainCompileTask: String, buildOptions: BuildOptions) {
    build("clean", mainCompileTask, buildOptions = buildOptions) {
        assertOutputContains("[DEBUG]")
        assertOutputContains("Kotlin compiler args")
        assertOutputDoesNotContain("Kotlin compiler args:.*-verbose".toRegex())
        assertOutputContains("IncrementalCompilationOptions.*reportSeverity=2".toRegex())
    }
}

private fun TestProject.checkInfoLogPlusExplicitVerboseTrue(mainCompileTask: String, buildOptions: BuildOptions) {
    build("clean", mainCompileTask, buildOptions = buildOptions) {
        assertOutputDoesNotContain("[DEBUG]")
        assertOutputContains("Kotlin compiler args:.*-verbose".toRegex())
        assertOutputContains("IncrementalCompilationOptions.*reportSeverity=3".toRegex())
    }
}
