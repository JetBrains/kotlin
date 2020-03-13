// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.stateStore
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.ComponentManagerImpl
import com.intellij.serviceContainer.processAllImplementationClasses
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.PlatformTestUtil.useAppConfigDir
import com.intellij.testFramework.TemporaryDirectory
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.createOrLoadProject
import com.intellij.util.io.getDirectoryTree
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Paths

internal class DoNotSaveDefaultsTest {
  companion object {
    @JvmField @ClassRule val appRule = ApplicationRule()
  }

  @JvmField @Rule val tempDir = TemporaryDirectory()

  @Test fun testApp() = useAppConfigDir {
    runBlocking {
      doTest(ApplicationManager.getApplication() as ComponentManagerImpl)
    }
  }

  @Test fun testProject() = runBlocking {
    createOrLoadProject(tempDir, directoryBased = false) { project ->
      doTest(project as ComponentManagerImpl)
    }
  }

  @Test fun `project - load empty state`() = runBlocking {
    createOrLoadProject(tempDir, directoryBased = false) { project ->
      doTest(project as ComponentManagerImpl, isTestEmptyState = true)
    }
  }

  private suspend fun doTest(componentManager: ComponentManagerImpl, isTestEmptyState: Boolean = false) {
    // wake up (edt, some configurables want read action)
    withContext(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
      val picoContainer = componentManager.picoContainer
      processAllImplementationClasses(componentManager.picoContainer) { clazz, _ ->
        val className = clazz.name
        // CvsTabbedWindow calls invokeLater in constructor
        if (className != "com.intellij.cvsSupport2.ui.CvsTabbedWindow"
            && className != "com.intellij.lang.javascript.bower.BowerPackagingService"
            && !className.endsWith(".MessDetectorConfigurationManager")
            && className != "org.jetbrains.plugins.groovy.mvc.MvcConsole") {
          val instance = picoContainer.getComponentInstance(className)
          if (isTestEmptyState && instance is PersistentStateComponent<*>) {
            testEmptyState(instance)
          }
        }
        true
      }
    }

    if (componentManager !is Project) {
      val propertyComponent = PropertiesComponent.getInstance()
      // <property name="file.gist.reindex.count" value="54" />
      propertyComponent.unsetValue("file.gist.reindex.count")
      propertyComponent.unsetValue("android-component-compatibility-check")
      // <property name="CommitChangeListDialog.DETAILS_SPLITTER_PROPORTION_2" value="1.0" />
      propertyComponent.unsetValue("CommitChangeListDialog.DETAILS_SPLITTER_PROPORTION_2")
      propertyComponent.unsetValue("ts.lib.d.ts.version")
      propertyComponent.unsetValue("nodejs_interpreter_path.stuck_in_default_project")
    }

    val useModCountOldValue = System.getProperty("store.save.use.modificationCount")
    try {
      System.setProperty("store.save.use.modificationCount", "false")
      componentManager.stateStore.save(forceSavingAllSettings = true)
    }
    finally {
      System.setProperty("store.save.use.modificationCount", useModCountOldValue ?: "false")
    }

    if (componentManager is Project) {
      assertThat(Paths.get(componentManager.projectFilePath!!)).doesNotExist()
      return
    }

    val directoryTree = Paths.get(componentManager.stateStore.storageManager.expandMacros(APP_CONFIG)).getDirectoryTree(hashSetOf(
      "path.macros.xml" /* todo EP to register (provide) macro dynamically */,
      "stubIndex.xml" /* low-level non-roamable stuff */,
      "usage.statistics.xml" /* SHOW_NOTIFICATION_ATTR in internal mode */,
      "tomee.extensions.xml", "jboss.extensions.xml",
      "glassfish.extensions.xml" /* javaee non-roamable stuff, it will be better to fix it */,
      "dimensions.xml" /* non-roamable sizes of window, dialogs, etc. */,
      "databaseSettings.xml" /* android garbage */,
      "updates.xml"
    ))
    println(directoryTree)
    assertThat(directoryTree).isEmpty()
  }

  private fun testEmptyState(instance: PersistentStateComponent<*>) {
    val stateSpec = getStateSpec(javaClass) ?: return
    if (stateSpec.defaultStateAsResource) {
      // component expect some default state if no state
      return
    }

    val stateClass = ComponentSerializationUtil.getStateClass<Any>(instance.javaClass)
    val emptyState = try {
      deserializeState(Element("state"), stateClass, null)!!
    }
    catch (e: Exception) {
      throw RuntimeException("Cannot create empty state for ${instance.javaClass}", e)
    }
    @Suppress("UNCHECKED_CAST")
    (instance as PersistentStateComponent<Any>).loadState(emptyState)
  }
}