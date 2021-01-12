// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build

import com.intellij.build.events.MessageEvent
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
import org.assertj.core.groups.Tuple
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import javax.swing.tree.TreePath

class BuildTreeConsoleViewTest : LightPlatformTestCase() {

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
    treeConsoleView = BuildTreeConsoleView(project, buildDescriptor, null, object : BuildViewSettingsProvider {
      override fun isExecutionViewHidden(): Boolean = false
      override fun isSideBySideView(): Boolean = true
    })
  }

  @Test
  fun `test tree console handles event`() {
    val tree = treeConsoleView.tree
    val buildId = Object()
    val message = "build Started"
    treeConsoleView.onEvent(buildId, StartBuildEventImpl(buildDescriptor, message))
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
  fun `test build level of tree console view are auto-expanded`() {
    val tree = treeConsoleView.tree
    treeConsoleView.addFilter { true }
    val buildId = Object()
    listOf(
      StartBuildEventImpl(buildDescriptor, "build started"),
      StartEventImpl("event_id", buildDescriptor.id, 1000, "build event"),
      StartEventImpl("sub_event_id", "event_id", 1100, "build nested event"),
      FinishEventImpl("sub_event_id", "event_id", 1200, "build nested event", SuccessResultImpl(true)),
      FinishEventImpl("event_id", buildDescriptor.id, 1500, "build event", SuccessResultImpl(true)),
      FinishBuildEventImpl(buildDescriptor.id, null, 2000, "build finished", SuccessResultImpl(true))
    ).forEach {
      treeConsoleView.onEvent(buildId, it)
    }

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " -build finished\n" +
                                           "  +build event")
    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)
    assertThat(visitor.userObjects)
      .extracting("name")
      .contains("build finished", "build event")
  }

  @Test
  fun `test first message node is auto-expanded`() {
    val tree = treeConsoleView.tree
    treeConsoleView.addFilter { true }
    val buildId = Object()
    listOf(
      StartBuildEventImpl(buildDescriptor, "build started"),

      StartEventImpl("event_id_1", buildDescriptor.id, 1000, "build event 1"),
      FileMessageEventImpl("event_id_1", MessageEvent.Kind.WARNING, null, "file message", null, FilePosition(File("a.file"), 0, 0)),
      FinishEventImpl("event_id_1", buildDescriptor.id, 1500, "build event 1", SuccessResultImpl(true)),

      StartEventImpl("event_id_2", buildDescriptor.id, 1000, "build event 2"),
      FileMessageEventImpl("event_id_2", MessageEvent.Kind.WARNING, null, "file message 1", null, FilePosition(File("a1.file"), 0, 0)),
      FileMessageEventImpl("event_id_2", MessageEvent.Kind.WARNING, null, "file message 2", null, FilePosition(File("a2.file"), 0, 0)),
      FinishEventImpl("event_id_2", buildDescriptor.id, 1500, "build event 2", SuccessResultImpl(true)),

      StartEventImpl("event_id_3", buildDescriptor.id, 1000, "build event 3"),
      FileMessageEventImpl("event_id_3", MessageEvent.Kind.WARNING, null, "file message 3", null, FilePosition(File("a3.file"), 0, 0)),
      FileMessageEventImpl("event_id_3", MessageEvent.Kind.ERROR, null, "file message with error", null,
                           FilePosition(File("a4.file"), 5, 0)),
      FinishEventImpl("event_id_3", buildDescriptor.id, 1500, "build event 3", FailureResultImpl()),

      FinishBuildEventImpl(buildDescriptor.id, null, 2000, "build failed", FailureResultImpl())
    ).forEach {
      treeConsoleView.onEvent(buildId, it)
    }

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " -build failed\n" +
                                           "  -build event 1\n" +
                                           "   -a.file\n" +
                                           "    file message\n" +
                                           "  +build event 2\n" +
                                           "  -build event 3\n" +
                                           "   +a3.file\n" +
                                           "   -a4.file\n" +
                                           "    file message with error")

    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)
    assertThat(visitor.userObjects)
      .extracting("name", "myHint")
      .contains(
        Tuple("file message", ":1"),
        Tuple("file message with error", ":6")
      )
  }

  @Test
  fun `test derived result depend on child result - fail case`() {
    treeConsoleView.addFilter { true }
    val buildId = Object()
    listOf(
      StartBuildEventImpl(buildDescriptor, "build started"),
      StartEventImpl("event_id", buildDescriptor.id, 1000, "build event"),
      StartEventImpl("sub_event_id", "event_id", 1100, "build nested event"),
      MessageEventImpl("event_id", MessageEvent.Kind.ERROR, "Error", "error message", "error message"),
      FinishEventImpl("sub_event_id", "event_id", 1200, "build nested event", FailureResultImpl()),
      FinishEventImpl("event_id", buildDescriptor.id, 1500, "build event", DerivedResultImpl()),
      FinishBuildEventImpl(buildDescriptor.id, null, 2000, "build finished", DerivedResultImpl())
    ).forEach {
      treeConsoleView.onEvent(buildId, it)
    }

    val tree = treeConsoleView.tree

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " -build finished\n" +
                                           "  -build event\n" +
                                           "   build nested event\n" +
                                           "   error message")
    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)


    assertThat(visitor.userObjects.map { (it as ExecutionNode).name + "--" + it.result!!.javaClass.simpleName })
      .containsExactly("build finished--FailureResultImpl", "build event--FailureResultImpl", "build nested event--FailureResultImpl",
                       "error message--")
  }

  @Test
  fun `test derived result depend on child result - success case`() {
    treeConsoleView.addFilter { true }
    val buildId = Object()
    listOf(
      StartBuildEventImpl(buildDescriptor, "build started"),
      StartEventImpl("event_id", buildDescriptor.id, 1000, "build event"),
      StartEventImpl("sub_event_id", "event_id", 1100, "build nested event"),
      FinishEventImpl("sub_event_id", "event_id", 1200, "build nested event", SuccessResultImpl()),
      FinishEventImpl("event_id", buildDescriptor.id, 1500, "build event", DerivedResultImpl()),
      FinishBuildEventImpl(buildDescriptor.id, null, 2000, "build finished", DerivedResultImpl())
    ).forEach {
      treeConsoleView.onEvent(buildId, it)
    }

    val tree = treeConsoleView.tree

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    PlatformTestUtil.waitWhileBusy(tree)

    PlatformTestUtil.assertTreeEqual(tree, "-\n" +
                                           " -build finished\n" +
                                           "  +build event")
    val visitor = CollectingTreeVisitor()
    TreeUtil.visitVisibleRows(tree, visitor)

    assertThat(visitor.userObjects.map { (it as ExecutionNode).name + "--" + it.result!!.javaClass.simpleName })
      .containsExactly("build finished--SuccessResultImpl", "build event--SuccessResultImpl")

  }


  @After
  override fun tearDown() {
    RunAll()
      .append(ThrowableRunnable { Disposer.dispose(treeConsoleView) })
      .append(ThrowableRunnable { super.tearDown() })
      .run()
  }
}

class CollectingTreeVisitor : TreeVisitor {

  val userObjects = mutableListOf<Any?>()

  override fun visit(path: TreePath): TreeVisitor.Action {
    userObjects.add(TreeUtil.getLastUserObject(path))
    return TreeVisitor.Action.CONTINUE
  }
}