/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.patternMatching

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parents
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import java.io.File
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

abstract class AbstractPsiUnifierTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(filePath: String) {
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

        myFixture.configureByFile(filePath)
        val file = myFixture.file as KtFile

        DirectiveBasedActionUtils.checkForUnexpectedErrors(file)

        val actualText =
                findPattern(file)
                        .toRange()
                        .match(file, KotlinPsiUnifier.DEFAULT)
                        .map { it.range.getTextRange().substring(file.getText()!!) }
                        .joinToString("\n\n")
        KotlinTestUtils.assertEqualsToFile(File("$filePath.match"), actualText)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = getProjectDescriptorFromTestName()
}
