// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.UnknownSdkType
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.HeavyPlatformTestCase
import org.jdom.Element

class SdkTypeRegistrationTest : HeavyPlatformTestCase() {
  fun `test unregister sdk type and register again`() {
    val sdkTable = ProjectJdkTable.getInstance()
    runWithRegisteredType {
      val sdk = sdkTable.createSdk("foo", MockSdkType.getInstance())
      val modificator = sdk.sdkModificator
      modificator.sdkAdditionalData = MockSdkAdditionalData("bar")
      modificator.commitChanges()
      runWriteAction {
        sdkTable.addJdk(sdk, testRootDisposable)
      }
      assertEquals("foo", assertOneElement(sdkTable.getSdksOfType(MockSdkType.getInstance())).name)
    }

    assertOneElement(sdkTable.getSdksOfType(UnknownSdkType.getInstance("Mock")))

    registerSdkType(testRootDisposable)
    val reloadedSdk = assertOneElement(sdkTable.getSdksOfType(MockSdkType.getInstance()))
    assertEquals("foo", reloadedSdk.name)
  }

  private fun runWithRegisteredType(action: () -> Unit) {
    val sdkTypeDisposable = Disposer.newDisposable()
    registerSdkType(sdkTypeDisposable)
    try {
      action()
    }
    finally {
      Disposer.dispose(sdkTypeDisposable)
    }
  }

  private fun registerSdkType(disposable: Disposable) {
    val sdkTypeDisposable = Disposer.newDisposable()
    Disposer.register(disposable, Disposable {
      runWriteAction {
        Disposer.dispose(sdkTypeDisposable)
      }
    })
    SdkType.EP_NAME.getPoint().registerExtension(MockSdkType(), sdkTypeDisposable)
  }
}

private class MockSdkType : SdkType("Mock") {
  companion object {
    @JvmStatic
    fun getInstance() = findInstance(MockSdkType::class.java)
  }

  override fun suggestHomePath(): String? = null

  override fun isValidSdkHome(path: String?): Boolean = false

  override fun suggestSdkName(currentSdkName: String?, sdkHome: String?): String = ""

  override fun createAdditionalDataConfigurable(sdkModel: SdkModel, sdkModificator: SdkModificator): AdditionalDataConfigurable? = null

  override fun getPresentableName(): String = "Mock"

  override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {
    additional.setAttribute("data", (additionalData as MockSdkAdditionalData).data)
  }

  override fun loadAdditionalData(additional: Element): SdkAdditionalData? {
    return MockSdkAdditionalData(additional.getAttributeValue("data") ?: "")
  }
}

private class MockSdkAdditionalData(var data: String) : SdkAdditionalData