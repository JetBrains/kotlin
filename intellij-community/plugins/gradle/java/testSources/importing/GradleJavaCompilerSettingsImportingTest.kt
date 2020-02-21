// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.RunAll
import com.intellij.util.ThrowableRunnable
import org.junit.Test

class GradleJavaCompilerSettingsImportingTest : GradleJavaImportingTestCase() {

  private lateinit var sdk: Sdk

  /**
   * @see org.gradle.tooling.model.idea.IdeaJavaLanguageSettings
   */
  private var isNotSupportedIdeaJavaLanguageSettings: Boolean = false

  override fun setUp() {
    super.setUp()
    sdk = createAndRegisterSdk()
    isNotSupportedIdeaJavaLanguageSettings = isGradleOlderThen("2.11")
  }

  override fun tearDown() {
    RunAll(
      ThrowableRunnable { if (::sdk.isInitialized) removeSdk(sdk) },
      ThrowableRunnable { super.tearDown() }
    ).run()
  }

  @Test
  fun `test project-module sdk replacing`() {
    createJavaGradleSubProject()
    importProject()
    assertSdks(GRADLE_JDK_NAME, "project", "project.main", "project.test")

    setProjectSdk(sdk)
    assertSdks(sdk.name, "project", "project.main", "project.test")

    importProject()
    assertSdks(sdk.name, "project", "project.main", "project.test")
  }

  @Test
  fun `test project-module compatibility replacing`() {
    if (isNotSupportedIdeaJavaLanguageSettings) return

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

    assertModuleLanguageLevel("project", LanguageLevel.JDK_1_3)
    assertModuleLanguageLevel("project.main", LanguageLevel.JDK_1_5)
    assertModuleLanguageLevel("project.test", LanguageLevel.JDK_1_7)
    assertModuleLanguageLevel("project.module", LanguageLevel.JDK_1_8)
    assertModuleLanguageLevel("project.module.main", LanguageLevel.JDK_1_6)
    assertModuleLanguageLevel("project.module.test", LanguageLevel.JDK_1_4)

    assertModuleTargetBytecodeVersion("project", "1.4")
    assertModuleTargetBytecodeVersion("project.main", "1.6")
    assertModuleTargetBytecodeVersion("project.test", "1.8")
    assertModuleTargetBytecodeVersion("project.module", "1.7")
    assertModuleTargetBytecodeVersion("project.module.main", "1.5")
    assertModuleTargetBytecodeVersion("project.module.test", "1.3")
  }

  private fun createGradleSettingsFile(vararg moduleNames: String) {
    createSettingsFile(
      GroovyBuilder.generate {
        property("rootProject.name", "'project'")
        for (moduleName in moduleNames) {
          call("include", "'$moduleName'")
        }
      }
    )
  }

  private fun createJavaGradleSubProject(
    relativePath: String = ".",
    projectSourceCompatibility: String? = null,
    projectTargetCompatibility: String? = null,
    mainSourceCompatibility: String? = null,
    mainTargetCompatibility: String? = null,
    testSourceCompatibility: String? = null,
    testTargetCompatibility: String? = null
  ): VirtualFile {
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withPrefix {
        propertyIfNotNull("sourceCompatibility", projectSourceCompatibility)
        propertyIfNotNull("targetCompatibility", projectTargetCompatibility)
      }
      .withTaskConfiguration("compileJava") {
        propertyIfNotNull("sourceCompatibility", mainSourceCompatibility)
        propertyIfNotNull("targetCompatibility", mainTargetCompatibility)
      }
      .withTaskConfiguration("compileTestJava") {
        propertyIfNotNull("sourceCompatibility", testSourceCompatibility)
        propertyIfNotNull("targetCompatibility", testTargetCompatibility)
      }
      .generate()
    createProjectSubDir("$relativePath/src/main/java")
    createProjectSubDir("$relativePath/src/test/java")
    return createProjectSubFile("$relativePath/build.gradle", buildScript)
  }
}