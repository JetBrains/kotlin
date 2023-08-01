/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonTest
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.assertContainsDependencies
import org.jetbrains.kotlin.gradle.util.assertNotContainsDependencies
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test

class KotlinMultiplatformExtensionTest {
    @Test
    fun `test - top level dependencies`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        kotlin.dependencies {
            implementation("foo:bar:1.0.0")
        }

        project.assertContainsDependencies(
            kotlin.sourceSets.commonMain.get().implementationConfigurationName, "foo:bar:1.0.0", exhaustive = true
        )

        project.assertNotContainsDependencies(
            kotlin.sourceSets.commonTest.get().implementationConfigurationName, "foo:bar:1.0.0"
        )
    }

    @Test
    fun `test - top level testDependencies`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.linuxX64()
        kotlin.linuxArm64()

        kotlin.testDependencies {
            implementation("foo:bar:1.0.0")
        }

        project.assertNotContainsDependencies(
            kotlin.sourceSets.commonMain.get().implementationConfigurationName, "foo:bar:1.0.0"
        )

        project.assertContainsDependencies(
            kotlin.sourceSets.commonTest.get().implementationConfigurationName, "foo:bar:1.0.0", exhaustive = true
        )
    }
}