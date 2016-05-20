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
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.refactoring.util.CommonRefactoringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractMultiFileIntentionTest : KotlinMultiFileTestCase() {
    protected fun doTest(path: String) {
        val config = JsonParser().parse(FileUtil.loadFile(File(path), true)) as JsonObject
        val mainFilePath = config.getString("mainFile")
        val intentionAction = Class.forName(config.getString("intentionClass")).newInstance() as IntentionAction
        val isApplicableExpected = config["isApplicable"]?.asBoolean ?: true

        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        doTest({ rootDir, rootAfter ->
                   val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
                   val conflictFile = rootDir.findFileByRelativePath("$mainFilePath.conflicts")
                   val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
                   val editor = EditorFactory.getInstance()!!.createEditor(document, project!!)!!
                   editor.caretModel.moveToOffset(document.extractMarkerOffset(project))
                   val mainPsiFile = PsiManager.getInstance(project!!).findFile(mainFile)!!

                   try {
                       Assert.assertTrue("isAvailable() for ${intentionAction.javaClass} should return $isApplicableExpected",
                                         isApplicableExpected == intentionAction.isAvailable(project, editor, mainPsiFile))
                       config.getNullableString("intentionText")?.let {
                           TestCase.assertEquals("Intention text mismatch", it, intentionAction.text)
                       }

                       if (isApplicableExpected) {
                           project.executeWriteCommand(intentionAction.text) {
                               intentionAction.invoke(project, editor, mainPsiFile)
                           }
                       }

                       assert(conflictFile == null) { "Conflict file $conflictFile should not exist" }
                   }
                   catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
                       KotlinTestUtils.assertEqualsToFile(File(conflictFile!!.path), e.message!!)
                   }
                   finally {
                       PsiDocumentManager.getInstance(project!!).commitAllDocuments()
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

    override fun getTestRoot() : String {
        return "/multiFileIntentions/"
    }

    override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}