/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirContainingClassDetails
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.unsupported
import org.junit.Test

abstract class LoweringVisibilityCommonizerTest(
    private val allowPrivate: Boolean,
    private val areMembersVirtual: Boolean
) : AbstractCommonizerTest<CirHasVisibility, Visibility>() {

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
    fun somethingUnexpected() = doTestFailure(
        PUBLIC.toMock(), LOCAL.toMock()
    )

    final override fun createCommonizer() = VisibilityCommonizer.lowering(allowPrivate = allowPrivate)

    protected fun Visibility.toMock() = object : CirFunctionOrProperty {
        override val annotations get() = unsupported()
        override val name get() = unsupported()
        override val typeParameters get() = unsupported()
        override val visibility = this@toMock
        override val modality get() = if (areMembersVirtual) Modality.OPEN else Modality.FINAL
        override val containingClassDetails =
            if (areMembersVirtual) CirContainingClassDetails(kind = ClassKind.CLASS, modality = Modality.OPEN, isData = false) else null
        override val isExternal get() = unsupported()
        override val extensionReceiver get() = unsupported()
        override val returnType get() = unsupported()
        override val kind get() = unsupported()
    }

    class PrivateMembers : LoweringVisibilityCommonizerTest(true, false) {

        @Test
        fun publicAndProtected() = doTestSuccess(
            expected = PROTECTED,
            PUBLIC.toMock(), PROTECTED.toMock(), PUBLIC.toMock()
        )

        @Test
        fun publicAndInternal() = doTestSuccess(
            expected = INTERNAL,
            PUBLIC.toMock(), INTERNAL.toMock(), PUBLIC.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndInternalAndProtected() = doTestFailure(
            PUBLIC.toMock(), INTERNAL.toMock(), PROTECTED.toMock()
        )

        @Test
        fun publicAndInternalAndPrivate() = doTestSuccess(
            expected = PRIVATE,
            PUBLIC.toMock(), INTERNAL.toMock(), PRIVATE.toMock()
        )

        @Test
        fun privateOnly() = doTestSuccess(
            expected = PRIVATE,
            PRIVATE.toMock(), PRIVATE.toMock(), PRIVATE.toMock()
        )
    }

    class NonVirtualMembers : LoweringVisibilityCommonizerTest(false, false) {

        @Test
        fun publicAndProtected() = doTestSuccess(
            expected = PROTECTED,
            PUBLIC.toMock(), PROTECTED.toMock(), PUBLIC.toMock()
        )

        @Test
        fun publicAndInternal() = doTestSuccess(
            expected = INTERNAL,
            PUBLIC.toMock(), INTERNAL.toMock(), PUBLIC.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndInternalAndProtected() = doTestFailure(
            PUBLIC.toMock(), INTERNAL.toMock(), PROTECTED.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndInternalAndPrivate() = doTestFailure(
            PUBLIC.toMock(), INTERNAL.toMock(), PRIVATE.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun privateOnly() = doTestFailure(
            PRIVATE.toMock()
        )
    }

    class VirtualMembers : LoweringVisibilityCommonizerTest(false, true) {

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndProtected1() = doTestFailure(
            PUBLIC.toMock(), PROTECTED.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndProtected2() = doTestFailure(
            PUBLIC.toMock(), PUBLIC.toMock(), PROTECTED.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndInternal1() = doTestFailure(
            PUBLIC.toMock(), INTERNAL.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndInternal2() = doTestFailure(
            PUBLIC.toMock(), PUBLIC.toMock(), INTERNAL.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun protectedAndInternal1() = doTestFailure(
            PROTECTED.toMock(), INTERNAL.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun protectedAndInternal2() = doTestFailure(
            PROTECTED.toMock(), PROTECTED.toMock(), INTERNAL.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun publicAndPrivate() = doTestFailure(
            PUBLIC.toMock(), PRIVATE.toMock()
        )

        @Test(expected = IllegalCommonizerStateException::class)
        fun privateOnly() = doTestFailure(
            PRIVATE.toMock()
        )
    }
}
