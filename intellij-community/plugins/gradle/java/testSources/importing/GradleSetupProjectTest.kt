// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.action.AttachExternalProjectAction
import com.intellij.openapi.externalSystem.importing.ExternalSystemSetupProjectTest
import com.intellij.openapi.externalSystem.importing.ExternalSystemSetupProjectTestCase
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.gradle.action.ImportProjectFromScriptAction
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.junit.runners.Parameterized

class GradleSetupProjectTest : ExternalSystemSetupProjectTest, GradleImportingTestCase() {
  override fun getSystemId(): ProjectSystemId = SYSTEM_ID

  override fun generateProject(id: String): ExternalSystemSetupProjectTestCase.ProjectInfo {
    val name = "${System.currentTimeMillis()}-$id"
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
    return ExternalSystemSetupProjectTestCase.ProjectInfo(projectFile,
                                                          "$name-project", "$name-project.main", "$name-project.test",
                                                          "$name-project.module", "$name-project.module.main", "$name-project.module.test",
                                                          "$name-project.$name-module", "$name-project.$name-module.main",
                                                          "$name-project.$name-module.test",
                                                          "$name-composite", "$name-composite.main", "$name-composite.test")
  }

  override fun assertDefaultProjectSettings(project: Project) {
    val externalProjectPath = project.basePath!!
    val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID) as GradleSettings
    val projectSettings = settings.getLinkedProjectSettings(externalProjectPath)!!
    assertEquals(projectSettings.externalProjectPath, externalProjectPath)
    assertEquals(projectSettings.isUseQualifiedModuleNames, true)
    assertEquals(settings.storeProjectFilesExternally, true)
  }

  override fun doAttachProject(project: Project, projectFile: VirtualFile) {
    AttachExternalProjectAction().perform(project, selectedFile = projectFile)
  }

  override fun doAttachProjectFromScript(project: Project, projectFile: VirtualFile) {
    ImportProjectFromScriptAction().perform(project, selectedFile = projectFile)
  }

  override fun waitForImportCompletion(project: Project) {
    ApplicationManager.getApplication().invokeAndWait { PlatformTestUtil.dispatchAllEventsInIdeEventQueue() }
  }

  override fun cleanupProjectTestResources(project: Project) {
    super.cleanupProjectTestResources(project)
    removeGradleJvmSdk(project)
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(BASE_GRADLE_VERSION))

    fun removeGradleJvmSdk(project: Project) {
      ApplicationManager.getApplication().invokeAndWait {
        runWriteAction {
          val projectJdkTable = ProjectJdkTable.getInstance()
          val settings = GradleSettings.getInstance(project)
          for (projectSettings in settings.linkedProjectsSettings) {
            val gradleJvm = projectSettings.gradleJvm
            val sdk = ExternalSystemJdkUtil.getJdk(project, gradleJvm)
            if (sdk != null) projectJdkTable.removeJdk(sdk)
          }
        }
      }
    }
  }
}