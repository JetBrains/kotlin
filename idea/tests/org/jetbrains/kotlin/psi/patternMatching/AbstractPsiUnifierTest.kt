/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.patternMatching

import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPsiUnifierTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(unused: String) {
        fun findPattern(file: KtFile): KtElement {
            val selectionModel = myFixture.editor.selectionModel
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            val selectionRange = TextRange(start, end)
            return file.findElementAt(start)?.parentsWithSelf?.last {
                (it is KtExpression || it is KtTypeReference || it is KtWhenCondition)
                        && selectionRange.contains(it.textRange ?: TextRange.EMPTY_RANGE)
            } as KtElement
        }

        val file = myFixture.configureByFile(fileName()) as KtFile

        DirectiveBasedActionUtils.checkForUnexpectedErrors(file)

        val actualText =
                findPattern(file)
                        .toRange()
                        .match(file, KotlinPsiUnifier.DEFAULT)
                        .map { it.range.getTextRange().substring(file.getText()!!) }
                        .joinToString("\n\n")
        KotlinTestUtils.assertEqualsToFile(File(testDataPath, "${fileName()}.match"), actualText)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = getProjectDescriptorFromTestName()
}
