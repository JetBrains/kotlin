/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.LightPlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class TypedKtxTest : LightCodeInsightTestCase() {

    fun testNewChildCaretPosition() {
        doCharTypeTest(
                '\n',
                """
                |fun test() {
                |	<LinearLayout><caret>
                |	</LinearLayout>
                |}
                """,
                """
                |fun test() {
                |	<LinearLayout>
                |		<caret>
                |	</LinearLayout>
                |}
                """,
                enableSmartEnterWithTabs()
        )
    }

    fun testNewlineCaretPosition() {
        doCharTypeTest(
                '\n',
                """
                |fun test() {
                |	<LinearLayout><caret></LinearLayout>
                |}
                """,
                /*
                TODO: Current behavior is not ideal, and should be changed at some point.
                Ideally, the result behavior should be more like the `{<caret>}` behavior, like below:
                """
                |fun test() {
                |	<LinearLayout>
                |	    <caret>
                |   </LinearLayout>
                |}
                """
                */
                """
                |fun test() {
                |	<LinearLayout>
                |	<caret></LinearLayout>
                |}
                """,
                enableSmartEnterWithTabs()
        )
    }

    private fun enableSmartEnterWithTabs(): () -> Unit = {
        val project = LightPlatformTestCase.getProject()
        val indentOptions = CodeStyleSettingsManager.getInstance(project).currentSettings.getIndentOptions(KotlinFileType.INSTANCE)
        indentOptions.USE_TAB_CHARACTER = true
        indentOptions.SMART_TABS = true
    }

    private fun doCharTypeTest(ch: Char, beforeText: String, afterText: String, settingsModifier: (() -> Unit)? = null) {
        try {
            if (settingsModifier != null) {
                settingsModifier()
            }

            LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", beforeText.trimMargin())
            EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), ch)
            checkResultByText(afterText.trimMargin())
        }
        finally {
            if (settingsModifier != null) {
                val project = LightPlatformTestCase.getProject()
                CodeStyleSettingsManager.getSettings(project).clearCodeStyleSettings()
            }
        }
    }

    private fun doLtGtTestNoAutoClose(initText: String) {
        doLtGtTest(initText, false)
    }

    private fun doLtGtTest(initText: String, shouldCloseBeInsert: Boolean) {
        LightPlatformCodeInsightTestCase.configureFromFileText("a.kt", initText)

        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), '<')
        checkResultByText(if (shouldCloseBeInsert) initText.replace("<caret>", "<<caret>>") else initText.replace("<caret>", "<<caret>"))

        EditorTestUtil.performTypingAction(LightPlatformCodeInsightTestCase.getEditor(), EditorTestUtil.BACKSPACE_FAKE_CHAR)
        checkResultByText(initText)
    }

    private fun doLtGtTest(initText: String) {
        doLtGtTest(initText, true)
    }
}
