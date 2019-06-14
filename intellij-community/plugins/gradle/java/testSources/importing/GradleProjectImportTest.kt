// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.junit.Test

class GradleProjectImportTest : GradleProjectImportTestCase() {

  @Test
  fun `test project open`() {
    val projectInfo = generateGradleProject("gradle")
    openProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test project import`() {
    val projectInfo = generateGradleProject("gradle")
    importProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test project attach`() {
    val projectInfo = generateGradleProject("gradle")
    attachNewProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test project import from script`() {
    val projectInfo = generateGradleProject("gradle")
    attachNewProjectFromScript(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test module attach`() {
    val projectInfo = generateGradleProject("gradle")
    val linkedProjectInfo = generateGradleProject("linked")
    openProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      attachProject(it, linkedProjectInfo.projectFile)
      assertModules(it, projectInfo, linkedProjectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test project re-open`() {
    val projectInfo = generateGradleProject("gradle")
    val linkedProjectInfo = generateGradleProject("linked")
    println(projectInfo.projectFile)
    openProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo)
      attachProject(it, linkedProjectInfo.projectFile)
      assertModules(it, projectInfo, linkedProjectInfo)
      assertDefaultProjectSettings(it)
    }
    openProject(projectInfo.projectFile).use {
      assertModules(it, projectInfo, linkedProjectInfo)
      assertDefaultProjectSettings(it)
    }
  }
}