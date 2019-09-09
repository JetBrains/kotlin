/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Setter
import org.junit.Test

class DefaultPropertySetterCommonizerTest : AbstractCommonizerTest<Setter?, Setter?>() {

    @Test
    fun absentOnly() = super.doTestSuccess(null, null, null, null)

    @Test(expected = IllegalCommonizerStateException::class)
    fun absentAndPublic() = doTestFailure(null, null, null, PUBLIC)

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndAbsent() = doTestFailure(PUBLIC, PUBLIC, PUBLIC, null)

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndAbsent() = doTestFailure(PROTECTED, PROTECTED, null)

    @Test(expected = IllegalCommonizerStateException::class)
    fun absentAndInternal() = doTestFailure(null, null, INTERNAL)

    @Test
    fun publicOnly() = doTestSuccess(PUBLIC, PUBLIC, PUBLIC, PUBLIC)

    @Test
    fun protectedOnly() = doTestSuccess(PROTECTED, PROTECTED, PROTECTED, PROTECTED)

    @Test
    fun internalOnly() = doTestSuccess(INTERNAL, INTERNAL, INTERNAL, INTERNAL)

    @Test(expected = IllegalCommonizerStateException::class)
    fun privateOnly() = doTestFailure(PRIVATE)

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndProtected() = doTestFailure(PUBLIC, PUBLIC, PROTECTED)

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndInternal() = doTestFailure(PUBLIC, PUBLIC, INTERNAL)

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndInternal() = doTestFailure(PROTECTED, PROTECTED, INTERNAL)

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndPrivate() = doTestFailure(PUBLIC, PUBLIC, PRIVATE)

    @Test(expected = IllegalCommonizerStateException::class)
    fun somethingUnexpected() = doTestFailure(PUBLIC, LOCAL)

    private fun doTestSuccess(expected: Visibility?, vararg variants: Visibility?) =
        super.doTestSuccess(
            expected?.let { Setter.createDefaultNoAnnotations(expected) },
            *variants.map { it?.let(Setter.Companion::createDefaultNoAnnotations) }.toTypedArray()
        )

    private fun doTestFailure(vararg variants: Visibility?) =
        super.doTestFailure(
            *variants.map { it?.let(Setter.Companion::createDefaultNoAnnotations) }.toTypedArray()
        )

    override fun createCommonizer() = PropertySetterCommonizer.default()
}
