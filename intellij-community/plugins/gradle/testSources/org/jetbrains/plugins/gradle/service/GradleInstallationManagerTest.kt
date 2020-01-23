// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service

import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.junit.Test

class GradleInstallationManagerTest : GradleInstallationManagerTestCase() {
  @Test
  fun testGradleVersionResolving() {
    val gradle_4_10 = GradleVersion.version("4.10")
    val gradle_6_0 = GradleVersion.version("6.0")
    val currentGradle = GradleVersion.current()

    testGradleVersion(gradle_4_10, DistributionType.LOCAL, wrapperVersionToGenerate = gradle_4_10)
    testGradleVersion(gradle_6_0, DistributionType.LOCAL, wrapperVersionToGenerate = gradle_6_0)
    testGradleVersion(currentGradle, DistributionType.BUNDLED, wrapperVersionToGenerate = null)
    testGradleVersion(currentGradle, DistributionType.DEFAULT_WRAPPED, wrapperVersionToGenerate = null)
    testGradleVersion(gradle_4_10, DistributionType.DEFAULT_WRAPPED, wrapperVersionToGenerate = gradle_4_10)
    testGradleVersion(gradle_6_0, DistributionType.DEFAULT_WRAPPED, wrapperVersionToGenerate = gradle_6_0)
  }
}