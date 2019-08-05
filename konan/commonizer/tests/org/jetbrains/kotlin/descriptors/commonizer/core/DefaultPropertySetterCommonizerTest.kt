/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.ir.Setter
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockProperty
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

class DefaultPropertySetterCommonizerTest : AbstractCommonizerTest<PropertySetterDescriptor?, Setter?>() {

    @Test
    fun absentOnly() = super.doTestSuccess(null, null, null, null)

    @Test(expected = IllegalStateException::class)
    fun absentAndPublic() = doTestFailure(null, null, null, PUBLIC)

    @Test(expected = IllegalStateException::class)
    fun publicAndAbsent() = doTestFailure(PUBLIC, PUBLIC, PUBLIC, null)

    @Test(expected = IllegalStateException::class)
    fun protectedAndAbsent() = doTestFailure(PROTECTED, PROTECTED, null)

    @Test(expected = IllegalStateException::class)
    fun absentAndInternal() = doTestFailure(null, null, INTERNAL)

    @Test
    fun publicOnly() = doTestSuccess(PUBLIC, PUBLIC, PUBLIC, PUBLIC)

    @Test
    fun protectedOnly() = doTestSuccess(PROTECTED, PROTECTED, PROTECTED, PROTECTED)

    @Test
    fun internalOnly() = doTestSuccess(INTERNAL, INTERNAL, INTERNAL, INTERNAL)

    @Test(expected = IllegalStateException::class)
    fun privateOnly() = doTestFailure(PRIVATE)

    @Test
    fun publicAndProtected() = doTestSuccess(PROTECTED, PUBLIC, PROTECTED, PUBLIC)

    @Test
    fun publicAndInternal() = doTestSuccess(INTERNAL, PUBLIC, INTERNAL, PUBLIC)

    @Test(expected = IllegalStateException::class)
    fun protectedAndInternal() = doTestFailure(PUBLIC, INTERNAL, PROTECTED)

    @Test(expected = IllegalStateException::class)
    fun publicAndPrivate() = doTestFailure(PUBLIC, INTERNAL, PRIVATE)

    @Test(expected = IllegalStateException::class)
    fun somethingUnexpected() = doTestFailure(PUBLIC, LOCAL)

    private fun doTestSuccess(expected: Visibility?, vararg variants: Visibility?) =
        super.doTestSuccess(
            expected?.let { Setter.createDefaultNoAnnotations(expected) },
            *variants.map { it?.let(Visibility::toMockProperty) }.toTypedArray()
        )

    private fun doTestFailure(vararg variants: Visibility?) =
        super.doTestFailure(
            *variants.map { it?.let(Visibility::toMockProperty) }.toTypedArray()
        )

    override fun createCommonizer() = PropertySetterCommonizer.default()
}


@UseExperimental(TypeRefinement::class)
private fun Visibility.toMockProperty() = mockProperty(
    name = "myProperty",
    setterVisibility = this,
    extensionReceiverType = null,
    returnType = mockClassType("kotlin.String").unwrap()
).setter!!
