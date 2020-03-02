// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import com.intellij.openapi.roots.ui.configuration.SdkLookupProviderImpl
import com.intellij.testFramework.PlatformTestUtil
import java.util.concurrent.TimeUnit

abstract class ExternalSystemJdkNonblockingUtilTestCase : ExternalSystemJdkUtilTestCase() {

  lateinit var sdkLookupProvider: SdkLookupProvider

  override fun setUp() {
    super.setUp()

    sdkLookupProvider = SdkLookupProviderImpl()
  }

  open fun nonblockingResolveJdkInfo(jdkReference: String?) =
    sdkLookupProvider.nonblockingResolveJdkInfo(projectSdk, jdkReference)

  fun assertSdkInfo(versionString: String, homePath: String, actualJdkReference: String?) {
    val actualSdkInfo = nonblockingResolveJdkInfo(actualJdkReference)
    require(actualSdkInfo is SdkInfo.Resolved)
    assertEquals(homePath, actualSdkInfo.homePath)
    assertEquals(versionString, actualSdkInfo.versionString)
  }

  fun assertSdkInfo(expected: Sdk, actualJdkReference: String?) {
    val actualSdkInfo = nonblockingResolveJdkInfo(actualJdkReference)
    require(actualSdkInfo is SdkInfo.Resolved) { actualSdkInfo }
    assertEquals(createSdkInfo(expected), actualSdkInfo)
  }

  fun assertSdkInfo(expected: SdkInfo, actualJdkReference: String?) {
    val actualSdkInfo = nonblockingResolveJdkInfo(actualJdkReference)
    assertEquals(expected, actualSdkInfo)
  }

  fun createResolvingSdkInfo(sdk: Sdk) = SdkInfo.Resolving(sdk.name, sdk.versionString, sdk.homePath)

  fun waitForLookup() = sdkLookupProvider.waitForLookup()

  companion object {
    fun SdkLookupProvider.waitForLookup() {
      this as SdkLookupProviderImpl
      val promise = getSdkPromiseForTests()
      if (promise != null) {
        PlatformTestUtil.waitForPromise(promise, TimeUnit.SECONDS.toMillis(10))
      }
    }
  }
}