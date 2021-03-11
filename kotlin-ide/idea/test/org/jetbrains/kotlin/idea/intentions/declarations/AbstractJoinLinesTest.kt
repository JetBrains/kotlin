/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.codeInsight.editorActions.JoinLinesHandler
import com.intellij.ide.DataManager
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import java.io.File

abstract class AbstractJoinLinesTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    fun doTest(unused: String) {
        myFixture.configureByFile(fileName())

        val dataContext = DataManager.getInstance().getDataContext(editor.contentComponent)

        myFixture.project.executeWriteCommand("Join lines") {
            JoinLinesHandler(null).execute(editor, editor.caretModel.currentCaret, dataContext)
        }

        val testFile = testDataFile()
        val expectedFile = File(testFile.parentFile, testFile.name + ".after")
        myFixture.checkResultByFile(expectedFile)
    }
}