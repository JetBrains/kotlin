/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LazyKtBlockExpressionTest : LazyElementTypeTestBase<KtBlockExpression>(KtBlockExpression::class.java) {

    fun testSimpleReparse() = noReparse(inIf(" { a<caret>b }"), 'c')

    fun testSimpleNotReparse() = reparse(inIf(" { a-<caret>b }"), '>')

    fun testSplitArrow() = reparse(inIf("{ a: Int -<caret>> }"), ' ')

    fun testDeleteArrow() = reparse(inIf("{ a: Int -<caret>> }"), EditorTestUtil.BACKSPACE_FAKE_CHAR)

    fun testImbalance1() = noReparse(inIf(" { {}<caret> }"), EditorTestUtil.BACKSPACE_FAKE_CHAR)

    fun testImbalance2() = noReparse(inIf(" { }<caret>"), EditorTestUtil.BACKSPACE_FAKE_CHAR)

    fun testBlockWithArrowInside() = noReparse(inIf(" { { a: Int -<caret> a } }"), '>')

    fun testBlockWithArrowInside2() = noReparse(inIf(" { } a: Int -<caret> a } }"), '>')

    fun testBlockWithCommaAsLambdaArgument() = reparse(inIf(" { a<caret>  }"), ',')

    fun testBlockWithCommaAsBlockContent() = noReparse(inIf(" { a.b<caret> c.d = 3  }"), ',')

    fun testBlockWithCommaAsBlockContent2() = noReparse(inIf(" { a(b<caret> c)  }"), ',')
}