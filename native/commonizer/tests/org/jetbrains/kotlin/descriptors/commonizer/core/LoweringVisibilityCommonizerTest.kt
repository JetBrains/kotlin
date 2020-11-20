/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirContainingClassDetailsFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.unsupported
import org.junit.Test

abstract class LoweringVisibilityCommonizerTest(
    private val areMembersVirtual: Boolean
) : AbstractCommonizerTest<CirHasVisibility, DescriptorVisibility>() {

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

    final override fun createCommonizer() = VisibilityCommonizer.lowering()

    protected fun DescriptorVisibility.toMock() = object : CirFunctionOrProperty {
        override val annotations get() = unsupported()
        override val name get() = unsupported()
        override val typeParameters get() = unsupported()
        override val visibility = this@toMock
        override val modality get() = if (areMembersVirtual) Modality.OPEN else Modality.FINAL
        override val containingClassDetails = if (areMembersVirtual)
            CirContainingClassDetailsFactory.create(
                kind = ClassKind.CLASS,
                modality = Modality.OPEN,
                isData = false
            ) else null
        override val extensionReceiver get() = unsupported()
        override val returnType get() = unsupported()
        override val kind get() = unsupported()
    }

    class NonVirtualMembers : LoweringVisibilityCommonizerTest(false) {

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

    class VirtualMembers : LoweringVisibilityCommonizerTest(true) {

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
