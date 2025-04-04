/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import kotlin.test.Test
import kotlin.test.assertEquals

private const val STABLE_VERSION = "2.0.20"

class KotlinNativeToolchainTest {

    @Test
    fun `check that kotlin native compiler stable version has been resolved correctly`() {
        val project = buildProjectWithMPP(preApplyCode = {
            setUpKotlinNativeToolchainWithStableVersion()
        }) {
            configureRepositoriesForTests()
            project.multiplatformExtension.linuxX64()
        }

        project.evaluate()

        val compileTask = project.tasks.withType(KotlinNativeCompile::class.java).first()

        assertEquals(
            "kotlin-native-prebuilt-${HostManager.platformName()}-$STABLE_VERSION",
            (compileTask.kotlinNativeProvider.get() as KotlinNativeFromToolchainProvider).kotlinNativeBundleVersion.get()
        )
    }

    @Test
    fun `KT-72068 - kotlin native link task has no operation Kotlin Native provider when target is not supported`() {
        val project = prepareProjectWithEnabledKlibsCrossCompilation()

        project.tasks.withType(KotlinNativeLink::class.java).forEach { linkTask ->
            assert(linkTask.kotlinNativeProvider.get() is NoopKotlinNativeProvider)
        }
    }

    @Test
    fun `kotlin native compile task has Kotlin Native from toolchain provider with cross compilation`() {
        val project = prepareProjectWithEnabledKlibsCrossCompilation()

        project.tasks.withType(KotlinNativeCompile::class.java).forEach { compileTask ->
            assert(compileTask.kotlinNativeProvider.get() is KotlinNativeFromToolchainProvider)
        }

    }

    private fun prepareProjectWithEnabledKlibsCrossCompilation(): Project {
        // we support all apple targets on MacOs, so this test should run only on Windows and Linux
        Assume.assumeTrue(!HostManager.hostIsMac)

        val project = buildProjectWithMPP {
            setUpKotlinNativeToolchainWithStableVersion()
        }
        project.multiplatformExtension.iosX64()
        project.multiplatformExtension.iosArm64()

        project.evaluate()

        return project
    }

    private fun Project.setUpKotlinNativeToolchainWithStableVersion() {
        project.extraProperties.set("kotlin.native.version", STABLE_VERSION)
        project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", true)
    }
}