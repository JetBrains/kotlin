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
        assertTrue(kotlin.main.fragments.create("some common") is KotlinGradleFragmentInternal)
        kotlin.main.fragments.create<KotlinJvmVariant>("jvm")
        kotlin.main.fragments.create<KotlinLinuxX64Variant>("linuxX64")
        kotlin.main.fragments.create<KotlinMacosX64Variant>("macosX64")
        kotlin.main.fragments.create<KotlinMacosArm64Variant>("macosArm64")
        kotlin.main.fragments.create<KotlinIosX64Variant>("iosX64")
        kotlin.main.fragments.create<KotlinIosArm64Variant>("iosArm64")
    }
}
