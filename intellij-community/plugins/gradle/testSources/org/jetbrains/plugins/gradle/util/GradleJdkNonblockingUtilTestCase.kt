// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkNonblockingUtilTestCase

abstract class GradleJdkNonblockingUtilTestCase : ExternalSystemJdkNonblockingUtilTestCase() {
  lateinit var externalProjectPath: String

  override fun setUp() {
    super.setUp()

    externalProjectPath = GradleJdkResolutionTestCase.createUniqueTempDirectory()
  }

  override fun nonblockingResolveJdkInfo(jdkReference: String?) =
    sdkLookupProvider.nonblockingResolveGradleJvmInfo(project, externalProjectPath, jdkReference)
}