// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*

class ExternalSystemJdkComboBoxUtilTest : ExternalSystemJdkComboBoxUtilTestCase() {
  fun `test reference usage`() {
    val sdk1 = createAndRegisterSdk()
    val sdk2 = createAndRegisterSdk(isProjectSdk = true)
    val sdk3 = createAndRegisterSdk()
    val javaHomeJdk = TestSdkGenerator.createNextSdk()
    val invalidSdk = TestSdkGenerator.createNextSdk()

    environment.variables(JAVA_HOME to javaHomeJdk.homePath)

    val comboBox = createJdkComboBox()

    assertComboBoxContent(comboBox)
      .reference<ProjectSdkItem>(USE_PROJECT_JDK)
      .reference<SdkItem>(sdk1)
      .reference<SdkItem>(sdk2)
      .reference<SdkItem>(sdk3)
      .nothing()

    comboBox.setSelectedJdkReference(USE_PROJECT_JDK)
    assertComboBoxSelection<ProjectSdkItem>(comboBox, sdk2, USE_PROJECT_JDK)
    comboBox.setSelectedJdkReference(USE_JAVA_HOME)
    assertComboBoxSelection<SdkReferenceItem>(comboBox, null, USE_JAVA_HOME)
    comboBox.setSelectedJdkReference(sdk1.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk1, sdk1.name)
    comboBox.setSelectedJdkReference(sdk2.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk2, sdk2.name)
    comboBox.setSelectedJdkReference(sdk3.name)
    assertComboBoxSelection<SdkItem>(comboBox, sdk3, sdk3.name)
    comboBox.setSelectedJdkReference(invalidSdk.name)
    assertComboBoxSelection<InvalidSdkItem>(comboBox, null, invalidSdk.name)
    comboBox.setSelectedJdkReference(null)
    assertComboBoxSelection<NoneSdkItem>(comboBox, null, null)

    assertComboBoxContent(comboBox)
      .reference<NoneSdkItem>(null, isSelected = true)
      .reference<ProjectSdkItem>(USE_PROJECT_JDK)
      .reference<InvalidSdkItem>(invalidSdk.name)
      .reference<SdkReferenceItem>(USE_JAVA_HOME) { assertReferenceItem(it, JAVA_HOME, true) }
      .reference<SdkItem>(sdk1)
      .reference<SdkItem>(sdk2)
      .reference<SdkItem>(sdk3)
      .nothing()
  }
}