// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import org.junit.Test

class ExternalSystemJdkComboBoxTest : ExternalSystemJdkComboBoxTestCase() {

  @Test
  fun `test simple usage`() {
    comboBox.refreshData(USE_PROJECT_JDK, JDK10)
    assertSelectedJdk(JDK6.name, JDK6)
    assertSelectedJdk(JDK7.name, JDK7)
    assertSelectedJdk(JDK8.name, JDK8)
    assertSelectedJdk(JDK9.name, JDK9)
    assertSelectedJdk(JDK10.name, JDK10)
    assertSelectedJdk(JDK11.name, JDK11)
    assertSelectedJdk(USE_PROJECT_JDK, JDK10)
  }

  @Test
  fun `test undefined project jdk`() {
    comboBox.refreshData(USE_PROJECT_JDK, null)
    assertEquals(null, comboBox.selectedJdk)
  }
}