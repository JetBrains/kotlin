/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.repl

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.jetbrains.kotlin.idea.completion.test.KotlinFixtureCompletionBaseTestCase
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.io.File
import kotlin.properties.Delegates

abstract class AbstractIdeReplCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    private var consoleRunner: KotlinConsoleRunner by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        consoleRunner = KotlinConsoleKeeper.getInstance(project).run(myModule)!!
    }

    override fun tearDown() {
        consoleRunner.dispose()
        super.tearDown()
    }

    override fun getPlatform() = JvmPlatform
    override fun defaultCompletionType() = CompletionType.BASIC

    override fun doTest(testPath: String) {
        val file = File(testPath)
        val lines = file.readLines()
        lines.prefixedWith(">> ").forEach { consoleRunner.successfulLine(it) } // not actually executing anything, only simulating
        val codeSample = lines.prefixedWith("-- ").joinToString("\n")

        runWriteAction {
            val editor = consoleRunner.consoleView.editorDocument
            editor.setText(codeSample)
            FileDocumentManager.getInstance().saveDocument(consoleRunner.consoleView.editorDocument)
            PsiDocumentManager.getInstance(getProject()).commitAllDocuments()
        }

        myFixture.configureFromExistingVirtualFile(consoleRunner.consoleFile.virtualFile)
        myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.getLineEndOffset(0))

        testCompletion(file.readText(), getPlatform(), { completionType, count -> myFixture.complete(completionType, count) })
    }

    private fun List<String>.prefixedWith(prefix: String) = filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }

    override fun getProjectDescriptor(): LightProjectDescriptor = FullJdkProjectDescriptor
}

private object FullJdkProjectDescriptor : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
    override fun getSdk() = PluginTestCaseBase.fullJdk()
}