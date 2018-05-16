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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.FileEditorManagerTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.compatibility.projectDisposableEx
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchFileRenderer
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractScratchRunActionTest : FileEditorManagerTestCase() {

    fun doReplTest(fileName: String) {
        doTest(fileName, true)
    }

    fun doCompilingTest(fileName: String) {
        doTest(fileName, false)
    }

    fun doMultiFileTest(dirName: String) {
        val javaFiles = arrayListOf<File>()
        val kotlinFiles = arrayListOf<File>()
        val baseDir = File(testDataPath, dirName)
        baseDir.walk().forEach {
            if (it.isFile) {
                if (it.extension == "java") javaFiles.add(it)
                if (it.extension == "kt") kotlinFiles.add(it)
            }
        }

        javaFiles.forEach { myFixture.copyFileToProject(it.path, FileUtil.getRelativePath(baseDir, it)!!) }
        kotlinFiles.forEach { myFixture.copyFileToProject(it.path, FileUtil.getRelativePath(baseDir, it)!!) }

        val outputDir = myFixture.tempDirFixture.findOrCreateDir("out")

        MockLibraryUtil.compileKotlin(baseDir.path, File(outputDir.path))

        if (javaFiles.isNotEmpty()) {
            val options = Arrays.asList("-d", outputDir.path)
            KotlinTestUtils.compileJavaFiles(javaFiles, options)
        }

        PsiTestUtil.setCompilerOutputPath(myModule, outputDir.url, false)

        val mainFileName = "$dirName/${getTestName(true)}.kts"
        doCompilingTest(mainFileName)
        doReplTest(mainFileName)

        ModuleRootModificationUtil.updateModel(myModule) { model ->
            model.getModuleExtension(CompilerModuleExtension::class.java).inheritCompilerOutputPath(true)
        }
    }

    fun doTest(fileName: String, isRepl: Boolean) {
        val sourceFile = File(testDataPath, fileName)
        val fileText = sourceFile.readText()

        val scratchFile = ScratchRootType.getInstance().createScratchFile(
            project,
            sourceFile.name,
            KotlinLanguage.INSTANCE,
            fileText,
            ScratchFileService.Option.create_if_missing
        ) ?: error("Couldn't create scratch file ${sourceFile.path}")

        myFixture.openFileInEditor(scratchFile)

        val psiFile = PsiManager.getInstance(project).findFile(scratchFile) ?: error("Couldn't find psi file ${sourceFile.path}")
        val (editor, scratchPanel) = getEditorWithScratchPanel(myManager, scratchFile)?: error("Couldn't find scratch panel")
        scratchPanel.setReplMode(isRepl)

        val action = RunScratchAction(scratchPanel)
        val event = getActionEvent(scratchFile, action)
        launchAction(event, action)

        UIUtil.dispatchAllInvocationEvents()

        val start = System.currentTimeMillis()
        // wait until output is displayed in editor or for 1 minute
        while (!event.presentation.isEnabled && (System.currentTimeMillis() - start) < 60000) {
            Thread.sleep(100)
        }

        UIUtil.dispatchAllInvocationEvents()

        val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: error("Document for ${psiFile.name} is null")

        val actualOutput = StringBuilder(psiFile.text)
        for (line in doc.lineCount - 1 downTo 0) {
            editor.editor.inlayModel.getInlineElementsInRange(
                doc.getLineStartOffset(line),
                doc.getLineEndOffset(line)
            ).map { it.renderer }
                .filterIsInstance<InlayScratchFileRenderer>()
                .forEach {
                    val str = it.toString()
                    val offset = doc.getLineEndOffset(line); actualOutput.insert(offset, "    // $str")
                }
        }

        val expectedFileName = if (isRepl) {
            fileName.replace(".kts", ".repl.after")
        } else {
            fileName.replace(".kts", ".comp.after")
        }
        val expectedFile = File(testDataPath, expectedFileName)
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
        context.put<Project>(CommonDataKeys.PROJECT, project)
        return TestActionEvent(context, action)
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    override fun setUp() {
        super.setUp()

        PluginTestCaseBase.addJdk(myFixture.projectDisposableEx) { PluginTestCaseBase.fullJdk() }
    }

    override fun tearDown() {
        super.tearDown()

        ScratchFileService.getInstance().scratchesMapping.mappings.forEach { file, _ ->
            runWriteAction { file.delete(this) }
        }
    }
}