// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.projectWizard.NewProjectWizard
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.projectWizard.ProjectTypeStep
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleWizardStep
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.runners.Parameterized


abstract class GradleCreateProjectTestCase : GradleImportingTestCase() {

  data class ProjectInfo(val root: VirtualFile, val modules: List<ModuleInfo>) {
    constructor(root: VirtualFile, vararg modules: ModuleInfo) : this(root, modules.toList())
  }

  data class ModuleInfo(val name: String, val root: VirtualFile, val modulesPerSourceSet: List<String>) {
    constructor(name: String, root: VirtualFile, vararg modulesPerSourceSet: String) :
      this(name, root, modulesPerSourceSet.toList())
  }

  fun assertProjectStructure(project: Project, projectInfo: ProjectInfo) {
    val modules = projectInfo.modules.map { it.name }
    val sourceSetModules = projectInfo.modules.flatMap { it.modulesPerSourceSet }
    assertModules(project, *(modules + sourceSetModules).toTypedArray())
  }

  fun deleteProject(projectInfo: ProjectInfo) {
    invokeAndWaitIfNeeded {
      runWriteAction {
        for (module in projectInfo.modules) {
          val root = module.root
          if (root.exists()) {
            root.delete(null)
          }
        }
      }
    }
  }

  fun generateProjectInfo(id: String): ProjectInfo {
    val name = "${System.currentTimeMillis()}-$id"
    val projectDir = createProjectSubDir("$name-project")
    val projectModuleDir = createProjectSubDir("$name-project/module")
    val externalModuleDir = createProjectSubDir("$name-module")
    return ProjectInfo(projectDir,
                       ModuleInfo("$name-project", projectDir, "$name-project.main", "$name-project.test"),
                       ModuleInfo("$name-project.module", projectModuleDir, "$name-project.module.main", "$name-project.module.test"),
                       ModuleInfo("$name-project.$name-module", externalModuleDir, "$name-project.$name-module.main",
                                  "$name-project.$name-module.test")
    )
  }

  protected fun assertDefaultProjectSettings(project: Project) {
    val externalProjectPath = project.basePath!!
    val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID) as GradleSettings
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath)!!
    assertEquals(projectSettings.externalProjectPath, externalProjectPath)
    assertEquals(projectSettings.isUseAutoImport, false)
    assertEquals(projectSettings.isUseQualifiedModuleNames, true)
    assertEquals(settings.storeProjectFilesExternally, true)
  }

  fun createProject(projectInfo: ProjectInfo): Project {
    val projectDirectory = projectInfo.root
    val project = createProject(projectDirectory.path) {
      configureProjectWizardStep(projectDirectory, it)
    }
    for (module in projectInfo.modules) {
      val moduleDirectory = module.root
      if (moduleDirectory.path == projectDirectory.path) continue
      createModule(moduleDirectory.path, project) {
        configureModuleWizardStep(moduleDirectory, project, it)
      }
    }
    return project
  }

  private fun createProject(directory: String, configure: (ModuleWizardStep) -> Unit): Project {
    val project = invokeAndWaitIfNeeded {
      val wizard = createWizard(null, directory)
      wizard.runWizard(configure)
      NewProjectUtil.createFromWizard(wizard, null)
    }
    waitForImportCompletion()
    return project
  }

  private fun createModule(directory: String, project: Project, configure: (ModuleWizardStep) -> Unit) {
    invokeAndWaitIfNeeded {
      val wizard = createWizard(project, directory)
      wizard.runWizard(configure)
      NewModuleAction().createModuleFromWizard(project, null, wizard)
    }
    waitForImportCompletion()
  }

  private fun createWizard(project: Project?, directory: String): AbstractProjectWizard {
    val modulesProvider = project?.let { DefaultModulesProvider(it) } ?: ModulesProvider.EMPTY_MODULES_PROVIDER
    return NewProjectWizard(project, modulesProvider, directory).also {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  private fun AbstractProjectWizard.runWizard(configure: ModuleWizardStep.() -> Unit) {
    while (true) {
      val currentStep = currentStepObject
      currentStep.configure()
      if (isLast) break
      doNextAction()
      if (currentStep === currentStepObject) {
        throw RuntimeException("$currentStep is not validated")
      }
    }
    doFinishAction()
  }

  private fun waitForImportCompletion() {
    invokeAndWaitIfNeeded {
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }
  }

  private fun configureProjectWizardStep(directory: VirtualFile, step: ModuleWizardStep) {
    when (step) {
      is ProjectTypeStep -> {
        step.setSelectedTemplate("Gradle", null)
      }
      is GradleModuleWizardStep -> {
        step.setGroupId("org.example")
        step.setArtifactId(directory.name)
      }
      is ProjectSettingsStep -> {
        step.setNameValue(directory.name)
        step.setPath(directory.path)
      }
    }
  }

  private fun configureModuleWizardStep(directory: VirtualFile, project: Project, step: ModuleWizardStep) {
    when (step) {
      is ProjectTypeStep -> {
        step.setSelectedTemplate("Gradle", null)
      }
      is GradleModuleWizardStep -> {
        val projectPath = project.basePath!!
        val projectData = ExternalSystemApiUtil.findProjectData(project, GradleConstants.SYSTEM_ID, projectPath)!!
        step.setParentProject(projectData.data)
        step.setArtifactId(directory.name)
      }
      is ProjectSettingsStep -> {
        step.setNameValue(directory.name)
        step.setPath(directory.path)
      }
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