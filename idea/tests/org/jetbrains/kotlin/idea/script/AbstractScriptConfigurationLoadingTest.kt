/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
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
    val ktFile: KtFile get() = myFile as KtFile
    val virtualFile get() = myFile.virtualFile

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

        addExtensionPointInTest(
            DefaultScriptConfigurationManagerExtensions.LOADER,
            project,
            FileContentsDependentConfigurationLoader(project),
            testRootDisposable
        )

        configureScriptFile("idea/testData/script/definition/loading/async/")
        scriptConfigurationManager = ServiceManager.getService(project, ScriptConfigurationManager::class.java)
    }

    override fun tearDown() {
        super.tearDown()
        testScriptConfigurationNotification = false
        occurredLoadings = 0
        currentLoadingScriptConfigurationCallback = null
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

    protected fun assertAppliedConfiguration(contents: String) {
        val secondConfiguration = scriptConfigurationManager.getConfiguration(ktFile)!!
        assertEquals(
            contents,
            secondConfiguration.defaultImports.single().let {
                check(it.startsWith("x_"))
                it.removePrefix("x_")
            }
        )
    }

    protected fun makeChanges(contents: String) {
        changeContents(contents)

        scriptConfigurationManager.updater.ensureUpToDatedConfigurationSuggested(ktFile)
    }

    protected fun changeContents(contents: String) {
        runWriteAction {
            val fileDocumentManager = FileDocumentManager.getInstance()
            fileDocumentManager.reloadFiles(virtualFile)
            val document = fileDocumentManager.getDocument(virtualFile)!!
            document.setText(contents)
            fileDocumentManager.saveDocument(document)
            psiManager.reloadFromDisk(myFile)
            myFile = psiManager.findFile(virtualFile)
        }
    }

    protected fun assertReports(expected: String) {
        val actual = IdeScriptReportSink.getReports(virtualFile).single().message
        assertEquals(expected, actual)
    }

    protected fun assertSuggestedConfiguration() {
        assertTrue(virtualFile.hasSuggestedScriptConfiguration(project))
    }

    protected fun assertAndApplySuggestedConfiguration() {
        assertTrue(virtualFile.applySuggestedScriptConfiguration(project))
    }

    protected fun assertNoSuggestedConfiguration() {
        assertFalse(virtualFile.applySuggestedScriptConfiguration(project))
    }

    protected fun assertNoLoading() {
        assertEquals(0, occurredLoadings)
        occurredLoadings = 0
    }

    protected fun assertSingleLoading() {
        assertEquals(1, occurredLoadings)
        occurredLoadings = 0
    }

    protected fun assertAndLoadInitialConfiguration() {
        assertNull(scriptConfigurationManager.getConfiguration(ktFile))
        assertAndDoAllBackgroundTasks()
        assertSingleLoading()
        assertAppliedConfiguration("initial")
    }
}