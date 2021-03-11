/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.highlighter.KotlinChangeLocalityDetector
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils

private const val SCOPE_DIRECTIVE = "// SCOPE:"

abstract class AbstractChangeLocalityDetectorTest : KotlinLightCodeInsightFixtureTestCase() {

    protected fun doTest(unused: String?) {
        val ktFile = myFixture.configureByFile(fileName()) as KtFile

        val expectedScopeLines = InTextDirectivesUtils.findListWithPrefixes(ktFile.text, SCOPE_DIRECTIVE)
        assertTrue("scope has to be specified with $SCOPE_DIRECTIVE", expectedScopeLines.isNotEmpty())
        val selectionModel = editor.selectionModel
        assertTrue("changed item has to be specified in <selection></selection", selectionModel.hasSelection())

        val element = PsiTreeUtil.findElementOfClassAtRange(
            ktFile,
            selectionModel.selectionStart,
            selectionModel.selectionEnd,
            PsiElement::class.java
        )
            ?: error("No PsiElement at selection range")
        val changeLocalityDetector = KotlinChangeLocalityDetector()

        val dirtyScope = changeLocalityDetector.getChangeHighlightingDirtyScopeFor(element)
            ?: error("scope has to be calculated")
        println("dirtyScopeFor ${element.text} is ${dirtyScope?.text}")
        assertEquals(expectedScopeLines.joinToString("\n"), dirtyScope.text)
    }
}