// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.DependentSdkType
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.containers.MultiMap
import org.jdom.Element
import java.io.File
import java.util.*
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.collections.LinkedHashMap

abstract class SdkTestCase : LightPlatformTestCase() {

  val projectSdk get() = ProjectRootManager.getInstance(project).projectSdk

  override fun setUp() {
    super.setUp()

    TestSdkGenerator.reset()
    SdkType.EP_NAME.point.registerExtension(TestSdkType, testRootDisposable)
    SdkType.EP_NAME.point.registerExtension(DependentTestSdkType, testRootDisposable)
    SdkDownload.EP_NAME.point.registerExtension(TestSdkDownloader, testRootDisposable)
  }

  fun createAndRegisterSdk(isProjectSdk: Boolean = false): TestSdk {
    val sdk = TestSdkGenerator.createNextSdk()
    registerSdk(sdk, isProjectSdk)
    return sdk
  }

  fun createAndRegisterDependentSdk(isProjectSdk: Boolean = false): DependentTestSdk {
    val sdk = TestSdkGenerator.createNextDependentSdk()
    registerSdk(sdk.parent)
    registerSdk(sdk, isProjectSdk)
    return sdk
  }

  private fun registerSdk(sdk: TestSdk, isProjectSdk: Boolean = false) {
    registerSdk(sdk, testRootDisposable)
    if (isProjectSdk) {
      setProjectSdk(sdk)
    }
  }

  fun registerSdks(vararg sdks: TestSdk) {
    registerSdks(*sdks, parentDisposable = testRootDisposable)
  }

  private fun setProjectSdk(sdk: Sdk?) {
    invokeAndWaitIfNeeded {
      runWriteAction {
        val rootManager = ProjectRootManager.getInstance(project)
        rootManager.projectSdk = sdk
      }
    }
  }

  fun withProjectSdk(sdk: TestSdk, action: () -> Unit) {
    val projectSdk = projectSdk
    setProjectSdk(sdk)
    try {
      action()
    }
    finally {
      setProjectSdk(projectSdk)
    }
  }

  fun withRegisteredSdk(sdk: TestSdk, isProjectSdk: Boolean = false, action: () -> Unit) {
    withRegisteredSdks(sdk) {
      when (isProjectSdk) {
        true -> withProjectSdk(sdk, action)
        else -> action()
      }
    }
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

  interface TestSdkType : JavaSdkType, SdkTypeId {
    companion object : SdkType("test-type"), TestSdkType {
      override fun getPresentableName(): String = name
      override fun isValidSdkHome(path: String?): Boolean = true
      override fun suggestSdkName(currentSdkName: String?, sdkHome: String?): String = TestSdkGenerator.findTestSdk(sdkHome!!)!!.name
      override fun suggestHomePath(): String? = null
      override fun suggestHomePaths(): Collection<String> = TestSdkGenerator.getAllTestSdks().map { it.homePath }
      override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? = null
      override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}
      override fun getBinPath(sdk: Sdk): String = File(sdk.homePath, "bin").path
      override fun getToolsPath(sdk: Sdk): String = File(sdk.homePath, "lib/tools.jar").path
      override fun getVMExecutablePath(sdk: Sdk): String = File(sdk.homePath, "bin/java").path
      override fun getVersionString(sdkHome: String): String? = TestSdkGenerator.findTestSdk(sdkHome)?.versionString
    }
  }

  object DependentTestSdkType : DependentSdkType("dependent-test-type"), TestSdkType {
    private fun getParentPath(sdk: Sdk, relativePath: String) =
      (sdk as? DependentTestSdk)
        ?.parent?.homePath
        ?.let { File(it, relativePath).path }

    override fun getPresentableName(): String = name
    override fun isValidSdkHome(path: String?): Boolean = true
    override fun suggestSdkName(currentSdkName: String?, sdkHome: String?): String = "dependent-sdk-name"
    override fun suggestHomePath(): String? = null
    override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? = null
    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}
    override fun getBinPath(sdk: Sdk) = getParentPath(sdk, "bin")
    override fun getToolsPath(sdk: Sdk) = getParentPath(sdk, "lib/tools.jar")
    override fun getVMExecutablePath(sdk: Sdk) = getParentPath(sdk, "bin/java")

