/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayParameterHintsExtension
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.kotlin.idea.codeInsight.hints.HintType
import org.junit.Assert

internal fun JavaCodeInsightTestFixture.checkHintType(text: String, hintType: HintType) {
    configureByText("A.kt", text.trimIndent())
    doHighlighting()

    checkHintType(hintType)
}

internal fun JavaCodeInsightTestFixture.checkHintType(hintType: HintType) {
    val hintInfo = getHintInfoFromProvider(caretOffset, file, editor)
    Assert.assertNotNull("No hint available at caret", hintInfo)
    Assert.assertEquals(hintType.option.name, (hintInfo as HintInfo.OptionInfo).optionName)
}

// It's crucial for this method to be conformable with IDEA internals.
// Originally copied from com.intellij.codeInsight.hints.getHintInfoFromProvider()
private fun getHintInfoFromProvider(offset: Int, file: PsiFile, editor: Editor): HintInfo? {
    val element = file.findElementAt(offset) ?: return null
    val provider = InlayParameterHintsExtension.forLanguage(file.language) ?: return null

    val isHintOwnedByElement: (PsiElement) -> Boolean = { e -> provider.getHintInfo(e)?.isOwnedByPsiElement(e, editor) ?: false }
    val method = PsiTreeUtil.findFirstParent(element, isHintOwnedByElement) ?: return null

    return provider.getHintInfo(method)
}
