// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkComboBoxTestCase
import com.intellij.openapi.roots.ui.configuration.SdkListItem

abstract class ExternalSystemJdkComboBoxUtilTestCase : SdkComboBoxTestCase() {

  override fun tearDown() {
    invokeAndWaitIfNeeded {
      runWriteAction {
        JavaAwareProjectJdkTableImpl.removeInternalJdkInTests()
      }
    }
    super.tearDown()
  }

  fun ComboBoxChecker.reference(sdk: Sdk, isSelected: Boolean = false) =
    reference(sdk.name, isSelected)

  fun ComboBoxChecker.reference(reference: String?, isSelected: Boolean = false) =
    item<SdkListItem>(isSelected) {
      assertEquals(reference, resolveJdkReferenceInTests(it))
    }

  fun getInternalJdk() = JavaAwareProjectJdkTableImpl.getInstanceEx().internalJdk

  inline fun <reified I : SdkListItem> assertComboBoxSelection(comboBox: SdkComboBox, expectedSdk: Sdk?, expectedReference: String?) {
    assertComboBoxSelection<I>(comboBox, expectedSdk)
    assertEquals(expectedReference, comboBox.getSelectedJdkReference())
  }
}