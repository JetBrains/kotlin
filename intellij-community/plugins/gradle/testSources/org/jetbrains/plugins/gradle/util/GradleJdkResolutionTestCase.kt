// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkNonblockingUtilTestCase.Companion.waitForLookup
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtilTestCase
import com.intellij.openapi.externalSystem.service.execution.nonblockingResolveSdkBySdkName
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupProviderImpl
import com.intellij.openapi.util.io.FileUtil
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import java.io.File
import java.util.*

abstract class GradleJdkResolutionTestCase : ExternalSystemJdkUtilTestCase() {

  lateinit var externalProjectPath: String
  lateinit var gradleUserHome: String
  lateinit var userHome: String
  lateinit var userCache: String

  lateinit var gradleVersion: GradleVersion

  lateinit var earliestSdk: TestSdk
  lateinit var latestSdk: TestSdk
  lateinit var unsupportedSdk: TestSdk

  override fun setUp() {
    super.setUp()

    externalProjectPath = createUniqueTempDirectory()
    gradleUserHome = createUniqueTempDirectory()
    userHome = createUniqueTempDirectory()
    userCache = FileUtil.join(userHome, GRADLE_CACHE_DIR_NAME)

    gradleVersion = GradleVersion.version("5.2.1")

    earliestSdk = TestSdkGenerator.createNextSdk("1.8")
    latestSdk = TestSdkGenerator.createNextSdk("11")
    unsupportedSdk = TestSdkGenerator.createNextSdk("13")

    environment.properties(USER_HOME to null)
    environment.variables(GradleConstants.SYSTEM_DIRECTORY_PATH_KEY to null)
  }

  fun assertGradleJvmSuggestion(expected: TestSdk, expectsSdkRegistration: Boolean = false) {
    assertGradleJvmSuggestion({ expected }, expectsSdkRegistration)
  }

  fun assertGradleJvmSuggestion(expected: () -> TestSdk, expectsSdkRegistration: Boolean = false) {
    assertNewlyRegisteredSdks({ if (expectsSdkRegistration) expected() else null }) {
      val gradleJvm = suggestGradleJvm(project, externalProjectPath, gradleVersion)
      val gradleJdk = SdkLookupProviderImpl().nonblockingResolveSdkBySdkName(gradleJvm)
      requireNotNull(gradleJdk) { "expected: ${expected()}" }
      assertSdk(expected(), gradleJdk)
    }
  }

  fun assertGradleJvmSuggestion(expected: String) {
    assertUnexpectedSdksRegistration {
      val gradleJvm = suggestGradleJvm(project, externalProjectPath, gradleVersion)
      assertEquals(expected, gradleJvm)
    }
  }

  private fun suggestGradleJvm(project: Project, externalProjectPath: String, gradleVersion: GradleVersion): String? {
    val projectSettings = GradleProjectSettings()
    projectSettings.externalProjectPath = externalProjectPath
    setupGradleJvm(project, projectSettings, gradleVersion)
    val provider = getGradleJvmLookupProvider(project, projectSettings)
    provider.waitForLookup()
    return projectSettings.gradleJvm
  }

  fun withGradleLinkedProject(java: TestSdk?, action: () -> Unit) {
    val settings = GradleSettings.getInstance(project)
    val externalProjectPath = createUniqueTempDirectory()
    val projectSettings = GradleProjectSettings().apply {
      this.externalProjectPath = externalProjectPath
      this.gradleJvm = java?.name
    }
    settings.linkProject(projectSettings)
    try {
      action()
    }
    finally {
      settings.unlinkExternalProject(externalProjectPath)
    }
  }

  fun assertGradleProperties(java: TestSdk?) {
    assertEquals(java?.homePath, getGradleJavaHome(externalProjectPath))
  }

  fun assertSuggestedGradleVersionFor(gradleVersionString: String?, javaVersionString: String) {
    val gradleVersion = gradleVersionString?.let(GradleVersion::version)
    val testJdk = TestSdkGenerator.createNextSdk(javaVersionString)
    withRegisteredSdk(testJdk, isProjectSdk = true) {
      val actualGradleVersion = suggestGradleVersion(project)
      assertEquals("Suggested incorrect grade version for $testJdk", gradleVersion, actualGradleVersion)
      if (actualGradleVersion == null) return@withRegisteredSdk
      val isSupported = isSupported(actualGradleVersion, javaVersionString)
      assertTrue("Suggested incompatible gradle version $actualGradleVersion for $testJdk", isSupported)
    }
  }

  fun withServiceGradleUserHome(action: () -> Unit) {
    val systemSettings = GradleSystemSettings.getInstance()
    val serviceDirectoryPath = systemSettings.serviceDirectoryPath
    systemSettings.serviceDirectoryPath = gradleUserHome
    try {
      action()
    } finally {
      systemSettings.serviceDirectoryPath = serviceDirectoryPath
    }
  }

  companion object {
    fun createUniqueTempDirectory(): String {
      val path = FileUtil.join(FileUtil.getTempDirectory(), UUID.randomUUID().toString())
      FileUtil.createDirectory(File(path))
      return path
    }

    fun withGradleProperties(parentDirectory: String, java: TestSdk?, action: () -> Unit) {
      val propertiesPath = FileUtil.join(parentDirectory, PROPERTIES_FILE_NAME)
      createProperties(propertiesPath) {
        java?.let { setProperty(GRADLE_JAVA_HOME_PROPERTY, it.homePath) }
      }
      action()
      FileUtil.delete(File(propertiesPath))
    }

    private fun createProperties(propertiesPath: String, configure: Properties.() -> Unit) {
      val propertiesFile = File(propertiesPath)
      FileUtil.createIfNotExists(propertiesFile)
      propertiesFile.outputStream().use {
        val properties = Properties()
        properties.configure()
        properties.store(it, null)
      }
    }
  }
}