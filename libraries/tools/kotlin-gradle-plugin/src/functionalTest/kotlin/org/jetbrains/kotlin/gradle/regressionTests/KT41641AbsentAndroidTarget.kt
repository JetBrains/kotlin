/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.assertNoDiagnostics
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class KT41641AbsentAndroidTarget : MultiplatformExtensionTest() {

    @Test
    fun `test android plugin without android target`() {
        defaultSetupWithoutAndroidTarget()
        /* Previously failed with 'Collection is empty.' */
        assertNull(project.findAndroidTarget())
        assertSame(kotlin.androidTarget(), project.findAndroidTarget())
    }

    @Test
    fun `test runMissingAndroidTargetProjectConfigurationHealthCheck - warning`() {
        defaultSetupWithoutAndroidTarget()

        // Missing android target -> expect warning message
        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.AndroidTargetIsMissing)
    }

    @Test
    fun `test runMissingAndroidTargetProjectConfigurationHealthCheck - no warning`() {
        defaultSetupWithoutAndroidTarget()
        kotlin.androidTarget()

        // Present android target -> expect no warning message anymore
        project.evaluate()
        project.assertNoDiagnostics()
    }

    @Test
    fun `test kotlin-mpp-absentAndroidTarget-nowarn flag set to true`() {
        defaultSetupWithoutAndroidTarget()
        project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = "true"

        project.evaluate()
        project.assertNoDiagnostics()
    }

    @Test
    fun `test kotlin-mpp-absentAndroidTarget-nowarn flag set to false`() {
        defaultSetupWithoutAndroidTarget()
        project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = null

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.AndroidTargetIsMissing)
    }

    @Test
    fun `test kotlin-mpp-absentAndroidTarget-nowarn flag set to null`() {
        defaultSetupWithoutAndroidTarget()
        project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = null

        project.evaluate()
        project.assertContainsDiagnostic(KotlinToolingDiagnostics.AndroidTargetIsMissing)
    }

    private fun defaultSetupWithoutAndroidTarget() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31
        kotlin.jvm()

        project.setMultiplatformAndroidSourceSetLayoutVersion(2)
    }
}
