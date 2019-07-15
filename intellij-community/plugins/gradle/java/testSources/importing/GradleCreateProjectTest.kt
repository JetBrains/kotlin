// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.externalSystem.util.use
import org.junit.Test

class GradleCreateProjectTest : GradleCreateProjectTestCase() {

  @Test
  fun `test project re-create`() {
    val projectInfo = generateProjectInfo("A")
    createProject(projectInfo).use(save = true) {
      assertProjectStructure(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
    deleteProject(projectInfo)
    createProject(projectInfo).use {
      assertProjectStructure(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }

  @Test
  fun `test project re-create forcibly`() {
    val projectInfo = generateProjectInfo("A")
    createProject(projectInfo).use(save = true) {
      assertProjectStructure(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
    createProject(projectInfo).use {
      assertProjectStructure(it, projectInfo)
      assertDefaultProjectSettings(it)
    }
  }
}