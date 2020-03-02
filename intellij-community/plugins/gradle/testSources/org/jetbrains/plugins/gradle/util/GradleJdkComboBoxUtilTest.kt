// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_JAVA_HOME
import com.intellij.openapi.roots.ui.configuration.SdkListItem.SdkReferenceItem

class GradleJdkComboBoxUtilTest : GradleJdkComboBoxUtilTestCase() {
  fun `test gradle JVM combobox empty setup`() {
    val comboBox = createJdkComboBox()
    comboBox.addUsefulGradleJvmReferences(externalProjectPath)
    assertComboBoxContent(comboBox)
      .nothing()
  }

  fun `test gradle JVM combobox setup`() {
    val javaHomeJdk = TestSdkGenerator.createNextSdk()
    val gradleJavaHomeJdk = TestSdkGenerator.createNextSdk()

    environment.withVariables(JAVA_HOME to javaHomeJdk.homePath) {
      GradleJdkResolutionTestCase.withGradleProperties(externalProjectPath, gradleJavaHomeJdk) {
        val comboBox = createJdkComboBox()
        comboBox.addUsefulGradleJvmReferences(externalProjectPath)
        assertComboBoxContent(comboBox)
          .reference<SdkReferenceItem>(USE_GRADLE_JAVA_HOME) { assertReferenceItem(it, GRADLE_JAVA_HOME_PROPERTY, true) }
          .reference<SdkReferenceItem>(USE_JAVA_HOME) { assertReferenceItem(it, JAVA_HOME, true) }
          .nothing()
      }
    }
  }

  fun `test gradle JVM reference selection`() {
    val comboBox = createJdkComboBox()

    comboBox.setSelectedJdkReference(USE_GRADLE_JAVA_HOME)
    assertComboBoxSelection<SdkReferenceItem>(comboBox, null, USE_GRADLE_JAVA_HOME)
    comboBox.setSelectedJdkReference(USE_JAVA_HOME)
    assertComboBoxSelection<SdkReferenceItem>(comboBox, null, USE_JAVA_HOME)

    assertComboBoxContent(comboBox)
      .reference<SdkReferenceItem>(USE_GRADLE_JAVA_HOME) { assertReferenceItem(it, GRADLE_JAVA_HOME_PROPERTY, false) }
      .reference<SdkReferenceItem>(USE_JAVA_HOME, isSelected = true) { assertReferenceItem(it, JAVA_HOME, false) }
      .nothing()
  }
}