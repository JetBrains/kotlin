package org.jetbrains.kotlin.idea.intentions.declarations

import com.intellij.codeInsight.editorActions.JoinLinesHandler
import com.intellij.ide.DataManager
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractJoinLinesTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    fun doTest(unused: String) {
        val path = fileName()
        myFixture.configureByFile(path)
        val dataContext = DataManager.getInstance().getDataContext(editor.contentComponent)
        myFixture.project.executeWriteCommand("") {
            JoinLinesHandler(null).execute(editor, editor.caretModel.currentCaret, dataContext)
        }
        val afterFilePath = "$path.after"
        try {
            myFixture.checkResultByFile(afterFilePath)
        }
        catch (e: FileComparisonFailure) {
            KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
        }
    }
}