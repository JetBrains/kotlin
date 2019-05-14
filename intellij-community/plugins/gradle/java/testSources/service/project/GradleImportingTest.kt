// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import com.intellij.ide.actions.ImportModuleAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.junit.Test
import org.junit.runners.Parameterized

class GradleImportingTest : GradleImportingTestCase() {
  @Test
  fun `test project import`() = `test project setup`(::importProject)

  @Test
  fun `test project open`() = `test project setup`(::openProject)

  private fun `test project setup`(createAndSetupProject: (VirtualFile) -> Project) {
    val (projectFile, linkedProjectFile) = generateGradleProject()
    createAndSetupProject(projectFile).use { project ->
      assertModules(project, "gradle-project", "gradle-project.main", "gradle-project.test",
                    "gradle-project.module", "gradle-project.module.main", "gradle-project.module.test",
                    "gradle-project.gradle-module", "gradle-project.gradle-module.main", "gradle-project.gradle-module.test",
                    "gradle-composite", "gradle-composite.main", "gradle-composite.test")
      linkProject(project, linkedProjectFile)
      assertModules(project, "gradle-project", "gradle-project.main", "gradle-project.test",
                    "gradle-project.module", "gradle-project.module.main", "gradle-project.module.test",
                    "gradle-project.gradle-module", "gradle-project.gradle-module.main", "gradle-project.gradle-module.test",
                    "gradle-composite", "gradle-composite.main", "gradle-composite.test",
                    "gradle-linked", "gradle-linked.main", "gradle-linked.test")
    }

  }

  private fun Project.use(action: (Project) -> Unit) {
    val project = this@use
    try {
      action(project)
    }
    finally {
      invokeAndWaitIfNeeded {
        val projectManager = ProjectManagerEx.getInstanceEx()
        projectManager.forceCloseProject(project, true)
      }
    }
  }

  private fun generateGradleProject(): Pair<VirtualFile, VirtualFile> {
    createProjectSubFile("gradle-linked/settings.gradle", "rootProject.name = 'gradle-linked'")
    createProjectSubFile("gradle-composite/settings.gradle", "rootProject.name = 'gradle-composite'")
    createProjectSubFile("gradle-project/settings.gradle", """
      rootProject.name = 'gradle-project'
      include 'module'
      includeBuild '../gradle-composite'
      includeFlat 'gradle-module'
    """.trimIndent())
    val buildScript = GradleBuildScriptBuilderEx().withJavaPlugin().generate()
    createProjectSubFile("gradle-composite/build.gradle", buildScript)
    createProjectSubFile("gradle-module/build.gradle", buildScript)
    createProjectSubFile("gradle-project/module/build.gradle", buildScript)
    val linkedProjectFile = createProjectSubFile("gradle-linked/build.gradle", buildScript)
    val projectFile = createProjectSubFile("gradle-project/build.gradle", buildScript)
    return projectFile to linkedProjectFile
  }

  private fun openProject(projectFile: VirtualFile): Project {
    return invokeAndWaitIfNeeded {
      val provider = ProjectOpenProcessor.getImportProvider(projectFile)!!
      provider.doOpenProject(projectFile, null, true)!!
    }
  }

  private fun importProject(projectFile: VirtualFile): Project {
    return importProject(null, projectFile)!!
  }

  private fun linkProject(project: Project, projectFile: VirtualFile) {
    assertNull(importProject(project, projectFile))
  }

  private fun importProject(project: Project?, projectFile: VirtualFile): Project? {
    return invokeAndWaitIfNeeded {
      val projectManager = ProjectManager.getInstance()
      val openProjects = projectManager.openProjects.map { it.name }.toSet()
      val providers = ImportModuleAction.getProviders(project).toTypedArray()
      val wizard = ImportModuleAction.createImportWizard(project, null, projectFile, *providers)!!
      ImportModuleAction.createFromWizard(project, wizard)
      wizard.disposeIfNeeded()
      projectManager.openProjects.find { it.name !in openProjects }
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