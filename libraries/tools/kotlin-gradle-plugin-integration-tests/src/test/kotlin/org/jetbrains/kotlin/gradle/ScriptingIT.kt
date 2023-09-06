/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.relativeTo

@OsCondition(
    supportedOn = [OS.LINUX, OS.MAC, OS.WINDOWS],
    enabledOnCI = [OS.LINUX], // Compiler plugin is leaking file descriptor preventing cleaning the project on Windows
)
@DisplayName("Scripting plugin")
@OtherGradlePluginTests
@GradleTestVersions(maxVersion = TestVersions.Gradle.G_7_3) // workaround for a Gradle synchronization bug: https://github.com/gradle/gradle/issues/23450
abstract class ScriptingIT : KGPBaseTest() {

    @DisplayName("basic script is working")
    @GradleTest
    open fun testScripting(gradleVersion: GradleVersion) {
        project("scripting", gradleVersion) {
            val appSubProject = subProject("app")
            val scriptTemplateSubProject = subProject("script-template")
            appSubProject.disableLightTreeIfNeeded()
            scriptTemplateSubProject.disableLightTreeIfNeeded()
            build("assemble", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertCompiledKotlinSources(
                    listOf(
                        appSubProject.kotlinSourcesDir().resolve("world.greet.kts").relativeTo(projectPath),
                        scriptTemplateSubProject.kotlinSourcesDir().resolve("GreetScriptTemplate.kt").relativeTo(projectPath)
                    ),
                    output
                )
                assertFileExists(
                    appSubProject.kotlinClassesDir().resolve("World_greet.class")
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
    open fun testScriptingCustomExtensionIncremental(gradleVersion: GradleVersion) {
        testScriptingCustomExtensionImpl(gradleVersion, withIC = true)
    }

    private fun testScriptingCustomExtensionImpl(
        gradleVersion: GradleVersion,
        withIC: Boolean,
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
            appSubproject.disableLightTreeIfNeeded()
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

    open fun GradleProject.disableLightTreeIfNeeded() {

    }
}

@DisplayName("K1 Scripting plugin")
class ScriptingK1IT : ScriptingIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK1()
}

@DisplayName("K2 Scripting plugin")
class ScriptingK2IT : ScriptingIT() {
    override val defaultBuildOptions = super.defaultBuildOptions.copyEnsuringK2()

    @Disabled("KT-61137")
    override fun testScripting(gradleVersion: GradleVersion) {
        super.testScripting(gradleVersion)
    }

    @Disabled("KT-61137")
    override fun testScriptingCustomExtensionIncremental(gradleVersion: GradleVersion) {
        super.testScriptingCustomExtensionIncremental(gradleVersion)
    }

    override fun GradleProject.disableLightTreeIfNeeded() {
        buildGradle.append(
            """
            tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xuse-fir-lt=false") // Scripts are not yet supported with K2 in LightTree mode
                }
            }            
            """.trimIndent()
        )
    }
}