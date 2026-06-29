/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirContainingClass
import org.jetbrains.kotlin.commonizer.cir.CirFunctionOrProperty
import org.jetbrains.kotlin.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.commonizer.cir.unsupported
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities.Internal
import org.jetbrains.kotlin.descriptors.Visibilities.Local
import org.jetbrains.kotlin.descriptors.Visibilities.Private
import org.jetbrains.kotlin.descriptors.Visibilities.Protected
import org.jetbrains.kotlin.descriptors.Visibilities.Public
import org.jetbrains.kotlin.descriptors.Visibility
import org.junit.jupiter.api.Test

abstract class LoweringVisibilityCommonizerTest(
    private val areMembersVirtual: Boolean
) : AbstractCommonizerTest<CirHasVisibility, Visibility>() {

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

    @Test
    fun somethingUnexpected() = doTestFailure<IllegalCommonizerStateException>(
        Public.toMock(), Local.toMock()
    )

    final override fun createCommonizer() = VisibilityCommonizer.lowering()

    protected fun Visibility.toMock() = object : CirFunctionOrProperty {
        override val annotations get() = unsupported()
        override val name get() = unsupported()
        override val typeParameters get() = unsupported()
        override val visibility = this@toMock
        override val modality get() = if (areMembersVirtual) Modality.OPEN else Modality.FINAL
        override val containingClass = if (areMembersVirtual)
            object : CirContainingClass {
                override val modality get() = Modality.OPEN
                override val kind get() = ClassKind.CLASS
                override val isData get() = false
                override val isValue get() = false
            } else null
        override val extensionReceiver get() = unsupported()
        override val returnType get() = unsupported()
        override val kind get() = unsupported()
    }

    class NonVirtualMembers : LoweringVisibilityCommonizerTest(false) {

        @Test
        fun publicAndProtected() = doTestSuccess(
            expected = Protected,
            Public.toMock(), Protected.toMock(), Public.toMock()
        )

        @Test
        fun publicAndInternal() = doTestSuccess(
            expected = Internal,
            Public.toMock(), Internal.toMock(), Public.toMock()
        )

        @Test
        fun publicAndInternalAndProtected() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Internal.toMock(), Protected.toMock()
        )

        @Test
        fun publicAndInternalAndPrivate() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Internal.toMock(), Private.toMock()
        )

        @Test
        fun privateOnly() = doTestFailure<IllegalCommonizerStateException>(
            Private.toMock()
        )
    }

    class VirtualMembers : LoweringVisibilityCommonizerTest(true) {

        @Test
        fun publicAndProtected1() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Protected.toMock()
        )

        @Test
        fun publicAndProtected2() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Public.toMock(), Protected.toMock()
        )

        @Test
        fun publicAndInternal1() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Internal.toMock()
        )

        @Test
        fun publicAndInternal2() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Public.toMock(), Internal.toMock()
        )

        @Test
        fun protectedAndInternal1() = doTestFailure<IllegalCommonizerStateException>(
            Protected.toMock(), Internal.toMock()
        )

        @Test
        fun protectedAndInternal2() = doTestFailure<IllegalCommonizerStateException>(
            Protected.toMock(), Protected.toMock(), Internal.toMock()
        )

        @Test
        fun publicAndPrivate() = doTestFailure<IllegalCommonizerStateException>(
            Public.toMock(), Private.toMock()
        )

        @Test
        fun privateOnly() = doTestFailure<IllegalCommonizerStateException>(
            Private.toMock()
        )
    }
}
