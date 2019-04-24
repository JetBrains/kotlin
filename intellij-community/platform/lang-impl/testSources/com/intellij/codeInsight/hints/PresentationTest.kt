// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import junit.framework.TestCase
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JPanel


class PresentationTest : TestCase() {

  fun testSequenceDimension() {
    val presentation = SequencePresentation(listOf(SpacePresentation(10, 8), SpacePresentation(30, 5)))
    assertEquals(40, presentation.width)
    assertEquals(8, presentation.height)
  }

  fun testSequenceMouseClick() {
    val left = ClickCheckingPresentation(SpacePresentation(50, 10), 40 to 5)
    val presentation = SequencePresentation(listOf(left, SpacePresentation(30, 10)))
    click(presentation, 40, 5)
  }

  fun testSequenceMouseClick2() {
    val left = SpacePresentation(50, 10)
    val right = ClickCheckingPresentation(SpacePresentation(30, 10), 10 to 5)
    val presentation = SequencePresentation(listOf(left, right))
    click(presentation, 60, 5)
  }

  fun testSequenceMouseClickOutside() {
    val left = ClickCheckingPresentation(SpacePresentation(50, 10), null)
    val right = ClickCheckingPresentation(SpacePresentation(30, 10), null)
    val presentation = SequencePresentation(listOf(left, right))
    click(presentation, 100, 5)
  }

  fun testDynamicSize() {
    val first = ClickCheckingPresentation(SpacePresentation(1, 2), null)
    val second = ClickCheckingPresentation(SpacePresentation(3, 4), null)
    val presentation = DynamicDelegatePresentation(first)
    assertEquals(presentation.width, 1)
    assertEquals(presentation.height, 2)
    presentation.delegate = second
    assertEquals(presentation.width, 3)
    assertEquals(presentation.height, 4)
  }

  fun testDynamicClicks() {
    val first = ClickCheckingPresentation(SpacePresentation(5, 8), 1 to 1)
    val second = ClickCheckingPresentation(SpacePresentation(10, 15), null)
    val presentation = DynamicDelegatePresentation(first)
    click(presentation, 1, 1)
    presentation.delegate = second
    second.expectedClick = 2 to 2
    first.expectedClick = null
    click(presentation, 2, 2)
  }

  fun testSeqInInset() {
    val presentation = InsetPresentation(
      SequencePresentation(listOf(
        ClickCheckingPresentation(SpacePresentation(20, 10), 19 to 5),
        SpacePresentation(30, 10)
      )),
      left = 5
    )
    click(presentation, 24, 5)
  }

  fun testSeqInInset2() {
    val presentation = InsetPresentation(
      SequencePresentation(listOf(
        ClickCheckingPresentation(SpacePresentation(20, 10), 1 to 5),
        SpacePresentation(30, 10)
      )),
      left = 5
    )
    click(presentation, 1, 5)
  }

  fun testFoldedStateIsNotUpdatedAndStatelessComponentIsUpdated() {
    data class State(val data: String)
    class Presentation(val inner: InlayPresentation, data: String) : StatefulPresentation<State>(State(data), StateMark("TestMark")) {
      override fun getPresentation(): InlayPresentation = inner
      override fun toString(): String = ""
    }

    val old = Presentation(Presentation(SpacePresentation(10, 20), "inner"), "outer")
    val new = Presentation(Presentation(SpacePresentation(0, 1), "innerNew"), "outerNew")
    new.updateState(old)
    assertEquals("outer", new.state.data)
    val newInner = new.inner as Presentation
    assertEquals("inner", newInner.state.data)
    val (width, height) = newInner.inner as SpacePresentation
    assertEquals(0, width)
    assertEquals(1, height)
  }

  private class ClickCheckingPresentation(
    val presentation: InlayPresentation,
    var expectedClick: Pair<Int, Int>?
  ):  InlayPresentation by presentation {


    override fun mouseClicked(e: MouseEvent, editorPoint: Point) {
      val expectedClickVal = expectedClick
      if (expectedClickVal == null) {
        fail("No clicks expected")
        return
      }
      assertEquals(expectedClickVal.first, e.x)
      assertEquals(expectedClickVal.second, e.y)
      super.mouseClicked(e, editorPoint)
    }
  }
}

class HeavyPresentationTest : LightPlatformCodeInsightFixtureTestCase() {
  fun testFoldedStateIsNotUpdatedAndStatelessComponentIsUpdated() {
    myFixture.configureByText("__Dummy__.java", "class A {}")
    val factory = PresentationFactory(myFixture.editor as EditorImpl)

    val old = unwrapFolding(factory.folding(factory.text("outerPlaceholder"), object : PresentationSupplier {
      override fun getPresentation(): InlayPresentation =
        unwrapFolding(factory.folding(factory.text("innerPlaceholder"), object : PresentationSupplier {
          override fun getPresentation() = factory.text("text")
        }))
    }))

    val new = factory.folding(factory.text("outerPlaceholderNew"), object : PresentationSupplier {
      override fun getPresentation(): InlayPresentation = factory.folding(
        factory.text("innerPlaceholderNew"), object : PresentationSupplier {
        override fun getPresentation(): InlayPresentation = factory.text("newText")
      })
    })
    new.updateState(old)
    assertEquals("<clicked><clicked>newText", new.toString())
  }

  private fun unwrapFolding(presentation: InlayPresentation): InlayPresentation {
    presentation as ChangeOnClickPresentation
    presentation.state = ChangeOnClickPresentation.State(true)
    return presentation
  }
}

private fun click(presentation: InlayPresentation, x: Int, y: Int) {
  val event = MouseEvent(JPanel(), 0, 0, 0, x, y, 0, true, 0)
  presentation.mouseClicked(event, Point(x, y))
}