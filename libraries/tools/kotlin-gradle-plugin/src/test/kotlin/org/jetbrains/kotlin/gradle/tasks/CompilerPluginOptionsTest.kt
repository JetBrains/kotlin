/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.junit.Test
import kotlin.test.assertEquals

class CompilerPluginOptionsTest {
    @Test
    fun shouldCreateNewOneWithoutLoosingExistingOptions() {
        val compilerOptions1 = CompilerPluginOptions()
        compilerOptions1.addPluginArgument(EXAMPLE_PLUGIN_ID, subpluginOption1)

        val compilerOptions2 = CompilerPluginOptions(compilerOptions1)

        assertEquals(setOf(EXAMPLE_PLUGIN_ID), compilerOptions2.allOptions().keys)
        assertEquals(listOf(subpluginOption1), compilerOptions2.allOptions()[EXAMPLE_PLUGIN_ID])
    }

    @Test
    fun addingNewArgumentDoNotLooseExistingOptions() {
        val compilerOptions = CompilerPluginOptions()

        compilerOptions.addPluginArgument(EXAMPLE_PLUGIN_ID, subpluginOption1)
        compilerOptions.addPluginArgument(EXAMPLE_PLUGIN_ID, subpluginOption2)

        assertEquals(setOf(EXAMPLE_PLUGIN_ID), compilerOptions.allOptions().keys)
        assertEquals(listOf(subpluginOption1, subpluginOption2), compilerOptions.allOptions()[EXAMPLE_PLUGIN_ID])
    }

    @Test
    fun combiningTwoCompilerOptionsCombinesOptionsWithTheSamePluginId() {
        val compilerOptions1 = CompilerPluginOptions()
        compilerOptions1.addPluginArgument(EXAMPLE_PLUGIN_ID, subpluginOption1)
        val compilerOptions2 = CompilerPluginOptions()
        compilerOptions2.addPluginArgument(EXAMPLE_PLUGIN_ID, subpluginOption2)

        val compilerOptions3 = compilerOptions1 + compilerOptions2

        assertEquals(setOf(EXAMPLE_PLUGIN_ID), compilerOptions3.allOptions().keys)
        assertEquals(listOf(subpluginOption1, subpluginOption2), compilerOptions3.allOptions()[EXAMPLE_PLUGIN_ID])
    }

    companion object {
        private const val EXAMPLE_PLUGIN_ID = "com.example"
        private val subpluginOption1 = SubpluginOption("option1", "value1")
        private val subpluginOption2 = SubpluginOption("option2", "value2")
    }
}