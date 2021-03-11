/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class LazyKtLambdaExpressionTest : LazyElementTypeTestBase<KtLambdaExpression>(KtLambdaExpression::class.java) {
    fun testSplitArrow() = reparse("val t = { a: Int -<caret>> }", ' ')
    fun testDeleteArrow() = reparse("val t = { a: Int -><caret> }", EditorTestUtil.BACKSPACE_FAKE_CHAR)

    fun testReformatNearArrow() = noReparse("val t = { a: Int<caret>-> }", ' ')
    fun testChangeAfterArrow() = noReparse("val t = { a: Int -> <caret> }", 'a')
    fun testDeleteIrrelevantArrow() = noReparse(
        "val t = { a: Int -> (1..3).filter { b -><caret> b > 2 } }",
        EditorTestUtil.BACKSPACE_FAKE_CHAR
    )

    fun testReformatNearLambdaStart() = noReparse("val t = {<caret>a: Int -> }", ' ')
    fun testNoArrow() = noReparse("val t = { <caret> }", 'a')

    fun testAfterRemovingParameterComma() = reparse(inIf("{t,<caret>}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingNoParameterComma() = noReparse(inIf("{,<caret>}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingNotLastParameterComma() = noReparse(inIf("{a, b,<caret>}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingSecondParameter() = noReparse(inIf("{a,b<caret>}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterFirstParameterRenamed() = noReparse(inIf("{a<caret>,}"), 'b')

    fun testAfterRemovingFirstParameterWithOther() = reparse(inIf("{a<caret>,b}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterRemovingFirstParameter() = reparse(inIf("{a<caret>,}"), EditorTestUtil.BACKSPACE_FAKE_CHAR)
    fun testAfterTypeComma() = reparse(inIf("{a<caret>}"), ',')
}