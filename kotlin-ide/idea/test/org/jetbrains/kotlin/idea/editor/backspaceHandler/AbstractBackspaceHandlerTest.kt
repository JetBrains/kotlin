/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.backspaceHandler

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import java.io.File

abstract class AbstractBackspaceHandlerTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureByText("a.kt", loadFile(path))
        EditorTestUtil.executeAction(editor, IdeActions.ACTION_EDITOR_BACKSPACE)
        myFixture.checkResult(loadFile("$path.after"))
    }

    private fun loadFile(path: String) = FileUtil.loadFile(File(path), true)
}
