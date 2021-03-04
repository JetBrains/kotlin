/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities.Internal
import org.jetbrains.kotlin.descriptors.Visibilities.Local
import org.jetbrains.kotlin.descriptors.Visibilities.Private
import org.jetbrains.kotlin.descriptors.Visibilities.Protected
import org.jetbrains.kotlin.descriptors.Visibilities.Public
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.junit.Test

class PropertySetterCommonizerTest : AbstractCommonizerTest<CirPropertySetter?, CirPropertySetter?>() {

    @Test
    fun missingOnly() = super.doTestSuccess(
        expected = null,
        null, null, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun missingAndPublic() = doTestFailure(
        null, null, null, Public
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndMissing() = doTestFailure(
        Public, Public, Public, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndMissing() = doTestFailure(
        Protected, Protected, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun missingAndInternal() = doTestFailure(
        null, null, Internal
    )

    @Test
    fun publicOnly() = doTestSuccess(
        expected = Public,
        Public, Public, Public
    )

    @Test
    fun protectedOnly() = doTestSuccess(
        expected = Protected,
        Protected, Protected, Protected
    )

    @Test
    fun internalOnly() = doTestSuccess(
        expected = Internal,
        Internal, Internal, Internal
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun privateOnly() = doTestFailure(
        Private
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndProtected() = doTestFailure(
        Public, Public, Protected
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndInternal() = doTestFailure(
        Public, Public, Internal
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndInternal() = doTestFailure(
        Protected, Protected, Internal
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndPrivate() = doTestFailure(
        Public, Public, Private
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun somethingUnexpected() = doTestFailure(
        Public, Local
    )

    private fun doTestSuccess(expected: Visibility?, vararg variants: Visibility?) =
        super.doTestSuccess(
            expected = expected?.let { CirPropertySetter.createDefaultNoAnnotations(expected) },
            *variants.map { it?.let(CirPropertySetter::createDefaultNoAnnotations) }.toTypedArray()
        )

    private fun doTestFailure(vararg variants: Visibility?) =
        super.doTestFailure(
            *variants.map { it?.let(CirPropertySetter::createDefaultNoAnnotations) }.toTypedArray(),
            shouldFailOnFirstVariant = false
        )

    override fun createCommonizer() = PropertySetterCommonizer()
}
