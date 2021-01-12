// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.RecursivelyUpdatingRootPresentation
import com.intellij.codeInsight.hints.presentation.RootInlayPresentation
import com.intellij.codeInsight.hints.presentation.SpacePresentation
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class InlayPassTest : BasePlatformTestCase() {
  private val noSettings = SettingsKey<NoSettings>("no")

  override fun setUp() {
    super.setUp()
    myFixture.configureByText("file.java", "class A{ }")
  }

  fun testTurnedOffHintsDisappear() {
    createPass(listOf(createOneOffCollector {
      it.addBlockElement(0, true, TestRootPresentation(0), null)
      it.addInlineElement(5, TestRootPresentation(1), null)
    })).collectAndApply()
    assertEquals(2, allHintsCount)
    createPass(emptyList()).collectAndApply()
    assertEquals(0, allHintsCount)
  }

  fun testNonProviderMangedInlayStayUntouched() {
    val presentation = HorizontalConstrainedPresentation(TestRootPresentation(), null)
    @Suppress("UNCHECKED_CAST")
    inlayModel.addInlineElement(3, InlineInlayRenderer(listOf(presentation as HorizontalConstrainedPresentation<in Any>)))
    createPass(emptyList()).collectAndApply()
    assertEquals(1, inlineElements.size)
    val renderer = inlineElements.first().renderer as InlineInlayRenderer
    assertEquals(presentation, renderer.getConstrainedPresentations().first())
  }

  fun testInlaysFromMultipleCollectorsMerged() {
    createPass(listOf(
      createOneOffCollector {
        it.addInlineElement(1, TestRootPresentation(1), null)
      },
      createOneOffCollector {
        it.addInlineElement(2, TestRootPresentation(2), null)
      }
    )).collectAndApply()
    val inlays = inlineElements
    assertEquals(2, inlays.size)
    assertEquals(1, extractContent(inlays, 0))
    assertEquals(2, extractContent(inlays, 1))
  }

  fun testPresentationUpdated() {
    createPass(listOf(
      createOneOffCollector {
        it.addInlineElement(1, TestRootPresentation(1), null)
      }
    )).collectAndApply()
    assertEquals(1, inlineElements.size)
    assertEquals(1, extractContent(inlineElements, 0))

    createPass(listOf(
      createOneOffCollector {
        it.addInlineElement(1, TestRootPresentation(3), null)
      }
    )).collectAndApply()
    assertEquals(1, inlineElements.size)
    assertEquals(3, extractContent(inlineElements, 0))
  }

  fun testNoPresentationRecreatedWhenNothingChanges() {
    val initilalRoot = RecursivelyUpdatingRootPresentation(SpacePresentation(10, 10))
    applyPassWithCollectorOfSingleElement(initilalRoot)
    applyPassWithCollectorOfSingleElement(RecursivelyUpdatingRootPresentation(SpacePresentation(10, 10)))
    val root = expectSingleHorizontalPresentation()
    assertSame(initilalRoot, root)
  }

  fun testPresentationIsTheSameButContentUpdated() {
    val initialContent = SpacePresentation(10, 10)
    val initilalRoot = RecursivelyUpdatingRootPresentation(initialContent)
    applyPassWithCollectorOfSingleElement(initilalRoot)
    applyPassWithCollectorOfSingleElement(RecursivelyUpdatingRootPresentation(SpacePresentation(5, 5)))
    val root = expectSingleHorizontalPresentation()
    assertSame(initilalRoot, root)
    assertNotSame(initialContent, root.content)
  }

  fun testUpdateWithNonRecursiveRootPresentation() {
    applyPassWithCollectorOfSingleElement(RecursivelyUpdatingRootPresentation(SpacePresentation(10, 10)))
    val rootAfter = TestRootPresentation(1)
    applyPassWithCollectorOfSingleElement(rootAfter)
    val presentation = expectSingleHorizontalPresentation()
    assertSame(rootAfter, presentation)
  }

  private fun expectSingleHorizontalPresentation(): RootInlayPresentation<out Any> {
    assertEquals(1, inlineElements.size)
    val inlay = inlineElements.first()
    val renderer = inlay.renderer as InlineInlayRenderer
    val presentations = renderer.getConstrainedPresentations()
    assertEquals(1, presentations.size)
    val constrainedPresentation = presentations.first()
    return constrainedPresentation.root
  }


  private fun <T : Any> applyPassWithCollectorOfSingleElement(presentation: RootInlayPresentation<T>) {
    createPass(listOf(
      createOneOffCollector {
        it.addInlineElement(1, presentation, null)
      }
    )).collectAndApply()
  }

  private fun extractContent(inlays: List<Inlay<*>>, index: Int): Int {
    val renderer = inlays[index].renderer as InlineInlayRenderer
    return renderer.getConstrainedPresentations().first().root.content as Int
  }

  private fun createOneOffCollector(collector: (InlayHintsSink) -> Unit): CollectorWithSettings<NoSettings> {
    var firstTime = true
    val collectorLambda: (PsiElement, Editor, InlayHintsSink) -> Boolean = { _, _, sink ->
      if (firstTime) {
        collector(sink)
        firstTime = false
      }
      false
    }
    return createCollector(collectorLambda)
  }

  private fun createCollector(collector: (PsiElement, Editor, InlayHintsSink) -> Boolean): CollectorWithSettings<NoSettings> {
    return CollectorWithSettings(object : InlayHintsCollector {
      override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        return collector(element, editor, sink)
      }
    }, noSettings, Language.ANY, InlayHintsSinkImpl(myFixture.editor))
  }

  private fun createPass(collectors: List<CollectorWithSettings<*>>): InlayHintsPass {
    return InlayHintsPass(myFixture.file, collectors, myFixture.editor)
  }

  private fun InlayHintsPass.collectAndApply() {
    val dumbProgressIndicator = DumbProgressIndicator()
    doCollectInformation(dumbProgressIndicator)
    applyInformationToEditor()
  }

  private val inlayModel
    get() = myFixture.editor.inlayModel

  private val blockElements: List<Inlay<*>>
    get() = inlayModel.getBlockElementsInRange(0, myFixture.file.textRange.endOffset)

  private val inlineElements: List<Inlay<*>>
    get() = inlayModel.getInlineElementsInRange(0, myFixture.file.textRange.endOffset)

  private val allHintsCount: Int
    get() = inlineElements.size + blockElements.size
}