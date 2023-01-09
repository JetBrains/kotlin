/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.kpm

import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import kotlin.test.Test
import kotlin.test.assertTrue

class KpmGradleFragmentContainerTest : AbstractKpmExtensionTest() {

    @Test
    fun `test creating several different fragment types`() {
        assertTrue(kotlin.main.fragments.create("some common") is GradleKpmFragmentInternal)
        kotlin.main.fragments.create<GradleKpmJvmVariant>("jvm")
        kotlin.main.fragments.create<GradleKpmLinuxX64Variant>("linuxX64")
        kotlin.main.fragments.create<GradleKpmMacosX64Variant>("macosX64")
        kotlin.main.fragments.create<GradleKpmMacosArm64Variant>("macosArm64")
        kotlin.main.fragments.create<GradleKpmIosX64Variant>("iosX64")
        kotlin.main.fragments.create<GradleKpmIosArm64Variant>("iosArm64")
    }
}
