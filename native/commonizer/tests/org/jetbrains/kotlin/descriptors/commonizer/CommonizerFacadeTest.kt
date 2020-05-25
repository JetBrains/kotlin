/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.AbstractCommonizationFromSourcesTest.Companion.eachModuleAsTarget
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertCommonizationPerformed
import org.jetbrains.kotlin.descriptors.commonizer.utils.mockEmptyModule
import org.junit.Test
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class CommonizerFacadeTest {

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
    fun commonized() {
        val modules = listOf(
            mockEmptyModule("<foo>"),
            mockEmptyModule("<foo>")
        )

        val result = runCommonization(modules.eachModuleAsTarget())

        assertCommonizationPerformed(result)

        assertSingleModuleForTarget("<foo>", result.modulesByTargets.getValue(result.commonTarget))

        assertEquals(2, result.concreteTargets.size)
        for (target in result.concreteTargets) {
            assertSingleModuleForTarget("<foo>", result.modulesByTargets.getValue(target))
        }
    }

    private fun assertSingleModuleForTarget(
        @Suppress("SameParameterValue") expectedModuleName: String,
        modules: Collection<ModuleDescriptor>
    ) {
        assertEquals(1, modules.size)
        assertEquals(expectedModuleName, modules.single().name.asString())
    }
}
