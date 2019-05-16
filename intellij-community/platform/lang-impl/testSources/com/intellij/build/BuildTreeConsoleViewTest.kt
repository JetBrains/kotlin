// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build

import com.intellij.build.events.impl.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.tree.TreeUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.swing.tree.TreePath

class BuildTreeConsoleViewTest: LightPlatformTestCase() {

  companion object {
    val LOG: Logger = Logger.getInstance(BuildTreeConsoleViewTest::class.java)
  }

  lateinit var treeConsoleView: BuildTreeConsoleView
  lateinit var buildDescriptor: BuildDescriptor

  @Before
  override fun setUp() {
    super.setUp()
    buildDescriptor = DefaultBuildDescriptor(Object(),
                                                 "test descriptor",
                                                 "fake path",
                                                 1L)
    treeConsoleView = BuildTreeConsoleView(getProject(), buildDescriptor, null, object : BuildViewSettingsProvider {
      override fun isExecutionViewHidden(): Boolean = false
      override fun isSideBySideView(): Boolean = true
    })
  }

  @Test
  fun `test tree console handles event`() {
    val tree = treeConsoleView.tree
    val message = "build Started"
    treeConsoleView.onEvent(StartBuildEventImpl(buildDescriptor, message))
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " build Started")
    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)
    assertThat(visitor.userObjects)
      .extracting("name")
      .containsExactly(message)
  }

  @Test
  fun `test two levels of tree console view are auto-expanded`() {
    val tree = treeConsoleView.tree
    treeConsoleView.addFilter { true }
    listOf(
      StartBuildEventImpl(buildDescriptor, "build started"),
      StartEventImpl("event_id", buildDescriptor.id, 1000, "build event"),
      StartEventImpl("sub_event_id", "event_id", 1100, "build nested event"),
      FinishEventImpl("sub_event_id", "event_id", 1200, "build nested event", SuccessResultImpl(true)),
      FinishEventImpl("event_id", buildDescriptor.id, 1500, "build event", SuccessResultImpl(true)),
      FinishBuildEventImpl(buildDescriptor.id, null, 2000, "build finished", SuccessResultImpl(true))
    ).forEach {
      treeConsoleView.onEvent(it)
    }

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " -build finished\n" +
                                           "  -build event\n" +
                                           "   build nested event")
    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)
    assertThat(visitor.userObjects)
      .extracting("name")
      .contains("build finished", "build event", "build nested event")
  }


  @After
  override fun tearDown() {
    RunAll()
      .append(ThrowableRunnable { Disposer.dispose(treeConsoleView) })
      .append(ThrowableRunnable { super.tearDown() })
      .run()
  }
}

class CollectingTreeVisitor: TreeVisitor {

  val userObjects = mutableListOf<Any?>()

  override fun visit(path: TreePath): TreeVisitor.Action {
    userObjects.add(TreeUtil.getLastUserObject(path))
    return TreeVisitor.Action.CONTINUE
  }
}