    override fun getUnsatisfiedDependencyMessage() = "Unsatisfied dependency message"
    override fun isValidDependency(sdk: Sdk) = sdk is TestSdkType
    override fun getDependencyType() = TestSdkType
  }

  open class TestSdk(name: String, homePath: String, versionString: String, sdkType: TestSdkType)
    : MockSdk(name, homePath, versionString, MultiMap(), sdkType) {

    constructor(name: String, homePath: String, versionString: String)
      : this(name, homePath, versionString, TestSdkType)

    override fun getHomePath(): String = super.getHomePath()!!
  }

  open class DependentTestSdk(name: String, homePath: String, versionString: String, val parent: TestSdk)
    : TestSdk(name, homePath, versionString, DependentTestSdkType)

  object TestSdkDownloader : SdkDownload {
    override fun supportsDownload(sdkTypeId: SdkTypeId) = sdkTypeId == TestSdkType

    override fun showDownloadUI(
      sdkTypeId: SdkTypeId,
      sdkModel: SdkModel,
      parentComponent: JComponent,
      selectedSdk: Sdk?,
      sdkCreatedCallback: Consumer<SdkDownloadTask>
    ) {
      val sdk = TestSdkGenerator.createNextSdk()
      sdkCreatedCallback.accept(object : SdkDownloadTask {
        override fun doDownload(indicator: ProgressIndicator) {}
        override fun getPlannedVersion() = sdk.versionString
        override fun getSuggestedSdkName() = sdk.name
        override fun getPlannedHomeDir() = sdk.homePath
      })
    }
  }

  object TestSdkGenerator {
    private var createdSdkCounter = 0
    private lateinit var createdSdks: MutableMap<String, TestSdk>

    fun getAllTestSdks() = createdSdks.values

    fun findTestSdk(sdk: Sdk): TestSdk? = findTestSdk(sdk.homePath!!)

    fun findTestSdk(homePath: String): TestSdk? = createdSdks[homePath]

    fun getCurrentSdk() = createdSdks.values.last()

    fun reserveNextSdk(versionString: String = "11"): SdkInfo {
      val name = "test $versionString (${createdSdkCounter++})"
      val homePath = FileUtil.toCanonicalPath(FileUtil.join(FileUtil.getTempDirectory(), "jdk-$name"))
      return SdkInfo(name, versionString, homePath)
    }

    fun createTestSdk(sdkInfo: SdkInfo): TestSdk {
      val sdk = TestSdk(sdkInfo.name, sdkInfo.homePath, sdkInfo.versionString)
      createdSdks[sdkInfo.homePath] = sdk
      return sdk
    }

    fun createNextSdk(versionString: String = "11"): TestSdk {
      val sdkInfo = reserveNextSdk(versionString)
      generateJdkStructure(sdkInfo)
      return createTestSdk(sdkInfo)
    }

    fun createNextDependentSdk(): DependentTestSdk {
      val name = "dependent-test-name (${createdSdkCounter++})"
      val versionString = "11"
      val homePath = FileUtil.getTempDirectory() + "/jdk-$name"
      val parentSdk = createNextSdk()
      val sdk = DependentTestSdk(name, homePath, versionString, parentSdk)
      createdSdks[homePath] = sdk
      return sdk
    }

    fun generateJdkStructure(sdkInfo: SdkInfo) {
      val homePath = sdkInfo.homePath
      createFile("$homePath/release")
      createFile("$homePath/jre/lib/rt.jar")
      createFile("$homePath/bin/javac")
      createFile("$homePath/bin/java")
      val properties = Properties()
      properties.setProperty("JAVA_FULL_VERSION", sdkInfo.versionString)
      File("$homePath/release").outputStream().use {
        properties.store(it, null)
      }
    }

    private fun createFile(path: String) {
      val file = File(path)
      file.parentFile.mkdirs()
      file.createNewFile()
    }

    fun reset() {
      createdSdkCounter = 0
      createdSdks = LinkedHashMap()
    }

    data class SdkInfo(val name: String, val versionString: String, val homePath: String)
  }

  companion object {
    fun assertSdk(expected: TestSdk, actual: Sdk) {
      assertEquals(expected.name, actual.name)
      assertEquals(expected.sdkType, actual.sdkType)
      assertEquals(expected, TestSdkGenerator.findTestSdk(actual))
    }

    fun registerSdk(sdk: TestSdk, parentDisposable: Disposable) {
      invokeAndWaitIfNeeded {
        runWriteAction {
          val jdkTable = ProjectJdkTable.getInstance()
          jdkTable.addJdk(sdk, parentDisposable)
        }
      }
    }

    fun registerSdks(vararg sdks: TestSdk, parentDisposable: Disposable) {
      sdks.forEach { registerSdk(it, parentDisposable) }
    }

    fun removeSdk(sdk: Sdk) {
      invokeAndWaitIfNeeded {
        runWriteAction {
          val jdkTable = ProjectJdkTable.getInstance()
          jdkTable.removeJdk(sdk)
        }
      }
    }

    fun removeSdks(vararg sdks: Sdk) {
      sdks.forEach(::removeSdk)
    }
  }
}