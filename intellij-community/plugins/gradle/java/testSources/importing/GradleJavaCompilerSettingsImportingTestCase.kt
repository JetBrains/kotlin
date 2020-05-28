// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.tooling.builder.AbstractModelBuilderTest
import org.junit.runners.Parameterized

abstract class GradleJavaCompilerSettingsImportingTestCase : GradleJavaImportingTestCase() {

  var isNotSupportedJava14: Boolean = false
    private set

  override fun setUp() {
    super.setUp()
    isNotSupportedJava14 = isGradleOlderThen("6.3")
  }

  fun createGradleSettingsFile(vararg moduleNames: String) {
    createSettingsFile(
      GroovyBuilder.generate {
        property("rootProject.name", "'project'")
        for (moduleName in moduleNames) {
          call("include", "'$moduleName'")
        }
      }
    )
  }

  fun createJavaGradleSubProject(
    relativePath: String = ".",
    projectSourceCompatibility: String? = null,
    projectTargetCompatibility: String? = null,
    mainSourceCompatibility: String? = null,
    mainSourceCompatibilityEnablePreview: Boolean = false,
    mainTargetCompatibility: String? = null,
    testSourceCompatibility: String? = null,
    testSourceCompatibilityEnablePreview: Boolean = false,
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
        if (mainSourceCompatibilityEnablePreview) {
          call("options.compilerArgs.add", "'--enable-preview'")
        }
      }
      .withTaskConfiguration("compileTestJava") {
        propertyIfNotNull("sourceCompatibility", testSourceCompatibility)
        propertyIfNotNull("targetCompatibility", testTargetCompatibility)
        if (testSourceCompatibilityEnablePreview) {
          call("options.compilerArgs.add", "'--enable-preview'")
        }
      }
      .generate()
    createProjectSubDir("$relativePath/src/main/java")
    createProjectSubDir("$relativePath/src/test/java")
    return createProjectSubFile("$relativePath/build.gradle", buildScript)
  }

  companion object {
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests() = arrayListOf(*AbstractModelBuilderTest.SUPPORTED_GRADLE_VERSIONS, arrayOf("6.3"))
  }
}