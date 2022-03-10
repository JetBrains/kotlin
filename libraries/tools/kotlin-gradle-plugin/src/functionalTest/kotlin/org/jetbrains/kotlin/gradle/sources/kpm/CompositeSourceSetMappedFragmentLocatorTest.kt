/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.sources.kpm

import org.jetbrains.kotlin.gradle.MultiplatformExtensionTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.SourceSetMappedFragmentLocator
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.DefaultCompositeLocator
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompositeSourceSetMappedFragmentLocatorTest : MultiplatformExtensionTest() {
    @BeforeTest
    override fun setup() {
        project.extensions.extraProperties.set(PropertiesProvider.PropertyNames.KOTLIN_KPM_EXPERIMENTAL_MODEL_MAPPING, "true")
        super.setup()
    }

    @Test
    fun `fallback used if registered locators return null`() {
        val l1 = LocatorDummyImpl(null)
        val l2 = LocatorDummyImpl(null)
        val l3 = LocatorDummyImpl("l3")
        val locator = DefaultCompositeLocator(l3).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result = locator.locateFragmentForSourceSet("test")
        assertEquals(result?.fragmentName, l3.result)
        assertEquals(1, l1.invocationsCount)
        assertEquals(1, l2.invocationsCount)
        assertEquals(1, l3.invocationsCount)
    }

    @Test
    fun `fallback not invoked if registered locator found in children`() {
        val l1 = LocatorDummyImpl(null)
        val l2 = LocatorDummyImpl("l2")
        val l3 = LocatorDummyImpl("l3")
        val locator = DefaultCompositeLocator(l3).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result = locator.locateFragmentForSourceSet("test")
        assertEquals(result?.fragmentName, l2.result)
        assertEquals(1, l1.invocationsCount)
        assertEquals(1, l2.invocationsCount)
        assertEquals(0, l3.invocationsCount)
    }

    @Test
    fun `fallback invoked if no other locator registered`() {
        val l1 = LocatorDummyImpl(null)
        val locator = DefaultCompositeLocator(l1)
        val result = locator.locateFragmentForSourceSet("test")
        assertEquals(result?.fragmentName, l1.result)
        assertEquals(1, l1.invocationsCount)
    }

    @Test
    fun `register additional locator, no fallback`() {
        val l1 = LocatorDummyImpl(null)
        val l2 = LocatorDummyImpl(null)
        val locator = DefaultCompositeLocator(null).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result = locator.locateFragmentForSourceSet("test")
        assertNull(result)
        assertEquals(1, l1.invocationsCount)
        assertEquals(1, l2.invocationsCount)

        val l3 = LocatorDummyImpl("l3")
        locator.registerLocator(l3)
        val newResult = locator.locateFragmentForSourceSet("test")
        assertEquals(l3.result, newResult?.fragmentName)
        assertEquals(2, l1.invocationsCount)
        assertEquals(2, l2.invocationsCount)
        assertEquals(1, l3.invocationsCount)
    }

    @Test
    fun `returns null if all locators return null`() {
        val l1 = LocatorDummyImpl(null)
        val l2 = LocatorDummyImpl(null)
        val locator = DefaultCompositeLocator(null).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result = locator.locateFragmentForSourceSet("test")
        assertNull(result)
        assertEquals(1, l1.invocationsCount)
        assertEquals(1, l2.invocationsCount)

        val l3 = LocatorDummyImpl(null)
        val withFallback = DefaultCompositeLocator(l3).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result2 = withFallback.locateFragmentForSourceSet("test")
        assertNull(result2)
        assertEquals(2, l1.invocationsCount)
        assertEquals(2, l2.invocationsCount)
        assertEquals(1, l3.invocationsCount)
    }

    @Test
    fun `prefer non-main locations`() {
        val l1 = LocatorDummyImpl("main")
        val l2 = LocatorDummyImpl("myTest")
        val l3 = LocatorDummyImpl("main")
        val locator = DefaultCompositeLocator(l3).apply {
            registerLocator(l1)
            registerLocator(l2)
        }
        val result = locator.locateFragmentForSourceSet("test")
        assertEquals(result?.moduleName, l2.result)
        assertEquals(1, l1.invocationsCount)
        assertEquals(1, l2.invocationsCount)
        assertEquals(0, l3.invocationsCount)

        // test the same preference of non-main results in case they are returned by the fallback locators
        val locator2 = DefaultCompositeLocator(l2).apply {
            registerLocator(l1)
        }
        val result2 = locator2.locateFragmentForSourceSet("test")
        assertEquals(result2?.moduleName, l2.result)
        assertEquals(2, l1.invocationsCount)
        assertEquals(2, l2.invocationsCount)
    }
}

private class LocatorDummyImpl(val result: String?) : SourceSetMappedFragmentLocator {
    var invocationsCount = 0

    override fun locateFragmentForSourceSet(sourceSetName: String): SourceSetMappedFragmentLocator.FragmentLocation? {
        ++invocationsCount
        return if (result != null) SourceSetMappedFragmentLocator.FragmentLocation(result, result) else null
    }
}