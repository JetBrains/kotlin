// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.environment.Environment
import com.intellij.openapi.externalSystem.util.environment.TestEnvironment
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.testFramework.replaceService

abstract class ExternalSystemJdkComboBoxUtilTestCase : SdkComboBoxTestCase() {

  val environment get() = Environment.getInstance() as TestEnvironment

  lateinit var sdkLookupProvider: SdkLookupProvider

  override fun setUp() {
    super.setUp()

    val application = ApplicationManager.getApplication()
    application.replaceService(Environment::class.java, TestEnvironment(), testRootDisposable)

    environment.variables(ExternalSystemJdkUtil.JAVA_HOME to null)

    sdkLookupProvider = SdkLookupProviderImpl()
  }

  override fun tearDown() {
    invokeAndWaitIfNeeded {
      runWriteAction {
        JavaAwareProjectJdkTableImpl.removeInternalJdkInTests()
      }
    }
    super.tearDown()
  }

  open fun SdkComboBox.setSelectedJdkReference(jdkReference: String?) {
    setSelectedJdkReference(sdkLookupProvider, jdkReference)
  }

  open fun SdkComboBox.getSelectedJdkReference(): String? {
    return getSelectedJdkReference(sdkLookupProvider)
  }

  open fun resolveJdkReference(item: SdkListItem): String? {
    return sdkLookupProvider.resolveJdkReference(item)
  }

  inline fun <reified I : SdkListItem> ComboBoxChecker.reference(
    sdk: Sdk,
    isSelected: Boolean = false,
    noinline assertItem: SdkComboBox.(I) -> Unit = {}
  ) = reference(sdk.name, isSelected, assertItem)

  inline fun <reified I : SdkListItem> ComboBoxChecker.reference(
    reference: String?,
    isSelected: Boolean = false,
    noinline assertItem: SdkComboBox.(I) -> Unit = {}
  ) = item<I>(isSelected) {
    assertEquals(reference, resolveJdkReference(it))
    assertItem(it)
  }

  inline fun <reified I : SdkListItem> assertComboBoxSelection(comboBox: SdkComboBox, expectedSdk: Sdk?, expectedReference: String?) {
    assertComboBoxSelection<I>(comboBox, expectedSdk)
    assertEquals(expectedReference, comboBox.getSelectedJdkReference())
  }

  fun assertReferenceItem(item: SdkListItem.SdkReferenceItem, name: String, isValid: Boolean) {
    assertEquals(name, item.name)
    assertEquals(isValid, item.isValid)
  }
}