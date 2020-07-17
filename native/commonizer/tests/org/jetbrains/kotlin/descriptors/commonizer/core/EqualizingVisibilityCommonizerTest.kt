/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility
import org.junit.Test

class EqualizingVisibilityCommonizerTest : AbstractCommonizerTest<CirHasVisibility, Visibility>() {

    @Test
    fun publicOnly() = doTestSuccess(
        expected = PUBLIC,
        PUBLIC.toMock(), PUBLIC.toMock(), PUBLIC.toMock()
    )

    @Test
    fun protectedOnly() = doTestSuccess(
        expected = PROTECTED,
        PROTECTED.toMock(), PROTECTED.toMock(), PROTECTED.toMock()
    )

    @Test
    fun internalOnly() = doTestSuccess(
        expected = INTERNAL,
        INTERNAL.toMock(), INTERNAL.toMock(), INTERNAL.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun privateOnly() = doTestFailure(
        PRIVATE.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndProtected() = doTestFailure(
        PROTECTED.toMock(), PROTECTED.toMock(), PUBLIC.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndInternal() = doTestFailure(
        INTERNAL.toMock(), INTERNAL.toMock(), PUBLIC.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun protectedAndInternal() = doTestFailure(
        PROTECTED.toMock(), PROTECTED.toMock(), INTERNAL.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun publicAndPrivate() = doTestFailure(
        PUBLIC.toMock(), PUBLIC.toMock(), PRIVATE.toMock()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun somethingUnexpected() = doTestFailure(
        PUBLIC.toMock(), LOCAL.toMock()
    )

    override fun createCommonizer() = VisibilityCommonizer.equalizing()
}

private fun Visibility.toMock() = object : CirHasVisibility {
    override val visibility: Visibility = this@toMock
}
