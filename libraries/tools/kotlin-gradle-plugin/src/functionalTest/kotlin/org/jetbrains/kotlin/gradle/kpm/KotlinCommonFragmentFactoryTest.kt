/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCommonFragmentFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinCommonFragmentFactoryTest : AbstractKpmExtensionTest() {
    @Test
    fun `test fragmentName`() {
        val fragment = KotlinCommonFragmentFactory(kotlin.main).create("common")
        assertEquals("common", fragment.fragmentName)
    }

    @Test
    fun `test has source roots`() {
        val fragment = KotlinCommonFragmentFactory(kotlin.main).create("common")
        assertTrue(fragment.kotlinSourceRoots.srcDirs.isNotEmpty())
    }
}
