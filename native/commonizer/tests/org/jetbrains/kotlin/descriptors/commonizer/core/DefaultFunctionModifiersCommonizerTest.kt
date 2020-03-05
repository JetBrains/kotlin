/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.core.CirTestFunctionModifiers.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunctionModifiers
import org.junit.Test

class DefaultFunctionModifiersCommonizerTest : AbstractCommonizerTest<CirFunctionModifiers, CirFunctionModifiers>() {

    @Test
    fun allDefault() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers()
    )

    @Test
    fun allSuspend() = doTestSuccess(
        mockFunctionModifiers(isSuspend = true),
        mockFunctionModifiers(isSuspend = true),
        mockFunctionModifiers(isSuspend = true),
        mockFunctionModifiers(isSuspend = true)
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun suspendAndNotSuspend() = doTestFailure(
        mockFunctionModifiers(isSuspend = true),
        mockFunctionModifiers(isSuspend = true),
        mockFunctionModifiers()
    )

    @Test(expected = IllegalCommonizerStateException::class)
    fun notSuspendAndSuspend() = doTestFailure(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isSuspend = true)
    )

    @Test
    fun allOperator() = doTestSuccess(
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers(isOperator = true)
    )

    @Test
    fun notOperatorAndOperator() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers(isOperator = true)
    )

    @Test
    fun operatorAndNotOperator() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers(isOperator = true),
        mockFunctionModifiers()
    )

    @Test
    fun allInfix() = doTestSuccess(
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers(isInfix = true)
    )

    @Test
    fun notInfixAndInfix() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers(isInfix = true)
    )

    @Test
    fun infixAndNotInfix() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers(isInfix = true),
        mockFunctionModifiers()
    )

    @Test
    fun allInline() = doTestSuccess(
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers(isInline = true)
    )

    @Test
    fun notInlineAndInline() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers(isInline = true)
    )

    @Test
    fun inlineAndNotInline() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers(isInline = true),
        mockFunctionModifiers()
    )

    @Test
    fun allTailrec() = doTestSuccess(
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers(isTailrec = true)
    )

    @Test
    fun notTailrecAndTailrec() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers(isTailrec = true)
    )

    @Test
    fun tailrecAndNotTailrec() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers(isTailrec = true),
        mockFunctionModifiers()
    )

    @Test
    fun allExternal() = doTestSuccess(
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers(isExternal = true)
    )

    @Test
    fun notExternalAndExternal() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(),
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers(isExternal = true)
    )

    @Test
    fun externalAndNotExternal() = doTestSuccess(
        mockFunctionModifiers(),
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers(isExternal = true),
        mockFunctionModifiers()
    )

    override fun createCommonizer() = FunctionModifiersCommonizer.default()
    override fun isEqual(a: CirFunctionModifiers?, b: CirFunctionModifiers?) = (a === b) || (a != null && b != null && areEqual(a, b))
}

private typealias mockFunctionModifiers = CirTestFunctionModifiers

private data class CirTestFunctionModifiers(
    override val isOperator: Boolean = false,
    override val isInfix: Boolean = false,
    override val isInline: Boolean = false,
    override val isTailrec: Boolean = false,
    override val isSuspend: Boolean = false,
    override val isExternal: Boolean = false
) : CirFunctionModifiers {
    companion object {
        fun areEqual(a: CirFunctionModifiers, b: CirFunctionModifiers) =
            a.isOperator == b.isOperator && a.isInfix == b.isInfix && a.isInline == b.isInline
                    && a.isTailrec == b.isTailrec && a.isSuspend == b.isSuspend && a.isExternal == b.isExternal
    }
}
