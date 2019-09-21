// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.build.*
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.replaceService
import org.assertj.core.api.Assertions

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

  private fun assertExecutionTree(viewManager: TestViewManager, expected: String, ignoreTasksOrder: Boolean) {
    Assertions.assertThat(viewManager.getBuildsMap()).hasSize(1)
    val buildView = viewManager.getBuildsMap().values.first()
    val eventView = buildView.getView(BuildTreeConsoleView::class.java.name, BuildTreeConsoleView::class.java)
    eventView!!.addFilter { true }
    edt {
      val tree = eventView.tree
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
      PlatformTestUtil.waitWhileBusy(tree)
      val treeStringPresentation = PlatformTestUtil.print(tree, false)
      if (ignoreTasksOrder) {
        assertSameElements(buildTasksNodesAsList(treeStringPresentation.trim()), buildTasksNodesAsList(expected.trim()))
      }
      else {
        assertEquals(expected.trim(), treeStringPresentation.trim())
      }
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

  interface TestViewManager : ViewManager {
    fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView>
  }

  protected class TestSyncViewManager(project: Project) :
    SyncViewManager(project), TestViewManager {
    override fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView> {
      return super.getBuildsMap()
    }
  }

  protected class TestBuildViewManager(project: Project) :
    BuildViewManager(project), TestViewManager {
    override fun getBuildsMap(): MutableMap<BuildDescriptor, BuildView> {
      return super.getBuildsMap()
    }
  }

  override fun handleImportFailure(errorMessage: String, errorDetails: String?) {
    // do not fail tests with failed builds
  }
}
