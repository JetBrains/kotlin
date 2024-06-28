/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName

@DisplayName("Tests for K/N builds with embeddable compiler jar")
@NativeGradlePluginTests
internal class NativeEmbeddableCompilerJarIT : KGPBaseTest() {

    private fun String.isRegularJar() = this.endsWith("/kotlin-native.jar")
    private fun String.isEmbeddableJar() = this.endsWith("/kotlin-native-compiler-embeddable.jar")

    private fun List<String>.includesRegularJar() = any { it.isRegularJar() }
    private fun List<String>.includesEmbeddableJar() = any { it.isEmbeddableJar() }

    private val String.withPrefix get() = "native-binaries/$this"

    @DisplayName("K/N with default config shouldn't contain kotlin-native.jar and should contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldNotUseRegularJarInDefaultConfig(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(":linkDebugExecutableHost") {
                assertNativeTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assert(!it.includesRegularJar()) {
                        assertionFailedMessage(it)
                    }
                    assert(it.includesEmbeddableJar()) {
                        assertionFailedMessage(it)
                    }
                }
            }
        }
    }

    @DisplayName("K/N with embeddable compiler flag turned off config should contain kotlin-native.jar and shouldn't contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldUseRegularJarWithoutUseEmbeddableCompilerJar(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=false",
            ) {
                assertNativeTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assert(it.includesRegularJar()) {
                        assertionFailedMessage(it)
                    }
                    assert(!it.includesEmbeddableJar()) {
                        assertionFailedMessage(it)
                    }
                }
            }
        }
    }

    @DisplayName("K/N with embeddable compiler flag turned on config shouldn't contain kotlin-native.jar and should contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldUseRegularJarWithUseEmbeddableCompilerJar(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=true",
            ) {
                assertNativeTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assert(!it.includesRegularJar()) {
                        assertionFailedMessage(it)
                    }
                    assert(it.includesEmbeddableJar()) {
                        assertionFailedMessage(it)
                    }
                }
            }
        }
    }

    @DisplayName("K/N project's builds with switching embeddable compiler flag turned from on to off")
    @GradleTest
    fun testSwitch(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(":linkDebugExecutableHost") {
                assertNativeTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assert(!it.includesRegularJar()) {
                        assertionFailedMessage(it)
                    }
                    assert(it.includesEmbeddableJar()) {
                        assertionFailedMessage(it)
                    }
                }
            }

            build(":linkDebugExecutableHost") {
                assertTasksUpToDate(":linkDebugExecutableHost", ":compileKotlinHost")
            }

            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=false",
            ) {
                assertTasksExecuted(":linkDebugExecutableHost", ":compileKotlinHost")
                assertNativeTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assert(it.includesRegularJar()) {
                        assertionFailedMessage(it)
                    }
                    assert(!it.includesEmbeddableJar()) {
                        assertionFailedMessage(it)
                    }
                }
            }
        }
    }

    private fun BuildResult.assertionFailedMessage(classPathValues: List<String>): String {
        printBuildOutput()
        return "Actual classpath is: ${classPathValues}"
    }
}