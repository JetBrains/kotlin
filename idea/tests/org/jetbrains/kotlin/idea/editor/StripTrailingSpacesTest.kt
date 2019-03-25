/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class StripTrailingSpacesTest : LightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/editor/stripTrailingSpaces/"
    }

    fun testKeepTrailingSpacesInRawString() {
        doTest()
    }

    fun doTest() {
        myFixture.configureByFile("${getTestName(true)}.kt")

        val editorSettings = EditorSettingsExternalizable.getInstance()
        val stripSpaces = editorSettings.stripTrailingSpaces
        try {
            editorSettings.stripTrailingSpaces = EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE
            val doc = myFixture.editor.document
            EditorTestUtil.performTypingAction(editor, ' ')
            PsiDocumentManager.getInstance(project).commitDocument(doc)
            FileDocumentManager.getInstance().saveDocument(doc)
        } finally {
            editorSettings.stripTrailingSpaces = stripSpaces
        }

        myFixture.checkResultByFile("${getTestName(true)}.kt.after")
    }
}
