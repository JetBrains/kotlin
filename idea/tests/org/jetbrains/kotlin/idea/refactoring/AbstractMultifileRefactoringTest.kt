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

package org.jetbrains.kotlin.idea.refactoring

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.refactoring.move.MoveAction
import org.jetbrains.kotlin.idea.refactoring.move.runMoveRefactoring
import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.extractMultipleMarkerOffsets
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractMultifileRefactoringTest : KotlinLightCodeInsightFixtureTestCase() {
    interface RefactoringAction {
        fun runRefactoring(rootDir: VirtualFile, mainFile: PsiFile, elementsAtCaret: List<PsiElement>, config: JsonObject)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.getProjectDescriptor()

        val testConfigurationFile = File(super.getTestDataPath(), fileName())
        val config = loadTestConfiguration(testConfigurationFile)
        val withRuntime = config["withRuntime"]?.asBoolean ?: false
        if (withRuntime) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        return KotlinLightProjectDescriptor.INSTANCE
    }

    protected abstract fun runRefactoring(path: String, config: JsonObject, rootDir: VirtualFile, project: Project)

    protected fun doTest(path: String) {
        val testFile = File(path)
        val config = JsonParser().parse(FileUtil.loadFile(testFile, true)) as JsonObject

        doTestCommittingDocuments(testFile) { rootDir ->
            runRefactoring(path, config, rootDir, project)
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        val endIndex = testName.lastIndexOf('_')
        if (endIndex < 0) return testName
        return testName.substring(0, endIndex).replace('_', '/')
    }

    override fun getTestDataPath() = super.getTestDataPath() + "/" + getTestDirName(true)

    protected fun doTestCommittingDocuments(testFile: File, action: (VirtualFile) -> Unit) {
        val beforeVFile = myFixture.copyDirectoryToProject("before", "")
        PsiDocumentManager.getInstance(myFixture.project).commitAllDocuments()

        val afterDir = File(testFile.parentFile, "after")
        val afterVFile = LocalFileSystem.getInstance().findFileByIoFile(afterDir)?.apply {
            UsefulTestCase.refreshRecursively(this)
        }

        action(beforeVFile)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
        PlatformTestUtil.assertDirectoriesEqual(afterVFile, beforeVFile)
    }
}

fun runRefactoringTest(
        path: String,
        config: JsonObject,
        rootDir: VirtualFile,
        project: Project,
        action: AbstractMultifileRefactoringTest.RefactoringAction
) {
    val testDir = path.substring(0, path.lastIndexOf("/"))
    val mainFilePath = config.getNullableString("mainFile") ?: config.getAsJsonArray("filesToMove").first().asString

    val conflictFile = File(testDir + "/conflicts.txt")

    val mainFile = rootDir.findFileByRelativePath(mainFilePath)!!
    val mainPsiFile = PsiManager.getInstance(project).findFile(mainFile)!!
    val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
    val editor = EditorFactory.getInstance()!!.createEditor(document, project)!!

    val caretOffsets = document.extractMultipleMarkerOffsets(project)
    val elementsAtCaret = caretOffsets.map {
        TargetElementUtil.getInstance().findTargetElement(
                editor,
                TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.ELEMENT_NAME_ACCEPTED,
                it
        )!!
    }

    try {
        action.runRefactoring(rootDir, mainPsiFile, elementsAtCaret, config)

        assert(!conflictFile.exists())
    }
    catch(e: BaseRefactoringProcessor.ConflictsInTestsException) {
        KotlinTestUtils.assertEqualsToFile(conflictFile, e.messages.distinct().sorted().joinToString("\n"))

        BaseRefactoringProcessor.ConflictsInTestsException.setTestIgnore(true)

        // Run refactoring again with ConflictsInTestsException suppressed
        action.runRefactoring(rootDir, mainPsiFile, elementsAtCaret, config)
    }
    finally {
        BaseRefactoringProcessor.ConflictsInTestsException.setTestIgnore(false)

        EditorFactory.getInstance()!!.releaseEditor(editor)
    }
}