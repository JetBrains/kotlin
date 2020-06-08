// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.readOnlyHandler.ReadonlyStatusHandlerImpl
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.project.stateStore
import com.intellij.testFramework.*
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.PathUtil
import com.intellij.util.io.readChars
import com.intellij.util.io.readText
import com.intellij.util.io.write
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.intellij.lang.annotations.Language
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

internal class ProjectStoreTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val tempDirManager = TemporaryDirectory()

  @Language("XML")
  private val iprFileContent =
    """<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="AATestComponent">
    <option name="AAvalue" value="customValue" />
  </component>
</project>""".trimIndent()

  @State(name = "AATestComponent")
  private class TestComponent : PersistentStateComponent<TestState> {
    private var state: TestState? = null

    override fun getState() = state

    override fun loadState(state: TestState) {
      this.state = state
    }
  }

  private data class TestState(var AAvalue: String = "default")

  @Test
  fun directoryBasedStorage() = runBlocking {
    loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/misc.xml", iprFileContent)
      Paths.get(it.path)
    }) { project ->
      val testComponent = test(project as ProjectEx)

      assertThat(project.basePath).isEqualTo(PathUtil.getParentPath((PathUtil.getParentPath(project.projectFilePath!!))))

      // test reload on external change
      val file = Paths.get(project.stateStore.storageManager.expandMacros(PROJECT_FILE))
      file.write(file.readText().replace("""<option name="AAvalue" value="foo" />""", """<option name="AAvalue" value="newValue" />"""))

      refreshProjectConfigDir(project)
      StoreReloadManager.getInstance().reloadChangedStorageFiles()

      assertThat(testComponent.state).isEqualTo(TestState("newValue"))

      testComponent.state!!.AAvalue = "s".repeat(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1024)
      project.stateStore.save()

      // we should save twice (first call - virtual file size is not yet set)
      testComponent.state!!.AAvalue = "b".repeat(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1024)
      project.stateStore.save()
    }
  }

  @Test
  fun fileBasedStorage() = runBlocking {
    loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
      Paths.get(it.writeChild("test${ProjectFileType.DOT_DEFAULT_EXTENSION}", iprFileContent).path)
    }) { project ->
      test(project)

      assertThat(project.basePath).isEqualTo(PathUtil.getParentPath(project.projectFilePath!!))
    }
  }

  @Test
  fun saveProjectName() = runBlocking {
    loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
      // test BOM
      val out = ByteArrayOutputStream()
      out.write(0xef)
      out.write(0xbb)
      out.write(0xbf)
      out.write(iprFileContent.toByteArray())
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/misc.xml", out.toByteArray())
      Paths.get(it.path)
    }) { project ->
      val store = project.stateStore
      assertThat(store.nameFile).doesNotExist()
      val newName = "Foo"
      val oldName = project.name
      (project as ProjectImpl).setProjectName(newName)
      project.stateStore.save()
      assertThat(store.nameFile).hasContent(newName)

      project.setProjectName("clear-read-only")
      File(store.nameFile.toUri()).setReadOnly()

      val handler = ReadonlyStatusHandler.getInstance(project) as ReadonlyStatusHandlerImpl
      try {
        handler.setClearReadOnlyInTests(true)
        project.stateStore.save()
      }
      finally {
        handler.setClearReadOnlyInTests(false)
      }
      assertThat(store.nameFile).hasContent("clear-read-only")

      project.setProjectName(oldName)
      project.stateStore.save()
      assertThat(store.nameFile).doesNotExist()
    }
  }

  @Test
  fun `saved project name must be not removed just on open`() = runBlocking {
    val name = "saved project name must be not removed just on open"
    loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/misc.xml", iprFileContent)
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/.name", name)
      Paths.get(it.path)
    }) { project ->
      val store = project.stateStore
      assertThat(store.nameFile).hasContent(name)

      project.stateStore.save()
      assertThat(store.nameFile).hasContent(name)

      (project as ProjectImpl).setProjectName(name)
      project.stateStore.save()
      assertThat(store.nameFile).hasContent(name)

      project.setProjectName("foo")
      project.stateStore.save()
      assertThat(store.nameFile).hasContent("foo")

      project.setProjectName(name)
      project.stateStore.save()
      assertThat(store.nameFile).doesNotExist()
    }
  }

  @Test
  fun `remove stalled data`() = runBlocking {
    loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/misc.xml", iprFileContent)
      @Language("XML")
      val expected = """<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ValidComponent" foo="some data" />
  <component name="AppLevelLoser" foo="old?" />
  <component name="ProjectLevelLoser" foo="old?" />
</project>""".trimIndent()
      it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/foo.xml", expected)
      Paths.get(it.path)
    }) { project ->
      val obsoleteStorageBean = ObsoleteStorageBean()
      val storageFileName = "foo.xml"
      obsoleteStorageBean.file = storageFileName
      obsoleteStorageBean.components.addAll(listOf("AppLevelLoser"))

      val projectStalledStorageBean = ObsoleteStorageBean()
      projectStalledStorageBean.file = storageFileName
      projectStalledStorageBean.isProjectLevel = true
      projectStalledStorageBean.components.addAll(listOf("ProjectLevelLoser"))
      ExtensionTestUtil.maskExtensions(OBSOLETE_STORAGE_EP, listOf(obsoleteStorageBean, projectStalledStorageBean), project)

      val componentStore = project.stateStore

      @State(name = "ValidComponent", storages = [(Storage(value = "foo.xml"))])
      class AOther : A()

      val component = AOther()
      componentStore.initComponent(component, null, null)
      assertThat(component.options.foo).isEqualTo("some data")

      componentStore.save()

      @Language("XML")
      val expected = """<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="AppLevelLoser" foo="old?" />
  <component name="ValidComponent" foo="some data" />
</project>""".trimIndent()
      assertThat(Paths.get(project.stateStore.storageManager.expandMacros(PROJECT_CONFIG_DIR)).resolve(obsoleteStorageBean.file)).isEqualTo(
        expected)
    }
  }

  @Test
  fun `save cancelled because project disposed`() = runBlocking {
    withTimeout(TimeUnit.SECONDS.toMillis(10)) {
      loadAndUseProjectInLoadComponentStateMode(tempDirManager, {
        it.writeChild("${Project.DIRECTORY_STORE_FOLDER}/misc.xml", iprFileContent)
        Paths.get(it.path)
      }) { project ->
        val testComponent = test(project as ProjectEx)
        testComponent.state!!.AAvalue = "s"
        launch {
          project.stateStore.save()
        }

        delay(50)
      }
    }
  }

  // heavy test that uses ProjectManagerImpl directly to test (opposite to DefaultProjectStoreTest)
  @Test
  fun `just created project must inherit settings from the default project`() {
    val projectManager = ProjectManagerEx.getInstanceEx()

    val testComponent = TestComponent()
    testComponent.loadState(TestState(AAvalue = "foo"))
    (projectManager.defaultProject as ComponentManager).stateStore.initComponent(testComponent, null, null)

    val newProjectPath = tempDirManager.newPath()
    val newProject = projectManager.openProject(newProjectPath, OpenProjectTask(isNewProject = true, isRefreshVfsNeeded = false))!!
    try {
      val miscXml = newProjectPath.resolve(".idea/misc.xml").readChars()
      assertThat(miscXml).contains("AATestComponent")
      assertThat(miscXml).contains("""<option name="AAvalue" value="foo" />""")
    }
    finally {
      PlatformTestUtil.forceCloseProjectWithoutSaving(newProject)
    }
  }

  private suspend fun test(project: Project): TestComponent {
    val testComponent = TestComponent()
    project.stateStore.initComponent(testComponent, null, null)
    assertThat(testComponent.state).isEqualTo(TestState("customValue"))

    testComponent.state!!.AAvalue = "foo"
    project.stateStore.save()

    val file = Paths.get(project.stateStore.storageManager.expandMacros(PROJECT_FILE))
    assertThat(file).isRegularFile
    // test exact string - xml prolog, line separators, indentation and so on must be exactly the same
    // todo get rid of default component states here
    assertThat(file.readText()).startsWith(iprFileContent.replace("customValue", "foo").replace("</project>", ""))

    return testComponent
  }
}