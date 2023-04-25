/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.util.MultiplatformExtensionTest
import org.jetbrains.kotlin.gradle.plugin.runMissingAndroidTargetProjectConfigurationHealthCheck
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.junit.Test
import kotlin.test.*

class KT41641AbsentAndroidTarget : MultiplatformExtensionTest() {

    @Test
    fun `test android plugin without android target`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31

        kotlin.jvm()

        /* Previously failed with 'Collection is empty.' */
        assertNull(project.findAndroidTarget())
        assertSame(kotlin.androidTarget(), project.findAndroidTarget())
    }

    @Test
    fun `test runMissingAndroidTargetProjectConfigurationHealthCheck`() {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31

        kotlin.jvm()

        // Missing android target -> expect warning message
        var warningMessage: String? = null
        project.runMissingAndroidTargetProjectConfigurationHealthCheck(warningLogger = { warningMessage = it })
        assertNotNull(warningMessage, "Expected warning message to be logged")

        // Present android target -> expect no warning message anymore
        kotlin.androidTarget()
        project.runMissingAndroidTargetProjectConfigurationHealthCheck(warningLogger = {
            fail("Expected no warning message to be logged. Received: $it")
        })
    }

    @Test
    fun `test kotlin-mpp-absentAndroidTarget-nowarn flag`() {
        project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = "true"
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31

        kotlin.jvm()

        project.runMissingAndroidTargetProjectConfigurationHealthCheck(warningLogger = {
            fail("Expected no warning message to be logged. Received $it")
        })

        // Test when nowarn is set to false
        run {
            var warningMessage: String? = null
            project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = "false"
            project.runMissingAndroidTargetProjectConfigurationHealthCheck(warningLogger = { warningMessage = it })
            assertNotNull(warningMessage, "Expected warning message to be logged")
        }

        // Test when nowarn is set to null
        run {
            var warningMessage: String? = null
            project.propertiesExtension["kotlin.mpp.absentAndroidTarget.nowarn"] = null
            project.runMissingAndroidTargetProjectConfigurationHealthCheck(warningLogger = { warningMessage = it })
            assertNotNull(warningMessage, "Expected warning message to be logged")
        }
    }
}
