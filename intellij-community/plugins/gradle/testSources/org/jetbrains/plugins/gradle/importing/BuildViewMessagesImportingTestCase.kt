// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.build.*
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.util.concurrency.Semaphore
import com.intellij.util.ui.tree.TreeUtil
import junit.framework.TestCase
import org.assertj.core.api.Assertions
import javax.swing.tree.DefaultMutableTreeNode

abstract class BuildViewMessagesImportingTestCase : GradleImportingTestCase() {

  private lateinit var syncViewManager: TestSyncViewManager
  private lateinit var buildViewManager: TestBuildViewManager

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    currentExternalProjectSettings.delegatedBuild = true
    useProjectTaskManager = true
    myProject.replaceService(BuildContentManager::class.java, BuildContentManagerImpl(myProject), testRootDisposable)
    syncViewManager = TestSyncViewManager(myProject)
    myProject.replaceService(SyncViewManager::class.java, syncViewManager, testRootDisposable)
    buildViewManager = TestBuildViewManager(myProject)
    myProject.replaceService(BuildViewManager::class.java, buildViewManager, testRootDisposable)
  }

  protected fun assertSyncViewTreeEquals(executionTreeText: String) {
    assertExecutionTree(syncViewManager, executionTreeText, false)
  }

  protected fun assertSyncViewTreeSame(executionTreeText: String) {
    assertExecutionTree(syncViewManager, executionTreeText, true)
  }

  protected fun assertBuildViewTreeEquals(executionTree: String) {
    assertExecutionTree(buildViewManager, executionTree, false)
  }

  protected fun assertBuildViewTreeSame(executionTree: String) {
    assertExecutionTree(buildViewManager, executionTree, true)
  }

  protected fun assertSyncViewSelectedNode(nodeText: String, consoleText: String) {
    assertExecutionTreeNode(syncViewManager, nodeText, consoleText, true)
  }

  protected fun assertBuildViewSelectedNode(nodeText: String, consoleText: String) {
    assertExecutionTreeNode(buildViewManager, nodeText, consoleText, true)
  }

  private fun assertExecutionTree(viewManager: TestViewManager, expected: String, ignoreTasksOrder: Boolean) {
    val recentBuild = viewManager.getRecentBuild()
    val buildView = viewManager.getBuildsMap()[recentBuild]
    val eventView = buildView!!.getView(BuildTreeConsoleView::class.java.name, BuildTreeConsoleView::class.java)
    eventView!!.addFilter { true }
    viewManager.waitForPendingBuilds()
    val treeStringPresentation = runInEdtAndGet {
      val tree = eventView.tree
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)
      return@runInEdtAndGet PlatformTestUtil.print(tree, false)
    }
    if (ignoreTasksOrder) {
      assertSameElements(buildTasksNodesAsList(treeStringPresentation.trim()), buildTasksNodesAsList(expected.trim()))
    }
    else {
      assertEquals(expected.trim(), treeStringPresentation.trim())
    }
  }

  private fun buildTasksNodesAsList(treeStringPresentation: String): List<String> {
    val list = mutableListOf<String>()
    val buffer = StringBuilder()
    for (line in treeStringPresentation.lineSequence()) {
      if (line.startsWith(" -") || line.startsWith("  :") || line.startsWith("  -")) {
        list.add(buffer.toString())
        buffer.clear()
      }
      buffer.appendln(line)
    }
    if (buffer.isNotEmpty()) {
      list.add(buffer.toString())
    }
    return list
  }

  private fun assertExecutionTreeNode(viewManager: TestViewManager, nodeText: String, consoleText: String, assertSelected: Boolean) {
    val recentBuild = viewManager.getRecentBuild()
    val buildView = viewManager.getBuildsMap()[recentBuild]
    val eventView = buildView!!.getView(BuildTreeConsoleView::class.java.name, BuildTreeConsoleView::class.java)
    eventView!!.addFilter { true }
    viewManager.waitForPendingBuilds()

    val tree = eventView.tree
    val node = runInEdtAndGet {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)

      TreeUtil.findNode(tree.model.root as DefaultMutableTreeNode) {
        val userObject = it.userObject
        userObject is ExecutionNode && userObject.name == nodeText
      }
    }
    if (!assertSelected && node != tree.selectionPath!!.lastPathComponent) {
      edt {
        TreeUtil.selectNode(tree, node)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        PlatformTestUtil.waitWhileBusy(tree)
      }
    }
    val selectedPathComponent = tree.selectionPath!!.lastPathComponent
    if (node != selectedPathComponent) {
      assertEquals(node.toString(), selectedPathComponent.toString())
    }
    assertEquals(consoleText, eventView.selectedNodeConsoleText)
  }

  interface TestViewManager : ViewManager {
    fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView>
    fun waitForPendingBuilds()
    fun getRecentBuild(): BuildDescriptor
  }

  protected class TestSyncViewManager(project: Project) :
    SyncViewManager(project), TestViewManager {
    private val semaphore = Semaphore()
    private lateinit var recentBuild: BuildDescriptor
    override fun waitForPendingBuilds() = TestCase.assertTrue(semaphore.waitFor(1000))
    override fun getRecentBuild(): BuildDescriptor = recentBuild
    override fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView> = super.getBuildsMap()

    override fun onBuildStart(buildDescriptor: BuildDescriptor) {
      super.onBuildStart(buildDescriptor)
      recentBuild = buildDescriptor
      semaphore.down()
    }

    override fun onBuildFinish(buildDescriptor: BuildDescriptor) {
      super.onBuildFinish(buildDescriptor)
      semaphore.up()
    }
  }

  protected class TestBuildViewManager(project: Project) :
    BuildViewManager(project), TestViewManager {
    private val semaphore = Semaphore()
    private lateinit var recentBuild: BuildDescriptor
    override fun waitForPendingBuilds() = TestCase.assertTrue(semaphore.waitFor(1000))
    override fun getRecentBuild(): BuildDescriptor = recentBuild
    override fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView> = super.getBuildsMap()
    override fun onBuildStart(buildDescriptor: BuildDescriptor) {
      super.onBuildStart(buildDescriptor)
      recentBuild = buildDescriptor
      semaphore.down()
    }

    override fun onBuildFinish(buildDescriptor: BuildDescriptor?) {
      super.onBuildFinish(buildDescriptor)
      semaphore.up()
    }
  }

  override fun handleImportFailure(errorMessage: String, errorDetails: String?) {
    // do not fail tests with failed builds
  }
}
