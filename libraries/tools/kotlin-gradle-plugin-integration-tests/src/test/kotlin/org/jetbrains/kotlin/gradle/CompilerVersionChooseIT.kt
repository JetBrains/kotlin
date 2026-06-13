/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.KOTLIN_BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

/**
 * The configuration-phase scenarios (default compiler version, choosing it via the `kotlin.compilerVersion`
 * extension) live in the functional test `CompilerVersionSelectionTest`. What remains here needs real dependency
 * resolution: verifying that a manually added `kotlin-compiler-embeddable` does not override the chosen version.
 */
@DisplayName("Tests for compiler version choosing via the Build Tools API")
@JvmGradlePluginTests
class CompilerVersionChooseIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(runViaBuildToolsApi = true)

    @GradleTest
    @DisplayName("Manual adding dependencies to the classpath configuration does not change kotlin version")
    fun manualAdding(gradleVersion: GradleVersion) {
        prepareProject(gradleVersion) {
            buildGradle.append(
                //language=Gradle
                """
                dependencies {
                    $KOTLIN_BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME("org.jetbrains.kotlin:kotlin-compiler-embeddable:${TestVersions.Kotlin.STABLE_RELEASE}")
                }
                """.trimIndent()
            )
            build(printClasspathTaskName) {
                assertCompilerVersion(buildOptions.kotlinVersion)
            }
        }
    }

    private val printClasspathTaskName = "printBuildToolsApiClasspath"

    private fun BuildResult.assertCompilerVersion(version: String) {
        val compilerDependencies = extractClasspathFromLogs()
            .filter { "kotlin-compiler-embeddable" in it }
            .toList()
        assert(compilerDependencies.isNotEmpty()) {
            "Expected dependencies to contain `kotlin-compiler-embeddable`"
        }
        val nonMatchingVersionDependencies = compilerDependencies
            .filter { !it.endsWith("$version.jar") }
        assert(nonMatchingVersionDependencies.isEmpty()) {
            "Kotlin dependencies expected to be of version '$version', the following dependencies do not pass the check: $nonMatchingVersionDependencies"
        }
    }

    private val classpathLogLinePrefix = "kotlin classpath: "

    private fun BuildResult.extractClasspathFromLogs() = output.lineSequence()
        .filter { it.startsWith(classpathLogLinePrefix) }
        .map { it.replaceFirst(classpathLogLinePrefix, "") }

    private fun prepareProject(gradleVersion: GradleVersion, test: TestProject.() -> Unit = {}) {
        project("simpleProject", gradleVersion) {
            buildGradle.append(
                //language=Gradle
                """
                tasks.register("$printClasspathTaskName") {
                    FileCollection classpath = configurations.$KOTLIN_BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
                    doFirst {
                        classpath.forEach {
                            println("$classpathLogLinePrefix" + it)
                        }
                    }
                }
                """.trimIndent()
            )
            test()
        }
    }
}