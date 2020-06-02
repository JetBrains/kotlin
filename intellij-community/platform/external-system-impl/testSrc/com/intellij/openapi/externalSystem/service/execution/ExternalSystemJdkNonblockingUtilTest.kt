// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*
import com.intellij.openapi.externalSystem.service.execution.TestUnknownSdkResolver.TestUnknownSdkFixMode.TEST_DOWNLOADABLE_FIX
import com.intellij.openapi.externalSystem.service.execution.TestUnknownSdkResolver.TestUnknownSdkFixMode.TEST_LOCAL_FIX
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo

class ExternalSystemJdkNonblockingUtilTest : ExternalSystemJdkNonblockingUtilTestCase() {
  fun `test nonblocking jdk resolution (project jdk)`() {
    assertSdkInfo(SdkInfo.Undefined, USE_PROJECT_JDK)
    val sdk = createAndRegisterSdk(isProjectSdk = true)
    assertSdkInfo(sdk, USE_PROJECT_JDK)
  }

  fun `test nonblocking jdk resolution (java home)`() {
    val sdk = TestSdkGenerator.createNextSdk()
    environment.withVariables(JAVA_HOME to null) { assertSdkInfo(SdkInfo.Undefined, USE_JAVA_HOME) }
    environment.withVariables(JAVA_HOME to sdk.homePath) { assertSdkInfo(sdk.versionString, sdk.homePath, USE_JAVA_HOME) }
  }

  fun `test nonblocking jdk resolution (internal jdk)`() {
    assertSdkInfo(jdkProvider.internalJdk, USE_INTERNAL_JAVA)
  }

  fun `test nonblocking jdk resolution (sdk name)`() {
    val sdk = createAndRegisterSdk()
    assertSdkInfo(sdk, sdk.name)
  }

  fun `test nonblocking jdk resolution (resolving)`() {
    val sdk = createAndRegisterSdk()

    assertSdkInfo(SdkInfo.Undefined, null)

    sdkLookupProvider.newLookupBuilder()
      .testSuggestedSdkFirst { sdk }
      .onSdkNameResolved { assertSdkInfo(createResolvingSdkInfo(it!!), null) }
      .onSdkResolved { assertSdkInfo(it!!, null) }
      .executeLookup()
    waitForLookup()
    assertSdkInfo(sdk, null)

    assertUnexpectedSdksRegistration {
      TestUnknownSdkResolver.unknownSdkFixMode = TEST_LOCAL_FIX
      sdkLookupProvider.newLookupBuilder()
        .withSdkType(TestSdkType)
        .onSdkNameResolved { assertSdkInfo(createResolvingSdkInfo(it!!), null) }
        .onSdkResolved { assertSdkInfo(it!!, null) }
        .executeLookup()
      waitForLookup()
      assertSdkInfo(sdk, null)
    }

    assertNewlyRegisteredSdks({ TestSdkGenerator.getCurrentSdk() }) {
      TestUnknownSdkResolver.unknownSdkFixMode = TEST_DOWNLOADABLE_FIX
      sdkLookupProvider.newLookupBuilder()
        .withSdkType(TestSdkType)
        .onSdkNameResolved { assertSdkInfo(createResolvingSdkInfo(it!!), null) }
        .onSdkResolved { assertSdkInfo(it!!, null) }
        .executeLookup()
      waitForLookup()
      assertSdkInfo(TestSdkGenerator.getCurrentSdk(), null)
      assertNotSame(sdk, TestSdkGenerator.getCurrentSdk())
    }
  }
}