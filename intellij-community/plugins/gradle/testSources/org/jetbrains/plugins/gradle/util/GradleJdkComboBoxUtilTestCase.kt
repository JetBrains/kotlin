// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.ui.ExternalSystemJdkComboBoxUtilTestCase
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem

abstract class GradleJdkComboBoxUtilTestCase : ExternalSystemJdkComboBoxUtilTestCase() {

  lateinit var externalProjectPath: String

  override fun setUp() {
    super.setUp()

    externalProjectPath = GradleJdkResolutionTestCase.createUniqueTempDirectory()
  }

  override fun SdkComboBox.setSelectedJdkReference(jdkReference: String?) {
    setSelectedGradleJvmReference(sdkLookupProvider, externalProjectPath, jdkReference)
  }

  override fun SdkComboBox.getSelectedJdkReference(): String? {
    return getSelectedGradleJvmReference(sdkLookupProvider)
  }

  override fun resolveJdkReference(item: SdkListItem): String? {
    return sdkLookupProvider.resolveGradleJvmReference(item)
  }
}