/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.Visibilities
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PropertySetterCommonizerTest {

    @Test
    fun `missing only`() {
        assertNull(
            PropertySetterCommonizer.commonize(null, null),
            "Expected no property setting being commonized from nulls"
        )
    }

    @Test
    fun `missing and public`() {
        assertSame(
            PropertySetterCommonizer.privateFallbackSetter,
            PropertySetterCommonizer.commonize(null, CirPropertySetter.createDefaultNoAnnotations(Visibilities.Public))
        )
    }

    @Test
    fun `public public`() {
        assertEquals(
            CirPropertySetter.createDefaultNoAnnotations(Visibilities.Public),
            PropertySetterCommonizer.commonize(
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Public),
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Public)
            )
        )
    }

    @Test
    fun `internal internal`() {
        assertEquals(
            CirPropertySetter.createDefaultNoAnnotations(Visibilities.Internal),
            PropertySetterCommonizer.commonize(
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Internal),
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Internal)
            )
        )
    }

    @Test
    fun `internal public`() {
        assertEquals(
            PropertySetterCommonizer.privateFallbackSetter,
            PropertySetterCommonizer.commonize(
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Internal),
                CirPropertySetter.createDefaultNoAnnotations(Visibilities.Public)
            )
        )
    }
}
