/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.FileEditorManagerTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingUtil
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.ScratchCompilationSupport
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchFileRenderer
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
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

        val outputDir = createTempDir(dirName)

        if (javaFiles.isNotEmpty()) {
            val options = Arrays.asList("-d", outputDir.path)
            KotlinTestUtils.compileJavaFiles(javaFiles, options)
        }

        MockLibraryUtil.compileKotlin(baseDir.path, outputDir)

        PsiTestUtil.setCompilerOutputPath(myModule, outputDir.path, false)

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

        ScriptDependenciesManager.updateScriptDependenciesSynchronously(scratchFile, project)

        val psiFile = PsiManager.getInstance(project).findFile(scratchFile) ?: error("Couldn't find psi file ${sourceFile.path}")

        if (!KotlinHighlightingUtil.shouldHighlight(psiFile)) error("Highlighting for scratch file is switched off")

        val (editor, scratchPanel) = getEditorWithScratchPanel(myManager, scratchFile) ?: error("Couldn't find scratch panel")
        scratchPanel.scratchFile.saveOptions {
            copy(isRepl = isRepl, isInteractiveMode = false)
        }

        if (!InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_MODULE")) {
            scratchPanel.setModule(myFixture.module)
        }

        launchScratch(scratchFile)

        UIUtil.dispatchAllInvocationEvents()

        val start = System.currentTimeMillis()
        // wait until output is displayed in editor or for 1 minute
        while (ScratchCompilationSupport.isAnyInProgress() && (System.currentTimeMillis() - start) < 60000) {
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
                    val offset = doc.getLineEndOffset(line); actualOutput.insert(
                    offset,
                    "${str.takeWhile { it.isWhitespace() }}// ${str.trim()}"
                )
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

    private fun launchScratch(scratchFile: VirtualFile) {
        val action = RunScratchAction()
        val e = getActionEvent(scratchFile, action)

        action.beforeActionPerformedUpdate(e)
        Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
        action.actionPerformed(e)
    }

    private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
        val context = MapDataContext()
        context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
        context.put(CommonDataKeys.PROJECT, project)
        context.put(CommonDataKeys.EDITOR, myFixture.editor)
        return TestActionEvent(context, action)
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()


    override fun getProjectDescriptor():  com.intellij.testFramework.LightProjectDescriptor {
        val testName = StringUtil.toLowerCase(getTestName(false))

        return when {
            testName.endsWith("NoRuntime") -> KotlinLightProjectDescriptor.INSTANCE
            else -> KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
        }
    }

    override fun setUp() {
        super.setUp()

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        PluginTestCaseBase.addJdk(myFixture.projectDisposable) { PluginTestCaseBase.fullJdk() }
    }

    override fun tearDown() {
        super.tearDown()

        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())

        ScratchFileService.getInstance().scratchesMapping.mappings.forEach { file, _ ->
            runWriteAction { file.delete(this) }
        }
    }
}