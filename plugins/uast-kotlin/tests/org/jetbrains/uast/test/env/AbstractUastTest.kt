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

package org.jetbrains.uast.test.env

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File
import kotlin.test.fail

abstract class AbstractUastTest : AbstractTestWithCoreEnvironment() {
    protected companion object {
        val TEST_DATA_DIR = File("testData")
    }

    abstract fun getVirtualFile(testName: String): VirtualFile
    abstract fun check(testName: String, file: UFile)

    fun doTest(testName: String, checkCallback: (String, UFile) -> Unit = { testName, file -> check(testName, file) }) {
        val virtualFile = getVirtualFile(testName)

        val psiFile = psiManager.findFile(virtualFile) ?: error("Can't get psi file for $testName")
        val uFile = uastContext.convertElementWithParent(psiFile, null) ?: error("Can't get UFile for $testName")
        checkCallback(testName, uFile as UFile)
    }
}

fun <T> UElement.findElementByText(refText: String, cls: Class<T>): T {
    val matchingElements = mutableListOf<T>()
    accept(object : UastVisitor {
        override fun visitElement(node: UElement): Boolean {
            if (cls.isInstance(node) && node.psi?.text == refText) {
                matchingElements.add(node as T)
            }
            return false
        }
    })

    if (matchingElements.isEmpty()) {
        throw IllegalArgumentException("Reference '$refText' not found")
    }
    if (matchingElements.size != 1) {
        throw IllegalArgumentException("Reference '$refText' is ambiguous")
    }
    return matchingElements.single()
}

inline fun <reified T : Any> UElement.findElementByText(refText: String): T = findElementByText(refText, T::class.java)

inline fun <reified T : UElement> UElement.findElementByTextFromPsi(refText: String): T = (this.psi ?: fail("no psi for $this")).findUElementByTextFromPsi(refText)

inline fun <reified T : UElement> PsiElement.findUElementByTextFromPsi(refText: String): T {
    val elementAtStart = this.findElementAt(this.text.indexOf(refText))
                         ?: throw AssertionError("requested text '$refText' was not found in $this")
    val uElementContainingText = elementAtStart.parentsWithSelf.
            dropWhile { !it.text.contains(refText) }.
            mapNotNull { it.toUElementOfType<T>() }.firstOrNull()
            ?: throw AssertionError("requested text '$refText' not found as '${T::class.java.canonicalName}' in $this")
    if (uElementContainingText.psi != null && uElementContainingText.psi?.text != refText) {
        throw AssertionError("requested text '$refText' found as '${uElementContainingText.psi?.text}' in $uElementContainingText")
    }
    return uElementContainingText;
}
