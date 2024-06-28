/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test
import kotlin.test.assertFails

class AndroidGradlePluginIsMissingTest {
    @Test
    fun `test - android plugin is not applied`() {
        val project = buildProjectWithMPP()
        assertFails { project.multiplatformExtension.androidTarget() }
        project.checkDiagnostics("AndroidGradlePluginIsMissing")
    }

    @Test
    fun `test - android application plugin is applied`() {
        val project = buildProjectWithMPP()
        project.androidApplication { compileSdk = 33 }
        project.multiplatformExtension.androidTarget()
        project.assertNoDiagnostics()
    }
}
