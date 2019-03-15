/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.backspaceHandler

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
import java.io.File

abstract class AbstractBackspaceHandlerTest : LightCodeInsightTestCase() {
    fun doTest(path: String) {
        configureFromFileText("a.kt", loadFile(path))
        EditorTestUtil.executeAction(getEditor(), IdeActions.ACTION_EDITOR_BACKSPACE)
        checkResultByText(loadFile(path + ".after"))
    }

    private fun loadFile(path: String) = FileUtil.loadFile(File(path), true)
}
