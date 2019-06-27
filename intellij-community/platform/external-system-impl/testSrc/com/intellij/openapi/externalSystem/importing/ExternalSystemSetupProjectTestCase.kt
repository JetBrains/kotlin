// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.importing

import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDialog
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.impl.FileChooserFactoryImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.testFramework.TestActionEvent
import org.junit.Assert.assertEquals
import org.picocontainer.MutablePicoContainer
import java.awt.Component

interface ExternalSystemSetupProjectTestCase {

  data class ProjectInfo(val projectFile: VirtualFile, val modules: List<String>) {
    constructor(projectFile: VirtualFile, vararg modules: String) : this(projectFile, modules.toList())
  }

  fun generateProject(id: String): ProjectInfo

  fun getSystemId(): ProjectSystemId

  fun assertDefaultProjectSettings(project: Project)

  fun attachProject(project: Project, projectFile: VirtualFile)

  fun attachProjectFromScript(project: Project, projectFile: VirtualFile)

  fun openPlatformProjectFrom(projectDirectory: VirtualFile): Project {
    return invokeAndWaitIfNeeded {
      val openProcessor = PlatformProjectOpenProcessor.getInstance()
      openProcessor.doOpenProject(projectDirectory, null, true)!!
    }
  }

  fun openProjectFrom(projectFile: VirtualFile): Project {
    return detectOpenedProject {
      invokeAndWaitIfNeeded {
        ProjectUtil.openOrImport(projectFile.path, null, true)
      }
    }
  }

  fun importProjectFrom(projectFile: VirtualFile): Project {
    return detectOpenedProject {
      ImportModuleAction().perform(selectedFile = projectFile)
    }
  }

  fun assertModules(project: Project, vararg projectInfo: ProjectInfo) {
    val expectedNames = projectInfo.flatMap { it.modules }
    val actual = ModuleManager.getInstance(project).modules
    val actualNames = actual.map { it.name }
    assertEquals(HashSet(expectedNames), HashSet(actualNames))
  }

  fun AnAction.perform(project: Project? = null, selectedFile: VirtualFile? = null) {
    invokeAndWaitIfNeeded {
      withSelectedFileIfNeeded(selectedFile) {
        actionPerformed(TestActionEvent {
          when {
            ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.`is`(it) -> getSystemId()
            CommonDataKeys.PROJECT.`is`(it) -> project
            CommonDataKeys.VIRTUAL_FILE.`is`(it) -> selectedFile
            else -> null
          }
        })
      }
    }
  }

  fun Project.use(save: Boolean = false, action: (Project) -> Unit) {
    val project = this@use
    try {
      action(project)
    }
    finally {
      invokeAndWaitIfNeeded {
        val projectManager = ProjectManagerEx.getInstanceEx()
        if (save) {
          projectManager.closeAndDispose(project)
        }
        else {
          projectManager.forceCloseProject(project, true)
        }
      }
    }
  }

  private fun <R> withSelectedFileIfNeeded(selectedFile: VirtualFile?, action: () -> R): R {
    if (selectedFile == null) return action()
    val fileChooser = findApplicationComponent(FileChooserFactory::class.java)
    replaceApplicationComponent(FileChooserFactory::class.java, object : FileChooserFactoryImpl() {
      override fun createFileChooser(descriptor: FileChooserDescriptor, project: Project?, parent: Component?): FileChooserDialog {
        return object : FileChooserDialog {
          override fun choose(toSelect: VirtualFile?, project: Project?): Array<VirtualFile> {
            return choose(project, toSelect)
          }

          override fun choose(project: Project?, vararg toSelect: VirtualFile?): Array<VirtualFile> {
            return arrayOf(selectedFile)
          }
        }
      }
    })
    try {
      return action()
    }
    finally {
      replaceApplicationComponent(FileChooserFactory::class.java, fileChooser)
    }
  }

  private fun <T> replaceApplicationComponent(cls: Class<T>, instance: T) {
    val key = cls.name
    val container = ApplicationManager.getApplication().picoContainer as MutablePicoContainer
    container.unregisterComponent(key)
    container.registerComponentInstance(key, instance)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> findApplicationComponent(cls: Class<T>): T {
    val key = cls.name
    val container = ApplicationManager.getApplication().picoContainer as MutablePicoContainer
    return container.getComponentInstance(key) as T
  }

  private fun detectOpenedProject(action: () -> Unit): Project {
    val projectManager = ProjectManager.getInstance()
    val openProjects = projectManager.openProjects.map { it.name }.toSet()
    action()
    return projectManager.openProjects.first { it.name !in openProjects }
  }
}