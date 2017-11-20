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

package org.jetbrains.kotlin.idea.spring.tests.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.spring.SpringManager
import com.intellij.spring.model.utils.SpringModelSearchers
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.spring.lineMarking.KotlinSpringClassAnnotator
import org.jetbrains.kotlin.idea.spring.tests.SpringKtLightHighlightingTestCase
import org.jetbrains.kotlin.utils.SmartList

class SpringKtClassAnnotatorPerformanceTest : SpringKtLightHighlightingTestCase() {

    private val beansCount = 150

    private val expectedGutters = beansCount * 2 + 4

    private fun createFileAndAnnotateGutter(fileName: String, text: String, modificationText: (Long) -> String, expectedMs: Int) {
        myFixture.addFileToProject(fileName, text)
        myFixture.configureByFile(fileName)
        configureFileSet(fileName)

        //preload caches and indexes
        SpringManager.getInstance(project).getAllModels(module).map { SpringModelSearchers.findBean(it, "pkg.LocalBean1") }

        PlatformTestUtil.startPerformanceTest("Get gutters for $fileName", expectedMs) {
            val allElements = PsiTreeUtil.collectElements(myFixture.file, { true }).toList()
            val allGutters = SmartList<LineMarkerInfo<*>>()
            KotlinSpringClassAnnotator().collectSlowLineMarkers(allElements, allGutters)
            TestCase.assertEquals(expectedGutters, allGutters.size)
        }.setup {
            val modCount = myFixture.psiManager.modificationTracker.outOfCodeBlockModificationCount
            WriteCommandAction.runWriteCommandAction(project) {
                val document = myFixture.editor.document
                document.insertString(myFixture.caretOffset, modificationText(modCount))
            }
            FileDocumentManager.getInstance().saveAllDocuments()
            myFixture.configureByFile(fileName)
        }.attempts(5).assertTiming()

    }


    fun testAnnotateKtWithInCodeBlockChanges() {
        createFileAndAnnotateGutter("Config.kt", """
                package pkg;

                @org.springframework.context.annotation.Configuration
                @org.springframework.context.annotation.ComponentScan
                open class Config {

                fun foo(){
                <caret>
                }

                ${(0..beansCount).joinToString("\n") {
            """
                @org.springframework.context.annotation.Bean
                open fun localBean$it() = LocalBean$it()
                """.trimIndent()
        }}
                }

                ${(0..beansCount).joinToString("\n") {
            "class LocalBean$it"
        }}

                """, { "System.out.println($it);\n" }, 100)
    }

    fun testAnnotateKtWithOutCodeBlockChanges() {
        createFileAndAnnotateGutter("Config.kt", """
                package pkg;

                @org.springframework.context.annotation.Configuration
                @org.springframework.context.annotation.ComponentScan
                open class Config {

                <caret>

               ${(0..beansCount).joinToString("\n") {
            """
                    @org.springframework.context.annotation.Bean
                    open fun localBean$it() = LocalBean$it()
                    """.trimIndent()
        }}
                }

                ${(0..beansCount).joinToString("\n") {
            "class LocalBean$it"
        }}

                """, { "fun foo$it(){}\n" }, 2500)
    }


}
