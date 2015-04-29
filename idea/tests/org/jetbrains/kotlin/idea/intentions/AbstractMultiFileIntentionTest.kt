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

package org.jetbrains.kotlin.idea.intentions

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.refactoring.move.MoveAction
import org.jetbrains.kotlin.idea.refactoring.move.getNullableString
import org.jetbrains.kotlin.idea.refactoring.move.getString
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.JetTestUtils
import org.junit.Assert
import java.io.File

public abstract class AbstractMultiFileIntentionTest : KotlinMultiFileTestCase() {
    protected fun doTest(path: String) {
        val config = JsonParser().parse(FileUtil.loadFile(File(path), true)) as JsonObject
        val mainFilePath = config.getString("mainFile")
        val intentionAction = Class.forName(config.getString("intentionClass")).newInstance() as IntentionAction
        val isApplicableExpected = config["isApplicable"]?.getAsBoolean() ?: true

        val withRuntime = config["withRuntime"]?.getAsBoolean() ?: false
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        doTest({ rootDir, rootAfter ->
                   val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
                   val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
                   val editor = EditorFactory.getInstance()!!.createEditor(document, getProject()!!)!!
                   editor.getCaretModel().moveToOffset(extractCaretOffset(document))
                   val mainPsiFile = PsiManager.getInstance(getProject()!!).findFile(mainFile)!!

                   try {
                       Assert.assertTrue("isAvailable() for ${intentionAction.javaClass} should return $isApplicableExpected",
                                         isApplicableExpected == intentionAction.isAvailable(getProject(), editor, mainPsiFile))
                       config.getNullableString("intentionText")?.let {
                           TestCase.assertEquals("Intention text mismatch", it, intentionAction.getText())
                       }

                       if (isApplicableExpected) {
                           getProject().executeWriteCommand(intentionAction.getText()) {
                               intentionAction.invoke(getProject(), editor, mainPsiFile)
                           }
                       }
                   }
                   finally {
                       PsiDocumentManager.getInstance(getProject()!!).commitAllDocuments()
                       FileDocumentManager.getInstance().saveAllDocuments()

                       EditorFactory.getInstance()!!.releaseEditor(editor)

                       if (withRuntime) {
                           ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
                       }
                   }
               },
               getTestDirName(true))
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.lastIndexOf('_')).replace('_', '/')
    }

    protected override fun getTestRoot() : String {
        return "/multiFileIntentions/"
    }

    protected override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}