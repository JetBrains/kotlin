// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.externalSystem.action.AttachExternalProjectAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDialog
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.impl.FileChooserFactoryImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.testFramework.TestActionEvent
import org.jetbrains.plugins.gradle.action.ImportProjectFromScriptAction
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.junit.runners.Parameterized
import org.picocontainer.MutablePicoContainer
import java.awt.Component

abstract class GradleProjectImportTestCase : GradleImportingTestCase() {

  protected fun assertModules(project: Project, vararg projectInfo: ProjectInfo) {
    val modules = projectInfo.flatMap { it.modules }
    assertModules(project, *modules.toTypedArray())
  }

  protected fun assertDefaultProjectSettings(project: Project) {
    val externalProjectPath = project.basePath!!
    val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID) as GradleSettings
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath)!!
    assertEquals(projectSettings.externalProjectPath, externalProjectPath)
    assertEquals(projectSettings.isUseAutoImport, false)
    assertEquals(projectSettings.isUseQualifiedModuleNames, true)
    assertEquals(settings.storeProjectFilesExternally, true)
  }

  private fun AnAction.perform(project: Project? = null, selectedFile: VirtualFile? = null) {
    invokeAndWaitIfNeeded {
      withSelectedFileIfNeeded(selectedFile) {
        actionPerformed(TestActionEvent {
          when {
            ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.`is`(it) -> SYSTEM_ID
            CommonDataKeys.PROJECT.`is`(it) -> project
            CommonDataKeys.VIRTUAL_FILE.`is`(it) -> selectedFile
            else -> null
          }
        })
      }
    }
  }

  protected fun Project.use(action: (Project) -> Unit) {
    val project = this@use
    try {
      action(project)
    }
    finally {
      invokeAndWaitIfNeeded {
        val projectManager = ProjectManagerEx.getInstanceEx()
        projectManager.closeAndDispose(project)
      }
    }
  }

  data class ProjectInfo(val projectFile: VirtualFile, val modules: List<String>) {
    constructor(projectFile: VirtualFile, vararg modules: String) : this(projectFile, modules.toList())
  }

  protected fun generateGradleProject(prefix: String): ProjectInfo {
    val name = "${System.currentTimeMillis()}-$prefix"
    createProjectSubFile("$name-composite/settings.gradle", "rootProject.name = '$name-composite'")
    createProjectSubFile("$name-project/settings.gradle", """
      rootProject.name = '$name-project'
      include 'module'
      includeBuild '../$name-composite'
      includeFlat '$name-module'
    """.trimIndent())
    val buildScript = GradleBuildScriptBuilderEx().withJavaPlugin().generate()
    createProjectSubFile("$name-composite/build.gradle", buildScript)
    createProjectSubFile("$name-module/build.gradle", buildScript)
    createProjectSubFile("$name-project/module/build.gradle", buildScript)
    val projectFile = createProjectSubFile("$name-project/build.gradle", buildScript)
    return ProjectInfo(projectFile,
                       "$name-project", "$name-project.main", "$name-project.test",
                       "$name-project.module", "$name-project.module.main", "$name-project.module.test",
                       "$name-project.$name-module", "$name-project.$name-module.main", "$name-project.$name-module.test",
                       "$name-composite", "$name-composite.main", "$name-composite.test")
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

  protected fun openPlatformProject(projectDirectory: VirtualFile): Project {
    return invokeAndWaitIfNeeded {
      val openProcessor = PlatformProjectOpenProcessor.getInstance()
      openProcessor.doOpenProject(projectDirectory, null, true)!!
    }
  }

  protected fun openProject(projectFile: VirtualFile): Project {
    return detectOpenedProject {
      invokeAndWaitIfNeeded {
        ProjectUtil.openOrImport(projectFile.path, null, true)
      }
    }
  }

  protected fun importProject(projectFile: VirtualFile): Project {
    return detectOpenedProject {
      ImportModuleAction().perform(selectedFile = projectFile)
    }
  }

  protected fun attachProject(project: Project, projectFile: VirtualFile) {
    AttachExternalProjectAction().perform(project, selectedFile = projectFile)
  }

  protected fun attachProjectFromScript(project: Project, projectFile: VirtualFile) {
    ImportProjectFromScriptAction().perform(project, selectedFile = projectFile)
  }

  protected fun attachNewProject(projectFile: VirtualFile): Project {
    return openPlatformProject(projectFile.parent).also {
      attachProject(it, projectFile)
    }
  }

  protected fun attachNewProjectFromScript(projectFile: VirtualFile): Project {
    return openPlatformProject(projectFile.parent).also {
      attachProjectFromScript(it, projectFile)
    }
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))
  }
}