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

package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.checkers.AbstractJetPsiCheckerTest
import org.jetbrains.kotlin.completion.AbstractJvmBasicCompletionTest
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.idea.quickfix.ImportInsertHelper
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.psi.JetTypeReference
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.completion.ExpectedCompletionUtils
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.completion.handlers.AbstractCompletionHandlerTest
import com.intellij.codeInsight.completion.CompletionType

public abstract class AbstractCodeFragmentHighlightingTest : AbstractJetPsiCheckerTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        myFixture.checkHighlighting(true, false, false)
    }

    fun doTestWithImport(filePath: String) {
        myFixture.configureByCodeFragment(filePath)

        ApplicationManager.getApplication()?.runWriteAction {
            val fileText = FileUtil.loadFile(File(filePath), true)
            InTextDirectivesUtils.findListWithPrefixes(fileText, "// IMPORT: ").forEach {
                ImportInsertHelper.INSTANCE.addImportDirectiveIfNeeded(FqName(it), (myFixture.getFile() as JetFile))
            }
        }

        myFixture.checkHighlighting(true, false, false)
    }
}

public abstract class AbstractCodeFragmentCompletionTest : AbstractJvmBasicCompletionTest() {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath)
    }
}

public abstract class AbstractCodeFragmentCompletionHandlerTest : AbstractCompletionHandlerTest() {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath)
    }

    override val testDataRelativePath: String = "/completion/handlers/runtimeCast/"
    override val defaultCompletionType: CompletionType = CompletionType.BASIC
}

private fun JavaCodeInsightTestFixture.configureByCodeFragment(filePath: String) {
    configureByFile(filePath)

    val elementAt = getFile()?.findElementAt(getCaretOffset())
    val file = createCodeFragment(filePath, elementAt!!)

    val typeStr = InTextDirectivesUtils.findStringWithPrefixes(getFile().getText(), "// ${ExpectedCompletionUtils.RUNTIME_TYPE} ")
    if (typeStr != null) {
        file.putCopyableUserData(JetCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            val codeFragment = JetPsiFactory(getProject()).createBlockCodeFragment("val xxx: $typeStr" , PsiTreeUtil.getParentOfType(elementAt, javaClass<JetElement>()))
            val context = codeFragment.analyzeFully()
            val typeReference: JetTypeReference = PsiTreeUtil.getChildOfType(codeFragment.getContentElement().getFirstChild(), javaClass())
            context[BindingContext.TYPE, typeReference]
        })
    }

    configureFromExistingVirtualFile(file.getVirtualFile()!!)
}

private fun createCodeFragment(filePath: String, contextElement: PsiElement): JetCodeFragment {
    val fileForFragment = File(filePath + ".fragment")
    val codeFragmentText = FileUtil.loadFile(fileForFragment, true).trim()
    val psiFactory = JetPsiFactory(contextElement.getProject())
    if (fileForFragment.readLines().size == 1) {
        return psiFactory.createExpressionCodeFragment(
                codeFragmentText,
                KotlinCodeFragmentFactory.getContextElement(contextElement)
        )
    }
    return psiFactory.createBlockCodeFragment(
            codeFragmentText,
            KotlinCodeFragmentFactory.getContextElement(contextElement)
    )
}
