// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_INTERNAL_JAVA
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*

class ExternalSystemJdkComboBoxUtilTest : ExternalSystemJdkComboBoxUtilTestCase() {
  fun `test reference usage`() {
    val sdk1 = createAndRegisterSdk()
    val sdk2 = createAndRegisterSdk(isProjectSdk = true)
    val sdk3 = createAndRegisterSdk()
    val invalidSdk = TestSdkGenerator.createNextSdk()
    val internalJdk = getInternalJdk()

    val comboBox = createJdkComboBox()

    assertComboBoxContent(comboBox)
      .reference(USE_PROJECT_JDK)
      .reference(sdk1)
      .reference(sdk2)
      .reference(sdk3)
      .nothing()

    comboBox.setSelectedJdkReference(USE_PROJECT_JDK)
    assertComboBoxSelection<ProjectSdkItem>(comboBox, sdk2, USE_PROJECT_JDK)
    comboBox.setSelectedJdkReference(sdk1.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk1, sdk1.name)
    comboBox.setSelectedJdkReference(sdk2.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk2, sdk2.name)
    comboBox.setSelectedJdkReference(sdk3.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk3, sdk3.name)
    comboBox.setSelectedJdkReference(invalidSdk.name)
    assertComboBoxSelection<InvalidSdkItem>(comboBox, null, invalidSdk.name)
    comboBox.setSelectedJdkReference(USE_INTERNAL_JAVA)
    assertComboBoxSelection<InvalidSdkItem>(comboBox, null, internalJdk.name)
    comboBox.setSelectedJdkReference(null)
    assertComboBoxSelection<NoneSdkItem>(comboBox, null, null)

    assertComboBoxContent(comboBox)
      .reference(null, isSelected = true)
      .reference(USE_PROJECT_JDK)
      .reference(internalJdk.name)
      .reference(sdk1)
      .reference(sdk2)
      .reference(sdk3)
      .nothing()
  }
}