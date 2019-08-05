/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.kotlin.descriptors.commonizer.AbstractCommonizationFromSourcesTest.Companion.eachModuleAsTarget

class BasicCommonizerFacadeTest {

    @Test
    fun nothingToCommonize0() {
        val modules = listOf<ModuleDescriptor>()

        val result = runCommonization(modules.eachModuleAsTarget())

        assertEquals(NothingToCommonize, result)
    }

    @Test
    fun nothingToCommonize1() {
        val modules = listOf(
            mockEmptyModule("<foo>")
        )

        val result = runCommonization(modules.eachModuleAsTarget())

        assertEquals(NothingToCommonize, result)
    }

    @Test
    fun nothingToCommonize2() {
        val modules = listOf(
            mockEmptyModule("<foo>"),
            mockEmptyModule("<foo>")
        )

        val result = runCommonization(modules.eachModuleAsTarget())

        assertTrue(result is CommonizationPerformed)
        require(result is CommonizationPerformed) // to enforce Kotlin contracts

        assertSingleModuleForTarget("<foo>", result.commonModules)

        assertEquals(2, result.modulesByTargets.size)
        for (modulesSamePlatform in result.modulesByTargets.values) {
            assertSingleModuleForTarget("<foo>", modulesSamePlatform)
        }
    }

    @Test
    fun mismatchedModules() {
        val modules = listOf(
            mockEmptyModule("<foo>"),
            mockEmptyModule("<foo>"),
            mockEmptyModule("<bar>")
        )

        val result = runCommonization(modules.eachModuleAsTarget())

        assertEquals(NothingToCommonize, result)
    }

    private fun assertSingleModuleForTarget(
        @Suppress("SameParameterValue") expectedModuleName: String,
        modules: Collection<ModuleDescriptor>
    ) {
        assertEquals(1, modules.size)
        assertEquals(expectedModuleName, modules.single().name.asString())
    }
}
