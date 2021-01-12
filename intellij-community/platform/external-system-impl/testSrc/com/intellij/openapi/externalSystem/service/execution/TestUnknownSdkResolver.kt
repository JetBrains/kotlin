// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkRequirements
import com.intellij.openapi.roots.ui.configuration.SdkTestCase.TestSdkGenerator
import com.intellij.openapi.roots.ui.configuration.SdkTestCase.TestSdkType
import com.intellij.openapi.roots.ui.configuration.UnknownSdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.util.lang.JavaVersion

object TestUnknownSdkResolver : UnknownSdkResolver {
  lateinit var unknownSdkFixMode: TestUnknownSdkFixMode

  private val javaSdkType get() = ExternalSystemJdkProvider.getInstance().javaSdkType

  override fun supportsResolution(sdkTypeId: SdkTypeId): Boolean {
    return when (unknownSdkFixMode) {
      TestUnknownSdkFixMode.TEST_LOCAL_FIX -> sdkTypeId is TestSdkType
      TestUnknownSdkFixMode.REAL_LOCAL_FIX -> sdkTypeId == javaSdkType
      TestUnknownSdkFixMode.TEST_DOWNLOADABLE_FIX -> sdkTypeId is TestSdkType
    }
  }

  override fun createResolver(project: Project?, indicator: ProgressIndicator): UnknownSdkLookup {
    return TestUnknownSdkLookup()
  }

  private fun suggestsTestUnknownSdkLocalFixes(): Sequence<TestUnknownSdkLocalFix> {
    return TestSdkGenerator.getAllTestSdks().asSequence()
      .mapNotNull { createTestUnknownSdkLocalFix(it) }
  }

  private fun suggestsRealUnknownSdkLocalFixes(): Sequence<TestUnknownSdkLocalFix> {
    val appFixes = ProjectJdkTable.getInstance().allJdks.asSequence()
      .mapNotNull { createTestUnknownSdkLocalFix(it) }
    val localFixes = javaSdkType.suggestHomePaths().asSequence()
      .mapNotNull { createTestUnknownSdkLocalFix(it) }
    return appFixes + localFixes
  }

  private fun createTestUnknownSdkLocalFix(sdk: Sdk): TestUnknownSdkLocalFix? {
    val homePath = sdk.homePath ?: return null
    val versionString = sdk.versionString ?: return null
    val version = JavaVersion.tryParse(versionString) ?: return null
    return TestUnknownSdkLocalFix(homePath, versionString, version, sdk.name)
  }

  private fun createTestUnknownSdkLocalFix(homePath: String): TestUnknownSdkLocalFix? {
    val versionString = javaSdkType.getVersionString(homePath) ?: return null
    val version = JavaVersion.tryParse(versionString) ?: return null
    val suggestedName = javaSdkType.suggestSdkName(null, homePath)
    return TestUnknownSdkLocalFix(homePath, versionString, version, suggestedName)
  }

  private class TestUnknownSdkLookup : UnknownSdkLookup {
    override fun proposeLocalFix(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkLocalSdkFix? {
      val suggestedUnknownSdkLocalFixes = when (unknownSdkFixMode) {
        TestUnknownSdkFixMode.TEST_LOCAL_FIX -> suggestsTestUnknownSdkLocalFixes()
        TestUnknownSdkFixMode.REAL_LOCAL_FIX -> suggestsRealUnknownSdkLocalFixes()
        TestUnknownSdkFixMode.TEST_DOWNLOADABLE_FIX -> return null
      }
      val nameFilter = sdk.sdkName?.let { JdkRequirements.parseRequirement(it) }
      return suggestedUnknownSdkLocalFixes
        .filter { sdk.sdkVersionStringPredicate?.test(it.versionString) != false }
        .filter { sdk.sdkHomePredicate?.test(it.existingSdkHome) != false }
        .filter { nameFilter?.matches(it) != false }
        .maxBy { it.version }
    }

    override fun proposeDownload(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkDownloadableSdkFix? {
      if (unknownSdkFixMode != TestUnknownSdkFixMode.TEST_DOWNLOADABLE_FIX) return null
      return TestUnknownSdkDownloadableSdkFix()
    }
  }

  private class TestUnknownSdkLocalFix(
    private val homeDir: String,
    private val versionString: String,
    val version: JavaVersion,
    private val suggestedName: String
  ) : UnknownSdkLocalSdkFix {
    override fun getExistingSdkHome() = homeDir
    override fun getVersionString() = versionString
    override fun getSuggestedSdkName(): String = suggestedName
    override fun configureSdk(sdk: Sdk) {}
  }

  private class TestUnknownSdkDownloadableSdkFix : UnknownSdkDownloadableSdkFix {
    private val sdkInfo = TestSdkGenerator.reserveNextSdk()
    override fun getVersionString() = sdkInfo.versionString
    override fun getDownloadDescription() = sdkInfo.name
    override fun configureSdk(sdk: Sdk) {}
    override fun createTask(indicator: ProgressIndicator) =
      object : SdkDownloadTask {
        override fun getPlannedVersion() = sdkInfo.versionString
        override fun getSuggestedSdkName() = sdkInfo.name
        override fun getPlannedHomeDir() = sdkInfo.homePath
        override fun doDownload(indicator: ProgressIndicator) {
          TestSdkGenerator.generateJdkStructure(sdkInfo)
          TestSdkGenerator.createTestSdk(sdkInfo)
        }
      }
  }

  enum class TestUnknownSdkFixMode {
    TEST_LOCAL_FIX,
    REAL_LOCAL_FIX,
    TEST_DOWNLOADABLE_FIX
  }
}