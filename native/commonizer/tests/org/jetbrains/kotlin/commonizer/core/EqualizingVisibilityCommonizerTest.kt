/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.descriptors.Visibilities.Internal
import org.jetbrains.kotlin.descriptors.Visibilities.Local
import org.jetbrains.kotlin.descriptors.Visibilities.Private
import org.jetbrains.kotlin.descriptors.Visibilities.Protected
import org.jetbrains.kotlin.descriptors.Visibilities.Public
import org.jetbrains.kotlin.descriptors.Visibility
import org.junit.Test

class EqualizingVisibilityCommonizerTest : AbstractCommonizerTest<CirHasVisibility, Visibility>() {

    @Test
    fun publicOnly() = doTestSuccess(
        expected = Public,
        Public.toMock(), Public.toMock(), Public.toMock()
    )

    @Test
    fun protectedOnly() = doTestSuccess(
        expected = Protected,
        Protected.toMock(), Protected.toMock(), Protected.toMock()
    )

    @Test
    fun internalOnly() = doTestSuccess(
        expected = Internal,
        Internal.toMock(), Internal.toMock(), Internal.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun privateOnly() = doTestFailure(
        Private.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndProtected() = doTestFailure(
        Protected.toMock(), Protected.toMock(), Public.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndInternal() = doTestFailure(
        Internal.toMock(), Internal.toMock(), Public.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndInternal() = doTestFailure(
        Protected.toMock(), Protected.toMock(), Internal.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndPrivate() = doTestFailure(
        Public.toMock(), Public.toMock(), Private.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun somethingUnexpected() = doTestFailure(
        Public.toMock(), Local.toMock()
    )

    override fun createCommonizer() = VisibilityCommonizer.equalizing()
}

private fun Visibility.toMock() = object : CirHasVisibility {
    override val visibility: Visibility = this@toMock
}
