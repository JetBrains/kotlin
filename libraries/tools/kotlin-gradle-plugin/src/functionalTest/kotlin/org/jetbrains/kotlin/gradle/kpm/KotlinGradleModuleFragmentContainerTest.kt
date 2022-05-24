/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import kotlin.test.Test
import kotlin.test.assertTrue

class KotlinGradleModuleFragmentContainerTest : AbstractKpmExtensionTest() {

    @Test
    fun `test creating several different fragment types`() {
        assertTrue(kotlin.main.fragments.create("some common") is KpmGradleFragmentInternal)
        kotlin.main.fragments.create<KpmJvmVariant>("jvm")
        kotlin.main.fragments.create<KpmLinuxX64Variant>("linuxX64")
        kotlin.main.fragments.create<KpmMacosX64Variant>("macosX64")
        kotlin.main.fragments.create<KpmMacosArm64Variant>("macosArm64")
        kotlin.main.fragments.create<KpmIosX64Variant>("iosX64")
        kotlin.main.fragments.create<KpmIosArm64Variant>("iosArm64")
    }
}
