/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.FileEditorManagerTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingUtil
import org.jetbrains.kotlin.idea.scratch.actions.ClearScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.ScratchCompilationSupport
import org.jetbrains.kotlin.idea.scratch.output.InlayScratchFileRenderer
import org.jetbrains.kotlin.idea.scratch.output.getInlays
import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File

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
            val options = listOf("-d", outputDir.path)
            KotlinTestUtils.compileJavaFiles(javaFiles, options)
        }

        MockLibraryUtil.compileKotlin(baseDir.path, outputDir)

        PsiTestUtil.setCompilerOutputPath(myFixture.module, outputDir.path, false)

        val mainFileName = "$dirName/${getTestName(true)}.kts"
        doCompilingTest(mainFileName)

        launchAction(ClearScratchAction())

        doReplTest(mainFileName)

        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.getModuleExtension(CompilerModuleExtension::class.java).inheritCompilerOutputPath(true)
        }
    }

    fun doTest(fileName: String, isRepl: Boolean) {
        val sourceFile = File(testDataPath, fileName)
        val fileText = sourceFile.readText().inlinePropertiesValues(isRepl)

        configureScratchByText(sourceFile.name, fileText)

        if (!KotlinHighlightingUtil.shouldHighlight(myFixture.file)) error("Highlighting for scratch file is switched off")

        launchScratch()
        waitUntilScratchFinishes()

        val actualOutput = getFileTextWithInlays()

        val expectedFileName = if (isRepl) {
            fileName.replace(".kts", ".repl.after")
        } else {
            fileName.replace(".kts", ".comp.after")
        }
        val expectedFile = File(testDataPath, expectedFileName)
        KotlinTestUtils.assertEqualsToFile(expectedFile, actualOutput.toString())
    }

    protected fun String.inlinePropertiesValues(
        isRepl: Boolean = false,
        isInteractiveMode: Boolean = false
    ): String {
        return replace("~REPL_MODE~", isRepl.toString()).replace("~INTERACTIVE_MODE~", isInteractiveMode.toString())
    }

    private fun getFileTextWithInlays(): StringBuilder {
        val doc = myFixture.getDocument(myFixture.file) ?: error("Document for ${myFixture.file.name} is null")
        val actualOutput = StringBuilder(myFixture.file.text)
        for (line in doc.lineCount - 1 downTo 0) {
            getInlays(doc.getLineStartOffset(line), doc.getLineEndOffset(line))
                .forEach { inlay ->
                    val str = inlay.toString()
                    val offset = doc.getLineEndOffset(line)
                    actualOutput.insert(
                        offset,
                        "${str.takeWhile { it.isWhitespace() }}// ${str.trim()}"
                    )
                }
        }
        return actualOutput
    }

    protected fun getInlays(start: Int = 0, end: Int = myFixture.file.textLength): List<InlayScratchFileRenderer> {
        val inlineElementsInRange = myFixture.editor.inlayModel.getInlays(start, end)
        return inlineElementsInRange.map { it.renderer as InlayScratchFileRenderer }
    }

    protected fun configureScratchByText(name: String, text: String): ScratchTopPanel {
        val scratchFile = ScratchRootType.getInstance().createScratchFile(
            project,
            name,
            KotlinLanguage.INSTANCE,
            text,
            ScratchFileService.Option.create_if_missing
        ) ?: error("Couldn't create scratch file")

        myFixture.openFileInEditor(scratchFile)

        ScriptDependenciesManager.updateScriptDependenciesSynchronously(scratchFile, project)

        val (_, scratchPanel) = getEditorWithScratchPanel(myManager, myFixture.file.virtualFile)
            ?: error("Couldn't find scratch panel")

        configureOptions(scratchPanel, text, myFixture.module)

        return scratchPanel
    }

    protected fun launchScratch() {
        val action = RunScratchAction()
        launchAction(action)
    }

    protected fun launchAction(action: AnAction) {
        val e = getActionEvent(myFixture.file.virtualFile, action)
        action.beforeActionPerformedUpdate(e)
        Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
        action.actionPerformed(e)
    }

    protected fun waitUntilScratchFinishes() {
        UIUtil.dispatchAllInvocationEvents()

        val start = System.currentTimeMillis()
        // wait until output is displayed in editor or for 1 minute
        while (ScratchCompilationSupport.isAnyInProgress() && (System.currentTimeMillis() - start) < 60000) {
            Thread.sleep(100)
        }

        UIUtil.dispatchAllInvocationEvents()
    }

    private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
        val context = MapDataContext()
        context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
        context.put(CommonDataKeys.PROJECT, project)
        context.put(CommonDataKeys.EDITOR, myFixture.editor)
        return TestActionEvent(context, action)
    }

    protected fun testScratchText(): String {
        return File(testDataPath, "idea/testData/scratch/custom/test_scratch.kts").readText()
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()


    override fun getProjectDescriptor(): com.intellij.testFramework.LightProjectDescriptor {
        val testName = getTestName(false)

        return when {
            testName.endsWith("WithKotlinTest") -> INSTANCE_WITH_KOTLIN_TEST
            testName.endsWith("NoRuntime") -> INSTANCE_WITHOUT_RUNTIME
            else -> KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK
        }
    }

    override fun setUp() {
        super.setUp()

        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())

        PluginTestCaseBase.addJdk(myFixture.projectDisposable) { PluginTestCaseBase.fullJdk() }
    }

    override fun tearDown() {
        if (myFixture.file != null) {
            val (_, scratchPanel) = getEditorWithScratchPanel(myManager, myFixture.file.virtualFile)
                ?: error("Couldn't find scratch panel")
            scratchPanel.scratchFile.replScratchExecutor?.stop()
        }

        super.tearDown()

        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())

        ScratchFileService.getInstance().scratchesMapping.mappings.forEach { file, _ ->
            runWriteAction { file.delete(this) }
        }
    }

    companion object {
        private val INSTANCE_WITH_KOTLIN_TEST = object : KotlinWithJdkAndRuntimeLightProjectDescriptor(
            arrayListOf(
                ForTestCompileRuntime.runtimeJarForTests(),
                PathUtil.kotlinPathsForDistDirectory.kotlinTestPath
            )
        ) {
            override fun getSdk() = PluginTestCaseBase.fullJdk()
        }

        private val INSTANCE_WITHOUT_RUNTIME = object : KotlinLightProjectDescriptor() {
            override fun getSdk() = PluginTestCaseBase.fullJdk()
        }

        fun configureOptions(
            scratchPanel: ScratchTopPanel,
            fileText: String,
            module: Module?
        ) {
            if (InTextDirectivesUtils.getPrefixedBoolean(fileText, "// INTERACTIVE_MODE: ") != true) {
                scratchPanel.setInteractiveMode(false)
            }

            if (InTextDirectivesUtils.getPrefixedBoolean(fileText, "// REPL_MODE: ") == true) {
                scratchPanel.setReplMode(true)
            }

            if (module != null && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_MODULE")) {
                scratchPanel.setModule(module)
            }
        }

    }
}