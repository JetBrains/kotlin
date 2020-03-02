// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo

class GradleJdkNonblockingUtilTest : GradleJdkNonblockingUtilTestCase() {
  fun `test nonblocking jdk resolution (gradle properties)`() {
    val sdk = TestSdkGenerator.createNextSdk()
    assertSdkInfo(SdkInfo.Undefined, USE_GRADLE_JAVA_HOME)
    GradleJdkResolutionTestCase.withGradleProperties(externalProjectPath, sdk) {
      assertSdkInfo(sdk.versionString, sdk.homePath, USE_GRADLE_JAVA_HOME)
    }
  }
}