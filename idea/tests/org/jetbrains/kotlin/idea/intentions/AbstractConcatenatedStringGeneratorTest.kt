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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard.ConcatenatedStringGenerator
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File

/**
 * Compare xxx.kt.result file with the result of ConcatenatedStringGenerator().create(KtBinaryExpression) where KtBinaryExpression is the last KtBinaryExpression of xxx.kt file
 */
abstract class AbstractConcatenatedStringGeneratorTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    protected fun doTest(path: String) {
        val mainFile = File(path)
        val fileText = mainFile.readText()
        val resultFile = File("$path.result")
        val expectedText = resultFile.readText()

        val vFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: return
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val ktFile = KtPsiFactory(project).createAnalyzableFile("dummy.kt", fileText, psiFile)
        val expression = ktFile.collectDescendantsOfType<KtBinaryExpression>().lastOrNull()
        TestCase.assertNotNull("No binary expression found: $path", expression)
        val generatedString = ConcatenatedStringGenerator().create(expression!!)
        TestCase.assertEquals("mismatch '$expectedText' - '$generatedString'", expectedText, generatedString)
    }
}
