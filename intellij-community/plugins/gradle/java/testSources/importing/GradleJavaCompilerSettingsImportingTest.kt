// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.pom.java.LanguageLevel
import org.junit.Test

class GradleJavaCompilerSettingsImportingTest : GradleJavaCompilerSettingsImportingTestCase() {
  @Test
  fun `test project-module compatibility replacing`() {
    createJavaGradleSubProject(
      projectSourceCompatibility = "1.6",
      projectTargetCompatibility = "1.7"
    )
    importProject()
    assertLanguageLevels(LanguageLevel.JDK_1_6, "project", "project.main", "project.test")
    assertTargetBytecodeVersions("1.7", "project", "project.main", "project.test")

    setProjectLanguageLevel(LanguageLevel.JDK_13)
    setProjectTargetBytecodeVersion("13")
    assertLanguageLevels(LanguageLevel.JDK_13, "project", "project.main", "project.test")
    assertTargetBytecodeVersions("13", "project", "project.main", "project.test")

    importProject()
    assertLanguageLevels(LanguageLevel.JDK_1_6, "project", "project.main", "project.test")
    assertTargetBytecodeVersions("1.7", "project", "project.main", "project.test")
  }

  @Test
  fun `test maximum compiler settings dispersion`() {
    createJavaGradleSubProject(
      projectSourceCompatibility = "1.3",
      projectTargetCompatibility = "1.4",
      mainSourceCompatibility = "1.5",
      mainTargetCompatibility = "1.6",
      testSourceCompatibility = "1.7",
      testTargetCompatibility = "1.8"
    )
    createJavaGradleSubProject(
      "module",
      projectSourceCompatibility = "1.8",
      projectTargetCompatibility = "1.7",
      mainSourceCompatibility = "1.6",
      mainTargetCompatibility = "1.5",
      testSourceCompatibility = "1.4",
      testTargetCompatibility = "1.3"
    )
    createGradleSettingsFile("module")
    importProject()

    assertProjectLanguageLevel(LanguageLevel.JDK_1_3)
    assertModuleLanguageLevel("project", LanguageLevel.JDK_1_3)
    assertModuleLanguageLevel("project.main", LanguageLevel.JDK_1_5)
    assertModuleLanguageLevel("project.test", LanguageLevel.JDK_1_7)
    assertModuleLanguageLevel("project.module", LanguageLevel.JDK_1_8)
    assertModuleLanguageLevel("project.module.main", LanguageLevel.JDK_1_6)
    assertModuleLanguageLevel("project.module.test", LanguageLevel.JDK_1_4)

    assertProjectTargetBytecodeVersion("1.4")
    assertModuleTargetBytecodeVersion("project", "1.4")
    assertModuleTargetBytecodeVersion("project.main", "1.6")
    assertModuleTargetBytecodeVersion("project.test", "1.8")
    assertModuleTargetBytecodeVersion("project.module", "1.7")
    assertModuleTargetBytecodeVersion("project.module.main", "1.5")
    assertModuleTargetBytecodeVersion("project.module.test", "1.3")
  }

  @Test
  fun `test language level approximation`() {
    if (isNotSupportedJava14) return

    createJavaGradleSubProject(
      projectSourceCompatibility = "14",
      mainSourceCompatibilityEnablePreview = true,
      testSourceCompatibilityEnablePreview = true
    )
    importProject()
    assertProjectLanguageLevel(LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.test", LanguageLevel.JDK_14_PREVIEW)

    createJavaGradleSubProject(
      "module",
      projectSourceCompatibility = "14",
      mainSourceCompatibilityEnablePreview = true,
      testSourceCompatibilityEnablePreview = true
    )
    createGradleSettingsFile("module")
    importProject()
    assertProjectLanguageLevel(LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.test", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module.test", LanguageLevel.JDK_14_PREVIEW)

    createJavaGradleSubProject(
      "module1",
      projectSourceCompatibility = "14",
      mainSourceCompatibilityEnablePreview = true,
      testSourceCompatibilityEnablePreview = false
    )
    createJavaGradleSubProject(
      "module2",
      projectSourceCompatibility = "14",
      mainSourceCompatibilityEnablePreview = false,
      testSourceCompatibilityEnablePreview = true
    )
    createJavaGradleSubProject(
      "module3",
      projectSourceCompatibility = "14",
      mainSourceCompatibilityEnablePreview = false,
      testSourceCompatibilityEnablePreview = false
    )
    createGradleSettingsFile("module", "module1", "module2", "module3", "module4")
    importProject()
    assertProjectLanguageLevel(LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.test", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module.test", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module1", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module1.main", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module1.test", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module2", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module2.main", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module2.test", LanguageLevel.JDK_14_PREVIEW)
    assertModuleLanguageLevel("project.module3", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module3.main", LanguageLevel.JDK_14)
    assertModuleLanguageLevel("project.module3.test", LanguageLevel.JDK_14)
  }
}