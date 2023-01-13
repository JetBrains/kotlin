/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.relativeTo

@DisabledOnOs(
    OS.WINDOWS,
    disabledReason = "Compiler plugin is leaking file descriptor preventing cleaning the project"
)
@DisplayName("Scripting plugin")
@OtherGradlePluginTests
@GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_3) // workaround for a Gradle synchronization bug: https://github.com/gradle/gradle/issues/23450
class ScriptingIT : KGPBaseTest() {

    @DisplayName("basic script is working")
    @GradleTest
    fun testScripting(gradleVersion: GradleVersion) {
        project("scripting", gradleVersion) {
            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertCompiledKotlinSources(
                    listOf(
                        subProject("app").kotlinSourcesDir().resolve("world.greet.kts").relativeTo(projectPath),
                        subProject("script-template").kotlinSourcesDir().resolve("GreetScriptTemplate.kt").relativeTo(projectPath)
                    ),
                    output
                )
                assertFileExists(
                    subProject("app").kotlinClassesDir().resolve("World_greet.class")
                )
            }
        }
    }

    @DisplayName("With custom file extension compiled non-incremental")
    @GradleTest
    fun testScriptingCustomExtensionNonIncremental(gradleVersion: GradleVersion) {
        testScriptingCustomExtensionImpl(gradleVersion, withIC = false)
    }

    @DisplayName("With custom file extension compiled incremental")
    @GradleTest
    fun testScriptingCustomExtensionIncremental(gradleVersion: GradleVersion) {
        testScriptingCustomExtensionImpl(gradleVersion, withIC = true)
    }

    private fun testScriptingCustomExtensionImpl(
        gradleVersion: GradleVersion,
        withIC: Boolean
    ) {
        project(
            "scriptingCustomExtension",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                incremental = withIC,
                logLevel = if (withIC) LogLevel.DEBUG else defaultBuildOptions.logLevel
            )
        ) {
            val appSubproject = subProject("app")
            val bobGreetSource = appSubproject.kotlinSourcesDir().resolve("bob.greet")
            val bobGreet = bobGreetSource.relativeTo(projectPath)
            val aliceGreet = appSubproject.kotlinSourcesDir().resolve("alice.greet").relativeTo(projectPath)
            val worldGreet = appSubproject.kotlinSourcesDir().resolve("world.greet").relativeTo(projectPath)
            val greetScriptTemplateKt = subProject("script-template")
                .kotlinSourcesDir()
                .resolve("GreetScriptTemplate.kt")
                .relativeTo(projectPath)

            build("assemble") {
                val classesDir = appSubproject.kotlinClassesDir()
                assertFileExists(classesDir.resolve("World.class"))
                assertFileExists(classesDir.resolve("Alice.class"))
                assertFileExists(classesDir.resolve("Bob.class"))

                if (withIC) {
                    // compile iterations are not logged when IC is disabled
                    assertCompiledKotlinSources(
                        listOf(bobGreet, aliceGreet, worldGreet, greetScriptTemplateKt),
                        output
                    )
                }
            }

            bobGreetSource.modify { it.replace("Bob", "Uncle Bob") }
            build("assemble") {
                if (withIC) {
                    assertCompiledKotlinSources(listOf(bobGreet), output)
                }
            }
        }
    }

    @DisplayName("KT-31124: No scripting warning")
    @GradleTest
    fun testNoScriptingWarning(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            build("help") {
                assertOutputDoesNotContain(ScriptingGradleSubplugin.MISCONFIGURATION_MESSAGE_SUFFIX)
            }
        }
    }

}