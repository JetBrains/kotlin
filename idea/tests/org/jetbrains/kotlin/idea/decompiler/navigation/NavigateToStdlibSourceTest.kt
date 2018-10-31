/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.ModuleKind
import org.jetbrains.kotlin.idea.test.closeAndDeleteProject
import org.jetbrains.kotlin.idea.test.configureAs

class NavigateToStdlibSourceTest : KotlinCodeInsightTestCase() {

    private val FILE_TEXT = "fun foo() { <caret>println() }"

    fun testRefToPrintlnWithJVM() {
        doTest("ioH.kt", ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES)
    }

    fun testRefToPrintlnWithJVMAndJS() {
        doTest("ioH.kt", ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES, ModuleKind.KOTLIN_JAVASCRIPT)
    }

    fun testRefToPrintlnWithJS() {
        doTest("console.kt", ModuleKind.KOTLIN_JAVASCRIPT)
    }

    fun testRefToPrintlnWithJSAndJVM() {
        doTest("console.kt", ModuleKind.KOTLIN_JAVASCRIPT, ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES)
    }

    private fun doTest(sourceFileName: String, mainModule: ModuleKind, additionalModule: ModuleKind? = null) {
        val navigationElement = configureAndResolve(FILE_TEXT, mainModule, additionalModule)
        TestCase.assertEquals(sourceFileName, navigationElement.containingFile.name)
    }

    override fun tearDown() {
        super.tearDown()
        // Workaround for IDEA's bug during tests.
        // After tests IDEA disposes VirtualFiles within LocalFileSystem, but doesn't rebuild indices.
        // This causes library source files to be impossible to find via indices
        closeAndDeleteProject()
    }

    protected fun configureAndResolve(
            text: String,
            mainModuleKind: ModuleKind,
            additionalModuleKind: ModuleKind? = null
    ): PsiElement {
        module.configureAs(mainModuleKind)
        if (additionalModuleKind != null) {
            val additionalModule = this.createModule("additional-module")
            additionalModule.configureAs(additionalModuleKind)
        }

        configureByText(KotlinFileType.INSTANCE, text)

        val ref = file.findReferenceAt(editor.caretModel.offset)
        val resolve = ref!!.resolve()
        return resolve!!.navigationElement
    }
}