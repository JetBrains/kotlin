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

package org.jetbrains.uast.test.common

import com.intellij.psi.PsiNamedElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.test.env.findElementByText
import org.junit.Assert.assertEquals

interface ResolveTestBase {
    fun check(testName: String, file: UFile) {
        val refComment = file.allCommentsInFile.find { it.text.startsWith("// REF:") } ?: throw IllegalArgumentException("No // REF tag in file")
        val resultComment = file.allCommentsInFile.find { it.text.startsWith("// RESULT:") } ?: throw IllegalArgumentException("No // RESULT tag in file")

        val refText = refComment.text.substringAfter("REF:")
        val parent = refComment.uastParent
        val matchingElement = parent.findElementByText<UResolvable>(refText)
        val resolveResult = matchingElement.resolve() ?: throw IllegalArgumentException("Unresolved reference")
        val resultText = resolveResult.javaClass.simpleName + (if (resolveResult is PsiNamedElement) ":${resolveResult.name}" else "")
        assertEquals(resultComment.text.substringAfter("RESULT:"), resultText)
    }
}
