/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.repl

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.jetbrains.kotlin.idea.completion.test.KotlinFixtureCompletionBaseTestCase
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractIdeReplCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    private var consoleRunner: KotlinConsoleRunner? = null

    override fun setUp() {
        super.setUp()
        consoleRunner = KotlinConsoleKeeper.getInstance(project).run(myModule)!!
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())
        consoleRunner?.dispose()
        consoleRunner = null
        super.tearDown()
    }

    override fun getPlatform() = DefaultBuiltInPlatforms.jvmPlatform
    override fun defaultCompletionType() = CompletionType.BASIC

    override fun doTest(testPath: String) {
        val runner = consoleRunner!!
        val file = File(testPath)
        val lines = file.readLines()
        lines.prefixedWith(">> ").forEach { runner.successfulLine(it) } // not actually executing anything, only simulating
        val codeSample = lines.prefixedWith("-- ").joinToString("\n")

        runWriteAction {
            val editor = runner.consoleView.editorDocument
            editor.setText(codeSample)
            FileDocumentManager.getInstance().saveDocument(runner.consoleView.editorDocument)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        myFixture.configureFromExistingVirtualFile(runner.consoleFile.virtualFile)
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.getLineEndOffset(0))

        testCompletion(file.readText(), getPlatform(), { completionType, count -> myFixture.complete(completionType, count) })
    }

    private fun List<String>.prefixedWith(prefix: String) = filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }

    override fun getProjectDescriptor(): LightProjectDescriptor = FullJdkProjectDescriptor
}

private object FullJdkProjectDescriptor : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
    override fun getSdk() = PluginTestCaseBase.fullJdk()
}