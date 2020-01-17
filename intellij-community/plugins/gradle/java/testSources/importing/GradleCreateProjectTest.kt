// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import org.junit.Test

class GradleCreateProjectTest : GradleCreateProjectTestCase() {

  @Test
  fun `test project re-create`() {
    val projectInfo = projectInfo("A") {
      moduleInfo("$name-project", "$name-project")
      moduleInfo("$name-project.module", "$name-project/module")
      moduleInfo("$name-project.$name-module", "$name-module")
    }
    withProject(projectInfo, save = true) {
      assertProjectStructure(projectInfo)
      assertDefaultProjectSettings()
    }
    deleteProject(projectInfo)
    withProject(projectInfo) {
      assertProjectStructure(projectInfo)
      assertDefaultProjectSettings()
    }
  }

  @Test
  fun `test project re-create forcibly`() {
    val projectInfo = projectInfo("A") {
      moduleInfo("$name-project", "$name-project")
      moduleInfo("$name-project.module", "$name-project/module")
      moduleInfo("$name-project.$name-module", "$name-module")
    }
    withProject(projectInfo, save = true) {
      assertProjectStructure(projectInfo)
      assertDefaultProjectSettings()
    }
    withProject(projectInfo) {
      assertProjectStructure(projectInfo)
      assertDefaultProjectSettings()
    }
  }

  @Test
  fun `test project groovy setting generation`() {
    val projectInfo = projectInfo("A", useKotlinDsl = false) {
      moduleInfo("$name-project", "$name-project")
      moduleInfo("$name-project.module", "$name-project/module")
      moduleInfo("$name-project.$name-module", "$name-module")
      moduleInfo("$name-project.module", "$name-project/module", "module-id")
    }
    withProject(projectInfo) {
      assertSettingsFileContent(projectInfo)
      assertBuildScriptFiles(projectInfo)
    }
  }

  @Test
  fun `test project kotlin dsl setting generation`() {
    val projectInfo = projectInfo("A", useKotlinDsl = true) {
      moduleInfo("$name-project", "$name-project")
      moduleInfo("$name-project.module", "$name-project/module")
      moduleInfo("$name-project.$name-module", "$name-module")
      moduleInfo("$name-project.module", "$name-project/module", "module-id")
    }
    withProject(projectInfo) {
      assertSettingsFileContent(projectInfo)
      assertBuildScriptFiles(projectInfo)
    }
  }

  @Test
  fun `test project groovy setting generation with groovy-kotlin scripts`() {
    val projectInfo = projectInfo("A") {
      moduleInfo("$name-project", "$name-project", useKotlinDsl = false)
      moduleInfo("$name-project.module1", "$name-project/module1", useKotlinDsl = true)
      moduleInfo("$name-project.module2", "$name-project/module2", useKotlinDsl = false)
    }
    withProject(projectInfo) {
      assertSettingsFileContent(projectInfo)
      assertBuildScriptFiles(projectInfo)
    }
  }

  @Test
  fun `test project kotlin dsl setting generation with groovy-kotlin scripts`() {
    val projectInfo = projectInfo("A") {
      moduleInfo("$name-project", "$name-project", useKotlinDsl = true)
      moduleInfo("$name-project.module1", "$name-project/module1", useKotlinDsl = true)
      moduleInfo("$name-project.module2", "$name-project/module2", useKotlinDsl = false)
    }
    withProject(projectInfo) {
      assertSettingsFileContent(projectInfo)
      assertBuildScriptFiles(projectInfo)
    }
  }
}