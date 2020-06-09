// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.projectWizard.NewProjectWizard
import com.intellij.ide.projectWizard.ProjectTypeStep
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtil.convertLineSeparators
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.plugins.gradle.org.jetbrains.plugins.gradle.util.ProjectInfoBuilder
import org.jetbrains.plugins.gradle.org.jetbrains.plugins.gradle.util.ProjectInfoBuilder.ModuleInfo
import org.jetbrains.plugins.gradle.org.jetbrains.plugins.gradle.util.ProjectInfoBuilder.ProjectInfo
import org.jetbrains.plugins.gradle.service.project.wizard.GradleFrameworksWizardStep
import org.jetbrains.plugins.gradle.service.project.wizard.GradleStructureWizardStep
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.runners.Parameterized
import java.io.File
import java.util.concurrent.TimeUnit
import com.intellij.openapi.externalSystem.util.use as utilUse


abstract class GradleCreateProjectTestCase : GradleImportingTestCase() {

  fun Project.assertProjectStructure(projectInfo: ProjectInfo) {
    val rootModule = projectInfo.rootModule.ideName
    val rootSourceSetModules = projectInfo.rootModule.modulesPerSourceSet
    val modules = projectInfo.modules.map { it.ideName }
    val sourceSetModules = projectInfo.modules.flatMap { it.modulesPerSourceSet }
    assertModules(this, rootModule, *rootSourceSetModules.toTypedArray(), *modules.toTypedArray(), *sourceSetModules.toTypedArray())
  }

