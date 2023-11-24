/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

private const val STABLE_VERSION = "1.9.20"

@Ignore(value = "We need to provide proper way of validating k/n dependencies: KT-52483")
class KotlinNativeToolchainTest {

    @Test
    fun `check that kotlin native compiler stable version has been resolved correctly`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.linuxX64()
            project.extraProperties.set("kotlin.native.version", STABLE_VERSION)
            project.extraProperties.set("kotlin.native.distribution.downloadFromMaven", true)
        }

        project.evaluate()

        val compileTask = project.tasks.withType(KotlinNativeCompile::class.java).first()

        assertEquals(
            "kotlin-native-prebuilt-${HostManager.platformName()}-$STABLE_VERSION",
            compileTask.kotlinNativeProvider.get().kotlinNativeBundleVersion.get()
        )
    }
}