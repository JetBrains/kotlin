/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