  fun deleteProject(projectInfo: ProjectInfo) {
    ApplicationManager.getApplication().invokeAndWait {
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

  fun projectInfo(id: String, useKotlinDsl: Boolean = false, configure: ProjectInfoBuilder.() -> Unit): ProjectInfo {
    return ProjectInfoBuilder.projectInfo(id, myProjectRoot) {
      this.useKotlinDsl = useKotlinDsl
      configure()
    }
  }

  protected fun Project.assertDefaultProjectSettings() {
    val externalProjectPath = basePath!!
    val settings = getSettings(this, GradleConstants.SYSTEM_ID) as GradleSettings
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath)!!
    assertEquals(projectSettings.externalProjectPath, externalProjectPath)
    assertEquals(projectSettings.isUseQualifiedModuleNames, true)
    assertEquals(settings.storeProjectFilesExternally, true)
  }

  fun withProject(projectInfo: ProjectInfo, save: Boolean = false, action: Project.() -> Unit) {
    createProject(projectInfo).use(save = save) { project ->
      for (moduleInfo in projectInfo.modules) {
        createModule(moduleInfo, project)
      }
      project.action()
    }
  }

  private fun createProject(projectInfo: ProjectInfo): Project {
    return createProject(projectInfo.rootModule.root.path) { step ->
      configureWizardStepSettings(step, projectInfo.rootModule, null)
    }
  }

  private fun createModule(moduleInfo: ModuleInfo, project: Project) {
    val parentData = findProjectData(project, GradleConstants.SYSTEM_ID, project.basePath!!)!!
    return createModule(moduleInfo.root.path, project) { step ->
      configureWizardStepSettings(step, moduleInfo, parentData.data)
    }
  }

  private fun configureWizardStepSettings(step: ModuleWizardStep, moduleInfo: ModuleInfo, parentData: ProjectData?) {
    when (step) {
      is ProjectTypeStep -> {
        step.setSelectedTemplate("Gradle", null)
        val frameworksStep = step.frameworksStep
        frameworksStep as GradleFrameworksWizardStep
        frameworksStep.setUseKotlinDsl(moduleInfo.useKotlinDsl)
      }
      is GradleStructureWizardStep -> {
        step.parentData = parentData
        moduleInfo.groupId?.let { step.groupId = it }
        step.artifactId = moduleInfo.artifactId
        moduleInfo.version?.let { step.version = it }
        step.entityName = moduleInfo.simpleName
        step.location = moduleInfo.root.path
      }
    }
  }

  private fun createProject(directory: String, configure: (ModuleWizardStep) -> Unit): Project {
    return waitForProjectReload(alsoWaitForPreview = true) {
      invokeAndWaitIfNeeded {
        val wizard = createWizard(null, directory)
        wizard.runWizard(configure)
        wizard.disposeIfNeeded()
        NewProjectUtil.createFromWizard(wizard, null)
      }
    }
  }

  private fun createModule(directory: String, project: Project, configure: (ModuleWizardStep) -> Unit) {
    waitForProjectReload {
      ApplicationManager.getApplication().invokeAndWait {
        val wizard = createWizard(project, directory)
        wizard.runWizard(configure)
        wizard.disposeIfNeeded()
        NewModuleAction().createModuleFromWizard(project, null, wizard)
      }
    }
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
        throw RuntimeException("$currentStepObject is not validated")
      }
    }
    if (!doFinishAction()) {
      throw RuntimeException("$currentStepObject is not validated")
    }
  }

  fun assertSettingsFileContent(projectInfo: ProjectInfo) {
    val builder = StringBuilder()
    val rootModuleInfo = projectInfo.rootModule
    val useKotlinDsl = rootModuleInfo.useKotlinDsl
    builder.appendln(defineProject(rootModuleInfo.artifactId, useKotlinDsl))
    for (moduleInfo in projectInfo.modules) {
      val externalName = moduleInfo.externalName
      val artifactId = moduleInfo.artifactId
      when (moduleInfo.isFlat) {
        true -> builder.appendln(includeFlatModule(externalName, useKotlinDsl))
        else -> builder.appendln(includeModule(externalName, useKotlinDsl))
      }
      if (externalName != artifactId) {
        builder.appendln(renameModule(externalName, artifactId, useKotlinDsl))
      }
    }
    val settingsFileName = getSettingsFileName(useKotlinDsl)
    val settingsFile = File(rootModuleInfo.root.path, settingsFileName)
    assertFileContent(settingsFile, builder.toString())
  }

  private fun assertFileContent(file: File, content: String) {
    val expected = convertLineSeparators(file.readText().trim())
    val actual = convertLineSeparators(content.trim())
    assertEquals(expected, actual)
  }

  fun assertBuildScriptFiles(projectInfo: ProjectInfo) {
    for (module in projectInfo.modules + projectInfo.rootModule) {
      val buildFileName = getBuildFileName(module.useKotlinDsl)
      val buildFile = File(module.root.path, buildFileName)
      assertTrue(buildFile.exists())
    }
  }

  private fun getBuildFileName(useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """build.gradle.kts"""
      else -> """build.gradle"""
    }
  }

  private fun getSettingsFileName(useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """settings.gradle.kts"""
      else -> """settings.gradle"""
    }
  }

  private fun defineProject(name: String, useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """rootProject.name = "$name""""
      else -> """rootProject.name = '$name'"""
    }
  }

  private fun includeModule(name: String, useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """include("$name")"""
      else -> """include '$name'"""
    }
  }

  private fun includeFlatModule(name: String, useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """includeFlat("$name")"""
      else -> """includeFlat '$name'"""
    }
  }

  private fun renameModule(from: String, to: String, useKotlinDsl: Boolean): String {
    return when (useKotlinDsl) {
      true -> """findProject(":$from")?.name = "$to""""
      else -> """findProject(':$from')?.name = '$to'"""
    }
  }

  fun Project.use(save: Boolean = false, action: (Project) -> Unit) = utilUse(save, action)

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))


    /**
     * @param alsoWaitForPreview waits for double project reload (preview reload + project reload) if it is true
     * @param action or some async calls have to produce project reload
     *  for example invokeLater { refreshProject(project, spec) }
     * @throws java.lang.AssertionError if import is failed or isn't started
     */
    @JvmStatic
    fun <R> waitForProjectReload(alsoWaitForPreview: Boolean, action: ThrowableComputable<R, Throwable>): R {
      return waitForProjectReload(alsoWaitForPreview) { action.compute() }
    }


    fun <R> waitForProjectReload(alsoWaitForPreview: Boolean = false, action: () -> R): R {
      val projectReloadPromise = AsyncPromise<Any?>()
      val executionListenerDisposable = Disposer.newDisposable()
      val executionListener = object : ExternalSystemTaskNotificationListenerAdapter() {
        override fun onEnd(id: ExternalSystemTaskId) = Disposer.dispose(executionListenerDisposable)
        override fun onSuccess(id: ExternalSystemTaskId) {
          val project = id.findProject()!!
          if (alsoWaitForPreview) {
            getProjectDataServicesPromise(project).onProcessed {
              getProjectDataServicesPromise(project).onProcessed {
                projectReloadPromise.setResult(null)
              }
            }
          }
          else {
            getProjectDataServicesPromise(project).onProcessed {
              projectReloadPromise.setResult(null)
            }
          }
        }
      }
      ExternalSystemProgressNotificationManager.getInstance()
        .addNotificationListener(executionListener, executionListenerDisposable)
      val result = action()
      ApplicationManager.getApplication().invokeAndWait { PlatformTestUtil.waitForPromise(projectReloadPromise, TimeUnit.MINUTES.toMillis(1)) }
      return result
    }

    private fun getProjectDataServicesPromise(project: Project): AsyncPromise<Any?> {
      val promise = AsyncPromise<Any?>()
      val connection = project.messageBus.connect()
      connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener { promise.setResult(null) })
      return promise
    }
  }
}