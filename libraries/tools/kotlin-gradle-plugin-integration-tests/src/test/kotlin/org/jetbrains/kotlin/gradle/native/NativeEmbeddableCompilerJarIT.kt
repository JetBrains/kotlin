/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.native.GeneralNativeIT.Companion.withNativeCompilerClasspath
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeEmbeddableCompilerJarIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    private fun String.isRegularJar() = this.endsWith("/kotlin-native.jar")
    private fun String.isEmbeddableJar() = this.endsWith("/kotlin-native-compiler-embeddable.jar")

    private fun List<String>.includesRegularJar() = any { it.isRegularJar() }
    private fun List<String>.includesEmbeddableJar() = any { it.isEmbeddableJar() }

    @Test
    fun testDefault() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        build(":linkDebugExecutableHost") {
            assertSuccessful()
            withNativeCompilerClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                assertFalse(it.includesRegularJar())
                assertTrue(it.includesEmbeddableJar())
            }
        }
    }

    @Test
    fun testEmbeddableJarFalse() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        build(":linkDebugExecutableHost", "-Pkotlin.native.useEmbeddableCompilerJar=false") {
            assertSuccessful()
            withNativeCompilerClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                assertTrue(it.includesRegularJar())
                assertFalse(it.includesEmbeddableJar())
            }
        }
    }

    @Test
    fun testEmbeddableJarTrue() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        build(":linkDebugExecutableHost", "-Pkotlin.native.useEmbeddableCompilerJar=true") {
            assertSuccessful()
            withNativeCompilerClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                assertFalse(it.includesRegularJar())
                assertTrue(it.includesEmbeddableJar())
            }
        }
    }

    @Test
    fun testSwitch() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        build(":linkDebugExecutableHost") {
            assertSuccessful()
            withNativeCompilerClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                assertFalse(it.includesRegularJar())
                assertTrue(it.includesEmbeddableJar())
            }
        }

        build(":linkDebugExecutableHost") {
            assertTasksUpToDate(":linkDebugExecutableHost", ":compileKotlinHost")
        }

        build(":linkDebugExecutableHost", "-Pkotlin.native.useEmbeddableCompilerJar=false") {
            assertSuccessful()
            assertTasksExecuted(":linkDebugExecutableHost", ":compileKotlinHost")
            withNativeCompilerClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                assertTrue(it.includesRegularJar())
                assertFalse(it.includesEmbeddableJar())
            }
        }
    }
}
