// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.SpacePresentation
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase

class InlayPassTest : LightPlatformCodeInsightFixtureTestCase() {
  private val noSettings = SettingsKey<NoSettings>("no")

  fun testBlockAndInlineElementMayBeAtSameOffset() {
    myFixture.configureByText("file.java", "class A{ }")
    val sink = InlayHintsSinkImpl(noSettings)
    sink.addBlockElement(5, true, true, 0, SpacePresentation(0, 0))
    sink.addInlineElement(5, true, SpacePresentation(10, 10))
    val editor = myFixture.editor
    sink.applyToEditor(editor, emptyList(), emptyList(), true)
    val file = myFixture.file
    assertEquals(1, inlineElements.size)
    assertEquals(1, blockElements.size)
    assertEquals(5, inlineElements.first().offset)
    assertEquals(5, blockElements.first().offset)
  }

  fun testTurnedOffHintsDisappear() {
    myFixture.configureByText("file.java", "class A{ }")
    val sink = InlayHintsSinkImpl(noSettings)
    sink.addBlockElement(5, true, true, 0, SpacePresentation(0, 0))
    sink.addInlineElement(5, true, SpacePresentation(10, 10))
    val editor = myFixture.editor
    sink.applyToEditor(editor, inlineElements, blockElements, true)
    sink.applyToEditor(editor, inlineElements, blockElements, false)
    assertEquals(0, inlineElements.size)
    assertEquals(0, blockElements.size)
  }

  private val blockElements: MutableList<Inlay<EditorCustomElementRenderer>>
    get() = myFixture.editor.inlayModel.getBlockElementsInRange(0, myFixture.file.textRange.endOffset)

  private val inlineElements: MutableList<Inlay<EditorCustomElementRenderer>>
    get() = myFixture.editor.inlayModel.getInlineElementsInRange(0, myFixture.file.textRange.endOffset)
}