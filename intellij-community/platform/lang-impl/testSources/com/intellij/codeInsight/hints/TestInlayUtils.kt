// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.codeInsight.hints.presentation.RootInlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import org.junit.Assert.*
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

internal data class TestRootPresentation(override var content: Int = 0) : BasePresentation(), RootInlayPresentation<Int>  {
  override val key: ContentKey<Int>
    get() = InlayKey<Any, Int>("test.root.presentation")
  override val width: Int
    get() = 2
  override val height: Int
    get() = 3

  override fun update(newPresentationContent: Int, editor: Editor, factory: InlayPresentationFactory): Boolean {
    val changed = content != newPresentationContent
    this.content = newPresentationContent
    return changed
  }

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
  }
}

internal class ClickCheckingPresentation(
  val presentation: InlayPresentation,
  var expectedClick: Pair<Int, Int>?
): InlayPresentation by presentation {
  var wasClicked = false

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    val expectedClickVal = expectedClick
    if (expectedClickVal == null) {
      fail("No clicks expected")
      return
    }
    assertEquals(expectedClickVal.first, translated.x)
    assertEquals(expectedClickVal.second, translated.y)
    wasClicked = true
    super.mouseClicked(event, translated)
  }

  fun assertWasClicked() {
    assertTrue(wasClicked)
  }
}

internal class ChangeCountingListener : PresentationListener {
  var contentChangesCount = 0
  val contentChanged: Boolean
    get() = contentChangesCount != 0

  override fun contentChanged(area: Rectangle) {
    contentChangesCount++
  }

  override fun sizeChanged(previous: Dimension, current: Dimension) {
  }
}