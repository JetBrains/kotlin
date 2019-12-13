/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.script.IdeScriptReportSink
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.applySuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManagerExtensions
import org.jetbrains.kotlin.idea.core.script.configuration.loader.FileContentsDependentConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.testingBackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.testScriptConfigurationNotification
import org.jetbrains.kotlin.idea.core.script.hasSuggestedScriptConfiguration
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractScriptConfigurationLoadingTest : AbstractScriptConfigurationTest() {
    lateinit var scriptConfigurationManager: ScriptConfigurationManager

    companion object {
        private var occurredLoadings = 0
        private var currentLoadingScriptConfigurationCallback: (() -> Unit)? = null
        @JvmStatic
        @Suppress("unused")
        fun loadingScriptConfigurationCallback() {
            // this method is called from testData/script/definition/loading/async/template/template.kt
            currentLoadingScriptConfigurationCallback?.invoke()
            occurredLoadings++
        }
    }

    override fun setUp() {
        super.setUp()
        testScriptConfigurationNotification = true

        scriptConfigurationManager = ServiceManager.getService(project, ScriptConfigurationManager::class.java)
    }

    override fun tearDown() {
        super.tearDown()
        testScriptConfigurationNotification = false
        occurredLoadings = 0
        currentLoadingScriptConfigurationCallback = null
    }

    override fun setUpTestProject() {
        addExtensionPointInTest(
            DefaultScriptConfigurationManagerExtensions.LOADER,
            project,
            FileContentsDependentConfigurationLoader(project),
            testRootDisposable
        )

        configureScriptFile("idea/testData/script/definition/loading/async/")
    }

    override fun loadScriptConfigurationSynchronously(script: VirtualFile) {
        // do nothings
    }

    protected fun assertAndDoAllBackgroundTasks() {
        assertDoAllBackgroundTaskAndDoWhileLoading { }
    }

    protected fun assertDoAllBackgroundTaskAndDoWhileLoading(actions: () -> Unit) {
        assertTrue(doAllBackgroundTasksWith(actions))
    }

    protected fun doAllBackgroundTasksWith(actions: () -> Unit): Boolean {
        return scriptConfigurationManager.testingBackgroundExecutor.doAllBackgroundTaskWith {
            currentLoadingScriptConfigurationCallback = {
                actions()
                currentLoadingScriptConfigurationCallback = null
            }
        }
    }

    protected fun assertNoBackgroundTasks() {
        assertTrue(scriptConfigurationManager.testingBackgroundExecutor.noBackgroundTasks())
    }

    protected fun assertAppliedConfiguration(contents: String, file: KtFile = myFile as KtFile) {
        val secondConfiguration = scriptConfigurationManager.getConfiguration(file)!!
        assertEquals(
            StringUtilRt.convertLineSeparators(contents),
            StringUtilRt.convertLineSeparators(secondConfiguration.defaultImports.single().let {
                check(it.startsWith("x_"))
                it.removePrefix("x_")
            })
        )
    }

    protected fun makeChanges(contents: String, file: KtFile = myFile as KtFile) {
        changeContents(contents)

        scriptConfigurationManager.updater.ensureUpToDatedConfigurationSuggested(file)
    }

    protected fun changeContents(contents: String, file: PsiFile = myFile) {
        runWriteAction {
            VfsUtil.saveText(file.virtualFile, contents)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            myFile = psiManager.findFile(file.virtualFile)
        }
    }

    protected fun assertReports(expected: String, file: KtFile = myFile as KtFile) {
        val actual = IdeScriptReportSink.getReports(file.virtualFile).single().message
        assertEquals(expected, actual)
    }

    protected fun assertSuggestedConfiguration(file: KtFile = myFile as KtFile) {
        assertTrue(file.virtualFile.hasSuggestedScriptConfiguration(project))
    }

    protected fun assertAndApplySuggestedConfiguration(file: KtFile = myFile as KtFile) {
        assertTrue(file.virtualFile.applySuggestedScriptConfiguration(project))
    }

    protected fun assertNoSuggestedConfiguration(file: KtFile = myFile as KtFile) {
        assertFalse(file.virtualFile.applySuggestedScriptConfiguration(project))
    }

    protected fun assertNoLoading() {
        assertEquals(0, occurredLoadings)
        occurredLoadings = 0
    }

    protected fun assertSingleLoading() {
        assertEquals(1, occurredLoadings)
        occurredLoadings = 0
    }

    protected fun assertAndLoadInitialConfiguration(file: KtFile = myFile as KtFile) {
        assertNull(scriptConfigurationManager.getConfiguration(file))
        assertAndDoAllBackgroundTasks()
        assertSingleLoading()
        assertAppliedConfiguration(file.text, file)

        checkHighlighting(file)
    }
}