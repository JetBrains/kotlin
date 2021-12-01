/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue

class CocoaPodsNonMacIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun testImport() {
        transformProjectWithPluginsDsl(
            "native-cocoapods-tests"
        ).build("podImport", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()
            assertTrue { output.contains("Kotlin Cocoapods Plugin is fully supported on mac machines only. Gradle tasks that can not run on non-mac hosts will be skipped.") }
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsNonMac() {
            Assume.assumeFalse(HostManager.hostIsMac)
        }
    }
}