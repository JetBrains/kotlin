// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.externalDependencies.ProjectExternalDependency
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.PathManagerEx
import com.intellij.openapi.components.*
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.*
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.io.delete
import com.intellij.util.io.getDirectoryTree
import com.intellij.util.isEmpty
import kotlinx.coroutines.runBlocking
import org.jdom.Element
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Paths

private const val TEST_COMPONENT_NAME = "DefaultProjectStoreTestComponent"

@State(name = TEST_COMPONENT_NAME, storages = [(Storage(value = "testSchemes", stateSplitter = TestStateSplitter::class))])
private class TestComponent : PersistentStateComponent<Element> {
  private var element = Element("state")

  override fun getState() = element.clone()

  override fun loadState(state: Element) {
    element = state.clone()
  }
}

internal class DefaultProjectStoreTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @JvmField
  @Rule
  val fsRule = InMemoryFsRule()

  private val tempDirManager = TemporaryDirectory()
  private val requiredPlugins = listOf<ProjectExternalDependency>(DependencyOnPlugin("fake", "0", "1"))

  @JvmField
  @Rule
  val ruleChain = RuleChain(
    tempDirManager,
    WrapRule {
      val path = Paths.get(ApplicationManager.getApplication().stateStore.storageManager.expandMacros(APP_CONFIG))
      return@WrapRule {
        path.delete()
      }
    }
  )

  @Test
  fun `new project from default - file-based storage`() {
    val externalDependenciesManager = ProjectManager.getInstance().defaultProject.service<ExternalDependenciesManager>()
    externalDependenciesManager.allDependencies = requiredPlugins
    try {
      createProjectAndUseInLoadComponentStateMode(tempDirManager) {
        assertThat(it.service<ExternalDependenciesManager>().allDependencies).isEqualTo(requiredPlugins)
      }
    }
    finally {
      externalDependenciesManager.allDependencies = emptyList()
    }
  }

  @Test
  fun `new project from default - directory-based storage`() {
    val defaultTestComponent = TestComponent()
    defaultTestComponent.loadState(JDOMUtil.load("""
      <component>
        <main name="$TEST_COMPONENT_NAME"/><sub name="foo" /><sub name="bar" />
      </component>""".trimIndent()))
    val stateStore = ProjectManager.getInstance().defaultProject.stateStore as ComponentStoreImpl
    stateStore.initComponent(defaultTestComponent, null, null)
    try {
      // obviously, project must be directory-based also
      createProjectAndUseInLoadComponentStateMode(tempDirManager, directoryBased = true) {
        val component = TestComponent()
        it.stateStore.initComponent(component, null, null)
        assertThat(component.state).isEqualTo(defaultTestComponent.state)
      }
    }
    finally {
      // clear state
      defaultTestComponent.loadState(Element("empty"))
      val defaultStore = ProjectManager.getInstance().defaultProject.stateStore as ComponentStoreImpl
      runBlocking {
        defaultStore.save()
      }
      defaultStore.removeComponent(TEST_COMPONENT_NAME)
    }
  }

  @Test
  fun `new project from default - remove workspace component configuration`() {
    val testData = Paths.get(PathManagerEx.getCommunityHomePath(), "platform/configuration-store-impl/testData")
    val element = JDOMUtil.load(testData.resolve("testData1.xml"))

    val tempDir = fsRule.fs.getPath("")
    normalizeDefaultProjectElement(ProjectManager.getInstance().defaultProject, element, tempDir)
    assertThat(element.isEmpty()).isTrue()

    val directoryTree = tempDir.getDirectoryTree()
    assertThat(directoryTree).toMatchSnapshot(testData.resolve("testData1.txt"))
  }

  @Test
  fun `new IPR project from default - remove workspace component configuration`() {
    val testData = Paths.get(PathManagerEx.getCommunityHomePath(), "platform/configuration-store-impl/testData")
    val element = JDOMUtil.load(testData.resolve("testData1.xml"))

    val tempDir = fsRule.fs.getPath("")
    val projectFile = tempDir.resolve("test.ipr")
    moveComponentConfiguration(ProjectManager.getInstance().defaultProject, element, { "" }) {
      if (it == "workspace.xml") tempDir.resolve("test.iws") else { projectFile }
    }
    assertThat(JDOMUtil.isEmpty(element)).isTrue()
    assertThat(tempDir.getDirectoryTree()).toMatchSnapshot(testData.resolve("testData1-ipr.txt"))
  }
}