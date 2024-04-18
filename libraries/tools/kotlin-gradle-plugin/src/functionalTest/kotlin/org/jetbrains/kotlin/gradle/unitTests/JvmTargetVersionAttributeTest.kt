/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.java.TargetJvmVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.withType
import kotlin.test.*

private typealias GradleJvmTarget = Int

class JvmTargetVersionAttributeTest : BaseAttributeTest<GradleJvmTarget>(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE) {
    val Project.kotlin
        get() = multiplatformExtension

    @Test
    fun testDefaultValueForExternalTarget() {
        buildProjectWithMPP().runLifecycleAwareTest {
            val target = createExternalTarget()
            checkJvmTarget(target, 8) // bug? the default value isn't picked up from the JVM toolchain
        }
    }

    @Test
    fun testCustomizedValueForExternalTarget() {
        buildProjectWithMPP().runLifecycleAwareTest {
            val target = createExternalTarget()
            tasks.withType<KotlinCompile>().configureEach {
                it.compilerOptions.jvmTarget.set(JvmTarget.JVM_9)
            }
            checkJvmTarget(target, 9) // 9 is never the default value during the test run as the Kotlin repo requires Java 11
        }
    }

    @Test
    fun testDefaultValueForJvm() {
        buildProjectWithMPP().runLifecycleAwareTest {
            kotlin.jvm()
            configurationResult.await()
            checkJvmTarget(kotlin.jvm(), defaultJvmTarget)
        }
    }

    @Test
    fun testDefaultValueForAndroidLibrary() {
        buildProjectWithMPP().runLifecycleAwareTest {
            androidLibrary {
                compileSdk = 34
            }
            kotlin.androidTarget()
            configurationResult.await()
            checkJvmTarget(kotlin.androidTarget(), defaultJvmTarget)
        }
    }

    @Test
    fun testCustomizedValueForJvm() {
        buildProjectWithMPP().runLifecycleAwareTest {
            kotlin.jvm()
            tasks.named<KotlinCompile>("compileKotlinJvm").configure {
                it.compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8) // 1.8 is never the default value during the test run as the Kotlin repo requires Java 11
                }
            }
            configurationResult.await()
            checkJvmTarget(kotlin.jvm(), 8)
        }
    }

    @Test
    fun testCustomizedValueForAndroidLibrary() {
        buildProjectWithMPP().runLifecycleAwareTest {
            androidLibrary {
                compileSdk = 34
            }
            kotlin.androidTarget()
            tasks.withType<KotlinCompile>().configureEach {
                it.compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8) // 1.8 is never the default value during the test run as the Kotlin repo requires Java 11
                }
            }
            configurationResult.await()
            checkJvmTarget(kotlin.androidTarget(), 8)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun testIsNotSetForUnrelatedTargets() {
        buildProjectWithMPP().runLifecycleAwareTest {
            kotlin.wasmJs()
            kotlin.wasmWasi()
            kotlin.js().nodejs()
            kotlin.js().browser()
            kotlin.linuxX64()
            configurationResult.await()
            assertNoAttribute(kotlin.wasmJs())
            assertNoAttribute(kotlin.wasmWasi())
            assertNoAttribute(kotlin.js())
            assertNoAttribute(kotlin.linuxX64())
        }
    }

    private fun checkJvmTarget(target: KotlinTarget, expectedValue: GradleJvmTarget) {
        assertAttributeEquals(
            target,
            expectedValue,
            usages = target.usages.filter { it.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name != Category.DOCUMENTATION }
                .toSet()
        )
        assertNoAttribute(
            target,
            usages = target.usages.filter { it.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name == Category.DOCUMENTATION }
                .toSet()
        )
    }

    private fun Project.createExternalTarget() = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }.apply {
        createCompilation<FakeCompilation> { defaults(kotlin) }
        createCompilation<FakeCompilation> {
            defaults(kotlin)
            compilationName = "fake"
            defaultSourceSet = kotlin.sourceSets.create("fake")
        }
    }

    companion object {
        val defaultJvmTarget = JavaVersion.current().majorVersion.toInt()
    }
}