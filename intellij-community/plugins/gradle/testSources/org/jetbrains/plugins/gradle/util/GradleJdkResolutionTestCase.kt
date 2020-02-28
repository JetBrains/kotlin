// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkTestCase
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.replaceService
import com.intellij.util.SystemProperties
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap

abstract class GradleJdkResolutionTestCase : SdkTestCase() {

  val environment get() = Environment.getInstance() as TestEnvironment

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

    val application = ApplicationManager.getApplication()
    application.replaceService(Environment::class.java, TestEnvironment(), testRootDisposable)
    application.replaceService(ExternalSystemJdkProvider::class.java, TestJdkProvider(), testRootDisposable)

    externalProjectPath = createUniqueTempDirectory()
    gradleUserHome = createUniqueTempDirectory()
    userHome = createUniqueTempDirectory()
    userCache = FileUtil.join(userHome, GRADLE_CACHE_DIR_NAME)

    gradleVersion = GradleVersion.version("5.2.1")

    earliestSdk = TestSdkGenerator.createNextSdk("1.8")
    latestSdk = TestSdkGenerator.createNextSdk("11")
    unsupportedSdk = TestSdkGenerator.createNextSdk("13")

    environment.properties(Environment.USER_HOME to null)
    environment.variables(JAVA_HOME to null, GradleConstants.SYSTEM_DIRECTORY_PATH_KEY to null)
  }

  override fun tearDown() {
    closeAndDeleteProject()
    super.tearDown()
  }

  inner class TestJdkProvider : ExternalSystemJdkProvider {
    override fun getJavaSdkType() = TestSdkType

    override fun getInternalJdk(): Sdk {
      val jdkHome = SystemProperties.getJavaHome()
      return createJdk(null, jdkHome)
    }

    override fun createJdk(jdkName: String?, homePath: String): Sdk {
      val sdk = TestSdkGenerator.findTestSdk(homePath)!!
      Disposer.register(testRootDisposable, Disposable { removeSdk(sdk) })
      return sdk
    }
  }

  class TestEnvironment : Environment {
    private val properties = LinkedHashMap<String, String?>()
    private val variables = LinkedHashMap<String, String?>()
    private val previousEnvironment = Environment.getInstance()

    fun properties(vararg properties: Pair<String, String?>) {
      this.properties.putAll(properties)
    }

    fun variables(vararg variables: Pair<String, String?>) {
      this.variables.putAll(variables)
    }

    fun <R> withVariables(vararg variables: Pair<String, String?>, action: () -> R): R {
      val environment = TestEnvironment()
      environment.variables(*variables)
      val application = ApplicationManager.getApplication()
      val parentDisposable = Disposer.newDisposable("Test Environment")
      application.replaceService(Environment::class.java, environment, parentDisposable)
      try {
        return action()
      }
      finally {
        Disposer.dispose(parentDisposable)
      }
    }

    override fun getProperty(name: String): String? {
      return when (name) {
        in properties -> properties[name]
        else -> previousEnvironment.getProperty(name)
      }
    }

    override fun getVariable(name: String): String? {
      return when (name) {
        in variables -> variables[name]
        else -> previousEnvironment.getVariable(name)
      }
    }
  }

  private fun createUniqueTempDirectory(): String {
    val path = FileUtil.join(FileUtil.getTempDirectory(), UUID.randomUUID().toString())
    FileUtil.createDirectory(File(path))
    return path
  }

  fun withRegisteredSdks(vararg sdks: TestSdk, action: () -> Unit) {
    registerSdks(*sdks)
    try {
      action()
    }
    finally {
      removeSdks(*sdks)
    }
  }

  fun assertGradleJvmSuggestion(expected: TestSdk, projectSdk: TestSdk? = null, expectsSdkRegistration: Boolean = false) {
    val newSdks = detectRegisteredSdks {
      assertEquals(expected.name, suggestGradleJvm(project, projectSdk, externalProjectPath, gradleVersion))
    }
    if (expectsSdkRegistration) {
      assertTrue("Expected registration of $expected but found $newSdks", newSdks.size == 1)
      val newSdk = newSdks.first()
      assertSdk(expected, newSdk)
      removeSdk(newSdk)
    }
    else {
      assertTrue("Unexpected sdk registration $newSdks", newSdks.isEmpty())
    }
  }

  fun assertGradleJvmSuggestion(expected: String, projectSdk: TestSdk? = null) {
    val newSdks = detectRegisteredSdks {
      assertEquals(expected, suggestGradleJvm(project, projectSdk, externalProjectPath, gradleVersion))
    }
    assertTrue("Unexpected sdk registration $newSdks", newSdks.isEmpty())
  }

  private fun detectRegisteredSdks(action: () -> Unit): List<TestSdk> {
    val projectSdkTable = ProjectJdkTable.getInstance()
    val beforeSdks = projectSdkTable.allJdks.toSet()
    action()
    val afterSdks = projectSdkTable.allJdks.toSet()
    val newSdks = afterSdks - beforeSdks
    return newSdks.map { it as TestSdk }
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
    val actualProperties = getGradleProperties(externalProjectPath)
    assertEquals(java?.homePath, actualProperties.javaHomeProperty?.value)
  }

  fun withGradleProperties(parentDirectory: String, java: TestSdk?, action: () -> Unit) {
    val propertiesPath = FileUtil.join(parentDirectory, PROPERTIES_FILE_NAME)
    createProperties(propertiesPath) {
      java?.let { setProperty(GRADLE_JAVA_HOME_PROPERTY, it.homePath) }
    }
    action()
    FileUtil.delete(File(propertiesPath))
  }

  fun createProperties(propertiesPath: String, configure: Properties.() -> Unit) {
    val propertiesFile = File(propertiesPath)
    FileUtil.createIfNotExists(propertiesFile)
    propertiesFile.outputStream().use {
      val properties = Properties()
      properties.configure()
      properties.store(it, null)
    }
  }
}