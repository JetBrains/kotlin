/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.checkers.AbstractPsiCheckerTest
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.completion.test.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.completion.test.ExpectedCompletionUtils
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionHandlerTest
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class AbstractCodeFragmentHighlightingTest : AbstractPsiCheckerTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        myFixture.checkHighlighting(true, false, false)
    }

    fun doTestWithImport(filePath: String) {
        myFixture.configureByCodeFragment(filePath)

        project.executeWriteCommand("Imports insertion") {
            val fileText = FileUtil.loadFile(File(filePath), true)
            val file = myFixture.file as KtFile
            InTextDirectivesUtils.findListWithPrefixes(fileText, "// IMPORT: ").forEach {
                val descriptor = file.resolveImportReference(FqName(it)).singleOrNull()
                                 ?: error("Could not resolve descriptor to import: $it")
                ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
            }
        }

        myFixture.checkHighlighting(true, false, false)
    }
}

abstract class AbstractCodeFragmentCompletionTest : AbstractJvmBasicCompletionTest() {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath)
    }
}

abstract class AbstractCodeFragmentCompletionHandlerTest : AbstractCompletionHandlerTest(CompletionType.BASIC) {
    override fun setUpFixture(testPath: String) {
        myFixture.configureByCodeFragment(testPath)
    }

    override fun doTest(testPath: String) {
        super.doTest(testPath)

        val fragment = myFixture.file as KtCodeFragment
        fragment.checkImports(testPath)
    }
}

abstract class AbstractCodeFragmentAutoImportTest : AbstractPsiCheckerTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        myFixture.doHighlighting()

        val importFix = myFixture.availableIntentions.singleOrNull { it.familyName == "Import" }
                        ?: error("No import fix available")
        importFix.invoke(project, editor, file)

        myFixture.checkResultByFile(filePath + ".after")

        val fragment = myFixture.file as KtCodeFragment
        fragment.checkImports(testDataPath + File.separator + filePath)

        val fixAfter = myFixture.availableIntentions.firstOrNull { it.familyName == "Import" }
        assertNull(fixAfter, "No import fix should be available after")
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()
}

private fun KtCodeFragment.checkImports(testPath: String) {
    val importList = importsAsImportList()
    val importsText = StringUtil.convertLineSeparators(importList?.text ?: "")
    val fragmentAfterFile = File(testPath + ".after.imports")

    if (fragmentAfterFile.exists()) {
        KotlinTestUtils.assertEqualsToFile(fragmentAfterFile, importsText)
    }
    else {
        assertTrue(importsText.isEmpty(), "Unexpected imports found: $importsText" )
    }
}

private fun JavaCodeInsightTestFixture.configureByCodeFragment(filePath: String) {
    configureByFile(filePath)

    val elementAt = file?.findElementAt(caretOffset)
    val file = createCodeFragment(filePath, elementAt!!)

    val typeStr = InTextDirectivesUtils.findStringWithPrefixes(getFile().text, "// ${ExpectedCompletionUtils.RUNTIME_TYPE} ")
    if (typeStr != null) {
        file.putCopyableUserData(KtCodeFragment.RUNTIME_TYPE_EVALUATOR, {
            val codeFragment = KtPsiFactory(project).createBlockCodeFragment("val xxx: $typeStr", PsiTreeUtil.getParentOfType(elementAt, KtElement::class.java))
            val context = codeFragment.analyzeWithContent()
            val typeReference: KtTypeReference = PsiTreeUtil.getChildOfType(codeFragment.getContentElement().firstChild, KtTypeReference::class.java)!!
            context[BindingContext.TYPE, typeReference]
        })
    }

    configureFromExistingVirtualFile(file.virtualFile!!)
}

private fun createCodeFragment(filePath: String, contextElement: PsiElement): KtCodeFragment {
    val fileForFragment = File(filePath + ".fragment")
    val codeFragmentText = FileUtil.loadFile(fileForFragment, true).trim()
    val psiFactory = KtPsiFactory(contextElement.project)
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
