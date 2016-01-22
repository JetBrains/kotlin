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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.highlighting.actions.HighlightUsagesAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.ExpectedHighlightingData
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor

abstract class AbstractUsageHighlightingTest: KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        // Not standard <caret> to leave it in text after configureByFile and remove manually after collecting highlighting information
        val CARET_TAG = "~"
    }

    protected fun doTest(filePath: String) {
        myFixture.configureByFile(filePath)
        val document = myFixture.editor.document
        val data = ExpectedHighlightingData(document, false, false, true, false, myFixture.file)
        data.init()

        val caret = document.text.indexOf(CARET_TAG)
        assert(caret != -1) { "Caret marker '$CARET_TAG' expected" }

        WriteCommandAction.runWriteCommandAction(myFixture.project) {
            document.deleteString(caret, caret + CARET_TAG.length)
        }

        editor.caretModel.moveToOffset(caret)

        myFixture.testAction(HighlightUsagesAction())
        val highlighters = myFixture.editor.markupModel.allHighlighters

        val infos = highlighters.map { highlighter ->
            var startOffset = highlighter.startOffset
            var endOffset = highlighter.endOffset

            if (startOffset > caret) startOffset += CARET_TAG.length
            if (endOffset > caret) endOffset += CARET_TAG.length

            HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(startOffset, endOffset).create()
        }

        data.checkResult(infos, StringBuilder(document.text).insert(caret, CARET_TAG).toString())
    }



    override fun getProjectDescriptor() = KotlinLightProjectDescriptor.INSTANCE
    override fun getTestDataPath() = ""
}
