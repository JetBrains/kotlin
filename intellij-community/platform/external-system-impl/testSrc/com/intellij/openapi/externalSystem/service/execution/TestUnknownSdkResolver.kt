// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.SdkTestCase.*
import com.intellij.openapi.roots.ui.configuration.UnknownSdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask

object TestUnknownSdkResolver : UnknownSdkResolver {
  var useLocalSdkFix = true

  override fun supportsResolution(sdkTypeId: SdkTypeId): Boolean {
    return sdkTypeId is TestSdkType
  }

  override fun createResolver(project: Project?, indicator: ProgressIndicator): UnknownSdkLookup {
    return TestUnknownSdkLookup()
  }

  private class TestUnknownSdkLookup : UnknownSdkLookup {
    override fun proposeLocalFix(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkLocalSdkFix? {
      if (!useLocalSdkFix) return null
      val testSdk = TestSdkGenerator.getAllTestSdks().asSequence()
        .filter { sdk.sdkType == it.sdkType }
        .filter { sdk.sdkVersionStringPredicate?.test(it.versionString) != false }
        .filter { sdk.sdkHomePredicate?.test(it.homePath) != false }
        .maxWith(sdk.sdkType.versionComparator())
      if (testSdk == null) return null
      return TestUnknownSdkLocalSdkFix(testSdk)
    }

    override fun proposeDownload(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkDownloadableSdkFix? {
      if (useLocalSdkFix) return null
      return TestUnknownSdkDownloadableSdkFix()
    }
  }

  private class TestUnknownSdkLocalSdkFix(private val sdk: TestSdk) : UnknownSdkLocalSdkFix {
    override fun getVersionString() = sdk.versionString
    override fun getSuggestedSdkName() = sdk.name
    override fun getExistingSdkHome() = sdk.homePath
  }

  private class TestUnknownSdkDownloadableSdkFix : UnknownSdkDownloadableSdkFix {
    private val sdkInfo = TestSdkGenerator.reserveNextSdk()
    override fun getVersionString() = sdkInfo.versionString
    override fun getDownloadDescription() = sdkInfo.name
    override fun createTask(indicator: ProgressIndicator) =
      object : SdkDownloadTask {
        override fun getPlannedVersion() = sdkInfo.versionString
        override fun getSuggestedSdkName() = sdkInfo.name
        override fun getPlannedHomeDir() = sdkInfo.homePath
        override fun doDownload(indicator: ProgressIndicator) {
          TestSdkGenerator.createSdk(sdkInfo)
        }
      }
  }
}