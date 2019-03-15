/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.highlighting.actions.HighlightUsagesAction
import com.intellij.testFramework.ExpectedHighlightingData
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.extractMarkerOffset

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

        val caret = document.extractMarkerOffset(project, CARET_TAG)
        assert(caret != -1) { "Caret marker '$CARET_TAG' expected" }
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
