/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.TestFunctionModifiers
import org.jetbrains.kotlin.descriptors.commonizer.TestFunctionModifiers.Companion.areEqual
import org.jetbrains.kotlin.descriptors.commonizer.ir.FunctionModifiers
import org.jetbrains.kotlin.descriptors.commonizer.mockClassType
import org.jetbrains.kotlin.descriptors.commonizer.mockFunction
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.junit.Test

@TypeRefinement
class DefaultFunctionModifiersCommonizerTest : AbstractCommonizerTest<SimpleFunctionDescriptor, FunctionModifiers>() {

    @Test
    fun allDefault() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create().toMockFunction(),
        create().toMockFunction()
    )

    @Test
    fun allSuspend() = doTestSuccess(
        create(isSuspend = true),
        create(isSuspend = true).toMockFunction(),
        create(isSuspend = true).toMockFunction(),
        create(isSuspend = true).toMockFunction()
    )

    @Test(expected = IllegalStateException::class)
    fun suspendAndNotSuspend() = doTestFailure(
        create(isSuspend = true).toMockFunction(),
        create(isSuspend = true).toMockFunction(),
        create().toMockFunction()
    )

    @Test(expected = IllegalStateException::class)
    fun notSuspendAndSuspend() = doTestFailure(
        create().toMockFunction(),
        create().toMockFunction(),
        create(isSuspend = true).toMockFunction()
    )

    @Test
    fun allOperator() = doTestSuccess(
        create(isOperator = true),
        create(isOperator = true).toMockFunction(),
        create(isOperator = true).toMockFunction(),
        create(isOperator = true).toMockFunction()
    )

    @Test
    fun notOperatorAndOperator() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create(isOperator = true).toMockFunction(),
        create(isOperator = true).toMockFunction()
    )

    @Test
    fun operatorAndNotOperator() = doTestSuccess(
        create(),
        create(isOperator = true).toMockFunction(),
        create(isOperator = true).toMockFunction(),
        create().toMockFunction()
    )

    @Test
    fun allInfix() = doTestSuccess(
        create(isInfix = true),
        create(isInfix = true).toMockFunction(),
        create(isInfix = true).toMockFunction(),
        create(isInfix = true).toMockFunction()
    )

    @Test
    fun notInfixAndInfix() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create(isInfix = true).toMockFunction(),
        create(isInfix = true).toMockFunction()
    )

    @Test
    fun infixAndNotInfix() = doTestSuccess(
        create(),
        create(isInfix = true).toMockFunction(),
        create(isInfix = true).toMockFunction(),
        create().toMockFunction()
    )

    @Test
    fun allInline() = doTestSuccess(
        create(isInline = true),
        create(isInline = true).toMockFunction(),
        create(isInline = true).toMockFunction(),
        create(isInline = true).toMockFunction()
    )

    @Test
    fun notInlineAndInline() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create(isInline = true).toMockFunction(),
        create(isInline = true).toMockFunction()
    )

    @Test
    fun inlineAndNotInline() = doTestSuccess(
        create(),
        create(isInline = true).toMockFunction(),
        create(isInline = true).toMockFunction(),
        create().toMockFunction()
    )

    @Test
    fun allTailrec() = doTestSuccess(
        create(isTailrec = true),
        create(isTailrec = true).toMockFunction(),
        create(isTailrec = true).toMockFunction(),
        create(isTailrec = true).toMockFunction()
    )

    @Test
    fun notTailrecAndTailrec() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create(isTailrec = true).toMockFunction(),
        create(isTailrec = true).toMockFunction()
    )

    @Test
    fun tailrecAndNotTailrec() = doTestSuccess(
        create(),
        create(isTailrec = true).toMockFunction(),
        create(isTailrec = true).toMockFunction(),
        create().toMockFunction()
    )

    @Test
    fun allExternal() = doTestSuccess(
        create(isExternal = true),
        create(isExternal = true).toMockFunction(),
        create(isExternal = true).toMockFunction(),
        create(isExternal = true).toMockFunction()
    )

    @Test
    fun notExternalAndExternal() = doTestSuccess(
        create(),
        create().toMockFunction(),
        create(isExternal = true).toMockFunction(),
        create(isExternal = true).toMockFunction()
    )

    @Test
    fun externalAndNotExternal() = doTestSuccess(
        create(),
        create(isExternal = true).toMockFunction(),
        create(isExternal = true).toMockFunction(),
        create().toMockFunction()
    )

    override fun createCommonizer() = FunctionModifiersCommonizer.default()
    override fun isEqual(a: FunctionModifiers?, b: FunctionModifiers?) = (a === b) || (a != null && b != null && areEqual(a, b))
}

private typealias create = TestFunctionModifiers

@TypeRefinement
private fun TestFunctionModifiers.toMockFunction() = mockFunction(
    name = "myFunction",
    returnType = mockClassType("kotlin.String"),
    modifiers = this
)
