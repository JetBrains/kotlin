// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import junit.framework.TestCase
import java.awt.Rectangle

class InlineInlayRendererTest : LightPlatformCodeInsightTestCase() {
  override fun setUp() {
    super.setUp()
    configureFromFileText("file.java", "class A{ }")
  }

  fun testHorizontalPresentationPriorityIsHonoredWhenInsert() {
    val c1 = constrained(2)
    val renderer = InlineInlayRenderer(listOf(c1))
    val c2 = constrained(1)
    renderer.addOrUpdate(listOf(c1, c2), editor, factory())
    val arranged = renderer.getConstrainedPresentations()
    assertEquals(listOf(c2, c1), arranged)
  }

  fun testInitiallyMoreElements() {
    val c1 = constrained(3)
    val c2 = constrained(4)
    val c3 = constrained(5)
    val c4 = constrained(6)
    val renderer = InlineInlayRenderer(listOf(c1, c2, c3, c4))

    val c5 = constrained(2)
    val c6 = constrained(1)
    renderer.addOrUpdate(listOf(c5, c6), editor, factory())
    val arranged = renderer.getConstrainedPresentations()
    assertEquals(listOf(c6, c5), arranged)
  }


  fun testInitiallyLessElements() {
    val c1 = constrained(3)
    val c2 = constrained(4)

    val renderer = InlineInlayRenderer(listOf(c1, c2))

    val c3 = constrained(5)
    val c4 = constrained(6)
    val c5 = constrained(2)
    val c6 = constrained(1)
    renderer.addOrUpdate(listOf(c3, c4, c5, c6), editor, factory())
    val arranged = renderer.getConstrainedPresentations()
    assertEquals(listOf(c6, c5, c3, c4), arranged)
  }

  fun testNonSortedInitially() {
    val c1 = constrained(4)
    val c2 = constrained(2)
    val renderer = InlineInlayRenderer(listOf(c1, c2))
    val arranged = renderer.getConstrainedPresentations()
    TestCase.assertEquals(listOf(c2, c1), arranged)
  }

  fun testSamePriorityUpdate() {
    val c1 = HorizontalConstrainedPresentation(TestRootPresentation(1), horizontal(1))
    val renderer = InlineInlayRenderer(listOf(c1))

    val c2 = HorizontalConstrainedPresentation(TestRootPresentation(2), horizontal(1))
    renderer.addOrUpdate(listOf(c2), editor, factory())

    val presentations = renderer.getConstrainedPresentations()
    assertEquals(1, presentations.size)
    val updated = presentations.first()
    assertEquals(2, updated.root.content)
  }

  fun testListenerAdded() {
    val root = TestRootPresentation(1)
    val c1 = HorizontalConstrainedPresentation(root, horizontal(1))
    val renderer = InlineInlayRenderer(listOf(c1))
    val listener = ChangeCountingListener()
    renderer.setListener(listener)
    root.fireContentChanged(Rectangle(0,0,0,0))
    assertTrue(listener.contentChanged)
  }

  fun testListenerPreservedAfterUpdate() {
    val root = TestRootPresentation(1)
    val c1 = HorizontalConstrainedPresentation(root, horizontal(1))
    val renderer = InlineInlayRenderer(listOf(c1))

    val listener = ChangeCountingListener()
    renderer.setListener(listener)

    val c2 = HorizontalConstrainedPresentation(TestRootPresentation(2), horizontal(1))
    renderer.addOrUpdate(listOf(c2), editor, factory())

    root.fireContentChanged(Rectangle(0, 0, 0, 0))

    assertEquals(2, listener.contentChangesCount)
  }

  fun testListenerCalledAfterUpdate() {
    val root = TestRootPresentation(1)
    val c1 = HorizontalConstrainedPresentation(root, horizontal(1))
    val renderer = InlineInlayRenderer(listOf(c1))

    val listener = ChangeCountingListener()
    renderer.setListener(listener)

    val c2 = HorizontalConstrainedPresentation(TestRootPresentation(2), horizontal(1))
    renderer.addOrUpdate(listOf(c2), editor, factory())

    assertEquals(1, listener.contentChangesCount)
  }

  private fun factory() = PresentationFactory(editor as EditorImpl)

  private fun constrained(priority: Int): HorizontalConstrainedPresentation<Int> {
    return HorizontalConstrainedPresentation(TestRootPresentation(priority), horizontal(priority))
  }

  private fun horizontal(priority: Int) : HorizontalConstraints {
    return HorizontalConstraints(priority, true)
  }
}