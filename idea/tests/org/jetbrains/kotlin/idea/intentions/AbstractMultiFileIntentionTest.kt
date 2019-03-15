/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractMultiFileIntentionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.getProjectDescriptor()
        val testFile = File(testDataPath, fileName())
        val config = JsonParser().parse(FileUtil.loadFile(testFile, true)) as JsonObject
        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        return if (withRuntime)
            KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        else
            KotlinLightProjectDescriptor.INSTANCE
    }

    protected fun doTest(path: String) {
        val testFile = File(path)
        val config = JsonParser().parse(FileUtil.loadFile(testFile, true)) as JsonObject
        val mainFilePath = config.getString("mainFile")
        val intentionAction = Class.forName(config.getString("intentionClass")).newInstance() as IntentionAction
        val isApplicableExpected = config["isApplicable"]?.asBoolean ?: true

        doTest(path) { rootDir ->
            val mainFile = myFixture.configureFromTempProjectFile(mainFilePath)
            val conflictFile = rootDir.findFileByRelativePath("$mainFilePath.conflicts")

            try {
                Assert.assertTrue("isAvailable() for ${intentionAction::class.java} should return $isApplicableExpected",
                                  isApplicableExpected == intentionAction.isAvailable(project, editor, mainFile))
                config.getNullableString("intentionText")?.let {
                    TestCase.assertEquals("Intention text mismatch", it, intentionAction.text)
                }

                if (isApplicableExpected) {
                    project.executeWriteCommand(intentionAction.text) {
                        intentionAction.invoke(project, editor, mainFile)
                    }
                }

                assert(conflictFile == null) { "Conflict file $conflictFile should not exist" }
            }
            catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
                val expectedConflicts = LoadTextUtil.loadText(conflictFile!!).toString().trim()
                assertEquals(expectedConflicts, e.message)
            }
       }
    }

    protected fun doTest(path: String, action: (VirtualFile) -> Unit) {
        val beforeDir = path.removePrefix(testDataPath).substringBeforeLast('/') + "/before"
        val beforeVFile = myFixture.copyDirectoryToProject(beforeDir, "")
        PsiDocumentManager.getInstance(myFixture.project).commitAllDocuments()

        val afterDir = beforeDir.substringBeforeLast("/") + "/after"
        val afterDirIOFile = File(testDataPath, afterDir)
        val afterVFile = LocalFileSystem.getInstance().findFileByIoFile(afterDirIOFile)!!
        UsefulTestCase.refreshRecursively(afterVFile)

        action(beforeVFile)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
        PlatformTestUtil.assertDirectoriesEqual(afterVFile, beforeVFile)
    }
}