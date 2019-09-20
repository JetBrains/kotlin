// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.JavaDependentSdkType
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.RootProvider
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.util.EnvironmentUtil
import com.intellij.util.SystemProperties
import com.intellij.util.ThrowableRunnable
import com.intellij.util.containers.MultiMap
import com.intellij.util.lang.JavaVersion
import org.assertj.core.api.Assertions.*
import org.jdom.Element
import org.junit.Test

import org.mockito.Mockito.*
import java.io.File

class ExternalSystemJdkUtilTest : UsefulTestCase() {

  lateinit var testFixture: IdeaProjectTestFixture
  lateinit var project: Project

  override fun setUp() {
    super.setUp()
    testFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name, true).fixture
    testFixture.setUp()
    project = testFixture.project
  }

  override fun tearDown() {
    RunAll(
      ThrowableRunnable { testFixture.tearDown() },
      ThrowableRunnable { super.tearDown() }
    ).run()
  }

  @Test
  fun testGetJdk() {
    assertThat(getJdk(project, null)).isNull()

    assertThat(getJdk(project, USE_INTERNAL_JAVA)?.homePath)
      .isEqualTo(FileUtil.toSystemIndependentName(SystemProperties.getJavaHome()))

    val javaHomeEnv = EnvironmentUtil.getValue("JAVA_HOME")?.let { FileUtil.toSystemIndependentName(it) }
    if (javaHomeEnv.isNullOrBlank()) {
      assertThrows<UndefinedJavaHomeException>(UndefinedJavaHomeException::class.java) { getJdk(project, USE_JAVA_HOME) }
    }
    else {
      assertThat(getJdk(project, USE_JAVA_HOME)?.homePath)
        .isEqualTo(javaHomeEnv)
    }

    val sdk = IdeaTestUtil.getMockJdk9()
    WriteAction.run<Throwable> {
      ProjectJdkTable.getInstance().addJdk(sdk, testFixture.testRootDisposable)
      ProjectRootManager.getInstance(project).projectSdk = sdk
    }

    assertThat(getJdk(project, USE_PROJECT_JDK))
      .isEqualTo(sdk)
  }

  @Test
  fun testResolveJdkName() {
    assertThat(resolveJdkName(null, null)).isNull()

    assertThat(resolveJdkName(null, USE_INTERNAL_JAVA)?.homePath)
      .isEqualTo(FileUtil.toSystemIndependentName(SystemProperties.getJavaHome()))

    val javaHomeEnv = EnvironmentUtil.getValue("JAVA_HOME")?.let { FileUtil.toSystemIndependentName(it) }
    if (javaHomeEnv.isNullOrBlank()) {
      assertThrows<UndefinedJavaHomeException>(UndefinedJavaHomeException::class.java) { resolveJdkName(null, USE_JAVA_HOME) }
    }
    else {
      assertThat(resolveJdkName(null, USE_JAVA_HOME)?.homePath)
        .isEqualTo(javaHomeEnv)
    }

    assertThrows<ProjectJdkNotFoundException>(ProjectJdkNotFoundException::class.java) {
      resolveJdkName(null, USE_PROJECT_JDK)
    }
    val sdk: Sdk = mock(Sdk::class.java)
    assertThat(resolveJdkName(sdk, USE_PROJECT_JDK))
      .isEqualTo(sdk)
  }

  @Test
  fun testGetAvailableJdkChoosesLatestSdk() {

    val sdk8 = createMockJdk(JavaVersion.compose(8))
    val sdk9 = createMockJdk(JavaVersion.compose(9))

    WriteAction.run<Throwable> {
      ProjectJdkTable.getInstance().addJdk(sdk8, testFixture.testRootDisposable)
      ProjectJdkTable.getInstance().addJdk(sdk9, testFixture.testRootDisposable)
    }

    assertThat(getAvailableJdk(project).second).isEqualTo(sdk9)
  }

  @Test
  fun testGetAvailableJdkPrefersProjectSDKDependency() {
    val sdk8 = createMockJdk(JavaVersion.compose(8))
    val sdk9 = createMockJdk(JavaVersion.compose(9))

    val dependentSDK = TestJavaDependentSdk(sdk8)

    WriteAction.run<Throwable> {
      with(ProjectJdkTable.getInstance()) {
        addJdk(sdk8, testFixture.testRootDisposable)
        addJdk(sdk9, testFixture.testRootDisposable)
        addJdk(dependentSDK, testFixture.testRootDisposable)
      }
      ProjectRootManager.getInstance(project).projectSdk = dependentSDK
    }

    assertThat(getAvailableJdk(project).second).isEqualTo(sdk8)
  }


  private fun createMockJdk(jdkVersion: JavaVersion): Sdk {
    val jdkVersionStr = jdkVersion.toString()
    val jdkDir = FileUtil.createTempDirectory(jdkVersionStr, null)
    listOf("bin/javac",
           "bin/java",
           "lib/rt.jar")
      .forEach {
        File(jdkDir, it).apply {
          parentFile.mkdirs()
          createNewFile()
          writeText("Fake")
        }
      }

    val path = jdkDir.absolutePath
    assertThat(isValidJdk(path)).`as`("Mock JDK at $path is expected to pass validation by com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.isValidJdk() " +
                                      "Please, check validation code and update mock accordingly").isTrue()
    return (JavaSdkImpl.getInstance() as JavaSdkImpl).createMockJdk(jdkVersionStr, path, false)
  }
}

class TestJavaDependentSdk(val sdk: Sdk) : MockSdk("TestJavaDependentSdk",
                                                   "fake/path",
                                                   "1.0",
                                                   MultiMap.empty<OrderRootType, VirtualFile>(),
                                                   TestJavaDependentSdkType.getInstance())

class TestJavaDependentSdkType(val myName: String): JavaDependentSdkType(myName) {
  companion object {
    private val instance = TestJavaDependentSdkType("TestSdkType")
    fun getInstance(): TestJavaDependentSdkType = instance
  }

  override fun suggestHomePath(): String? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isValidSdkHome(path: String?): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun suggestSdkName(currentSdkName: String?, sdkHome: String?): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getPresentableName(): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getBinPath(sdk: Sdk): String {
    return (sdk as? TestJavaDependentSdk)?.let { JavaSdk.getInstance().getBinPath(it.sdk) }!!
  }

  override fun getToolsPath(sdk: Sdk): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getVMExecutablePath(sdk: Sdk): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}