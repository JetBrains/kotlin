// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.SpacePresentation
import com.intellij.lang.Language
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class InlayPassTest : BasePlatformTestCase() {
  private val noSettings = SettingsKey<NoSettings>("no")

  fun testBlockAndInlineElementMayBeAtSameOffset() {
    myFixture.configureByText("file.java", "class A{ }")
    val sink = InlayHintsSinkImpl(noSettings)
    sink.addBlockElement(5, true, true, 0, SpacePresentation(0, 0))
    sink.addInlineElement(5, true, SpacePresentation(10, 10))
    val editor = myFixture.editor
    sink.applyToEditor(editor, MarkList(emptyList()), MarkList(emptyList()), true)
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

  fun testPassRemovesNonRelatedInlays() {
    myFixture.configureByText("file.java", "class A{ }")
    var first = true
    val collector = CollectorWithSettings(
      object : InlayHintsCollector {
        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
          if (first) {
            first = false
            sink.addInlineElement(0, false, SpacePresentation(1, 1))
          }
          return true
        }
      }, noSettings, Language.ANY, InlayHintsSinkImpl(noSettings)
    )
    collectThenApply(listOf(collector))
    assertEquals(1, inlineElements.size)
    collectThenApply(emptyList())
    assertEquals(0, inlineElements.size)
  }

  private fun collectThenApply(collectors: List<CollectorWithSettings<NoSettings>>) {
    val pass = createPass(collectors)
    pass.doCollectInformation(DumbProgressIndicator())
    pass.doApplyInformationToEditor()
  }

  private fun createPass(collectors: List<CollectorWithSettings<NoSettings>>): InlayHintsPass {
    return InlayHintsPass(myFixture.file, collectors, myFixture.editor, InlayHintsSettings.instance())
  }

  private val blockElements: MarkList<Inlay<*>>
    get() = MarkList(myFixture.editor.inlayModel.getBlockElementsInRange(0, myFixture.file.textRange.endOffset))

  private val inlineElements: MarkList<Inlay<*>>
    get() = MarkList(myFixture.editor.inlayModel.getInlineElementsInRange(0, myFixture.file.textRange.endOffset))
}