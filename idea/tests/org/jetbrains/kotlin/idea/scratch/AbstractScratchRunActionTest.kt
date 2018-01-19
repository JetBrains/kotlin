/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.FileEditorManagerTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchOutputHandler
import org.jetbrains.kotlin.idea.scratch.ui.scratchTopPanel
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractScratchRunActionTest : FileEditorManagerTestCase() {
    fun doReplTest(fileName: String) {
        doTest(fileName, true)
    }

    fun doCompilingTest(fileName: String) {
        doTest(fileName, false)
    }

    fun doTest(fileName: String, isRepl: Boolean) {
        val sourceFile = File(testDataPath, fileName)
        val fileText = sourceFile.readText()

        val scratchFile = ScratchRootType.getInstance().createScratchFile(
            project,
            sourceFile.name,
            KotlinLanguage.INSTANCE,
            fileText,
            ScratchFileService.Option.create_new_always
        ) ?: error("Couldn't create scratch file ${sourceFile.path}")

        myFixture.openFileInEditor(scratchFile)

        ScratchFileLanguageProvider.createFile(myFixture.file)?.scratchTopPanel?.setReplMode(isRepl)

        val event = getActionEvent(myFixture.file.virtualFile, RunScratchAction())
        launchAction(event, RunScratchAction())

        UIUtil.dispatchAllInvocationEvents()

        val start = System.currentTimeMillis()
        // wait until output is displayed in editor or for 1 minute
        while (!event.presentation.isEnabled && (System.currentTimeMillis() - start) < 60000) {
            Thread.sleep(5000)
        }

        UIUtil.dispatchAllInvocationEvents()

        val editors = FileEditorManager.getInstance(project).getEditors(scratchFile).filterIsInstance<TextEditor>()
        val doc = PsiDocumentManager.getInstance(project).getDocument(myFixture.file) ?: error("Document for ${myFixture.file.name} is null")

        val actualOutput = StringBuilder(myFixture.file.text)
        for (line in doc.lineCount - 1 downTo 0) {
            editors.flatMap { it.editor.inlayModel.getInlineElementsInRange(doc.getLineStartOffset(line), doc.getLineEndOffset(line)) }
                .map { it.renderer }
                .filterIsInstance<InlayScratchOutputHandler.ScratchFileRenderer>()
                .forEach {
                    val str = it.toString()

                    val offset = doc.getLineEndOffset(line)
                    actualOutput.insert(offset, "    // $str")
                }
        }

        val expectedFileName = if (isRepl) fileName.replace(".kts", ".repl.after") else fileName.replace(".kts", ".comp.after")
        val expectedFile = File("$testDataPath/$expectedFileName")
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualOutput.toString())
    }

    private fun launchAction(e: TestActionEvent, action: AnAction) {
        action.beforeActionPerformedUpdate(e)
        Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
        action.actionPerformed(e)
    }

    private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
        val context = MapDataContext()
        context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
        context.put<Project>(CommonDataKeys.PROJECT, myFixture.project)
        return TestActionEvent(context, action)
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
}