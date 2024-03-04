/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.*

class NativeBinaryTest {

    @Test
    fun `test baseNameProvider`() {
        val project = buildProjectWithMPP {
            project.multiplatformExtension.iosSimulatorArm64()
        }

        val compilation = project
            .multiplatformExtension
            .iosSimulatorArm64()
            .compilations
            .main

        assertNotNull(compilation)

        val initialBaseName = "Shared"
        val binary = Framework("Test", initialBaseName, NativeBuildType.DEBUG, compilation)
        val nameProvider = binary.baseNameProvider

        fun checkBaseName(name: String) {
            assertEquals(binary.baseName, name)
            assertEquals(nameProvider.get(), name)
        }

        checkBaseName(initialBaseName)

        val newBaseName = "NewShared"
        binary.baseName = newBaseName
        checkBaseName(newBaseName)
    }
}