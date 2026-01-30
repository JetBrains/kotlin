/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.diagnosticsTests

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setAndroidSdkDirProperty
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.fail

class KMPWithAndroidDiagnosticsTest {

    @DisplayName("KT-81958 'androidTarget' used with new Android KMP library plugin")
    @Test
    fun testAndroidTargetWithKmpLibraryPlugin() {
        val project = buildProjectWithMPP(preApplyCode = {
            project.plugins.apply("com.android.kotlin.multiplatform.library")
        })
        setAndroidSdkDirProperty(project)

        try {
            project.kotlin {
                androidTarget()
            }
        } catch (_: InvalidUserCodeException) {
            project.assertContainsDiagnostic(KotlinToolingDiagnostics.KMPAndroidTargetIsIncompatibleWithTheNewAgpKMPPlugin)
            return
        }

        fail("Configuration was successful")
    }
}