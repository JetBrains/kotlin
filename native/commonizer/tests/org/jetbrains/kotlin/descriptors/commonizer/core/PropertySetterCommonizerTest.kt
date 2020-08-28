/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertySetterFactory
import org.junit.Test

class PropertySetterCommonizerTest : AbstractCommonizerTest<CirPropertySetter?, CirPropertySetter?>() {

    @Test
    fun absentOnly() = super.doTestSuccess(
        expected = null,
        null, null, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun absentAndPublic() = doTestFailure(
        null, null, null, PUBLIC
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndAbsent() = doTestFailure(
        PUBLIC, PUBLIC, PUBLIC, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndAbsent() = doTestFailure(
        PROTECTED, PROTECTED, null
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun absentAndInternal() = doTestFailure(
        null, null, INTERNAL
    )

    @Test
    fun publicOnly() = doTestSuccess(
        expected = PUBLIC,
        PUBLIC, PUBLIC, PUBLIC
    )

    @Test
    fun protectedOnly() = doTestSuccess(
        expected = PROTECTED,
        PROTECTED, PROTECTED, PROTECTED
    )

    @Test
    fun internalOnly() = doTestSuccess(
        expected = INTERNAL,
        INTERNAL, INTERNAL, INTERNAL
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun privateOnly() = doTestFailure(
        PRIVATE
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndProtected() = doTestFailure(
        PUBLIC, PUBLIC, PROTECTED
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndInternal() = doTestFailure(
        PUBLIC, PUBLIC, INTERNAL
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndInternal() = doTestFailure(
        PROTECTED, PROTECTED, INTERNAL
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndPrivate() = doTestFailure(
        PUBLIC, PUBLIC, PRIVATE
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun somethingUnexpected() = doTestFailure(
        PUBLIC, LOCAL
    )

    private fun doTestSuccess(expected: Visibility?, vararg variants: Visibility?) =
        super.doTestSuccess(
            expected = expected?.let { CirPropertySetterFactory.createDefaultNoAnnotations(expected) },
            *variants.map { it?.let(CirPropertySetterFactory::createDefaultNoAnnotations) }.toTypedArray()
        )

    private fun doTestFailure(vararg variants: Visibility?) =
        super.doTestFailure(
            *variants.map { it?.let(CirPropertySetterFactory::createDefaultNoAnnotations) }.toTypedArray(),
            shouldFailOnFirstVariant = false
        )

    override fun createCommonizer() = PropertySetterCommonizer()
}
