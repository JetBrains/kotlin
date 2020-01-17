// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("ExternalSystemJdkComboBoxUtil")

package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import org.jetbrains.annotations.TestOnly

fun SdkComboBox.getSelectedJdkReference() = resolveJdkReference(selectedItem)

private fun resolveJdkReference(item: SdkListItem?): String? {
  return when (item) {
    is SdkListItem.ProjectSdkItem -> USE_PROJECT_JDK
    is SdkListItem.SdkItem -> item.sdk.name
    is SdkListItem.InvalidSdkItem -> item.sdkName
    else -> null
  }
}

fun SdkComboBox.setSelectedJdkReference(jdkReference: String?) {
  selectedItem = resolveSdkItem(jdkReference)
}

private fun SdkComboBox.resolveSdkItem(jdkReference: String?): SdkListItem {
  if (jdkReference == null) return showNoneSdkItem()
  if (jdkReference == USE_PROJECT_JDK) return showProjectSdkItem()
  try {
    val selectedJdk = ExternalSystemJdkUtil.resolveJdkName(null, jdkReference)
    if (selectedJdk == null) return showInvalidSdkItem(jdkReference)
    return findSdkItem(selectedJdk) ?: addAndGetSdkItem(selectedJdk)
  }
  catch (ex: ExternalSystemJdkException) {
    return showInvalidSdkItem(jdkReference)
  }
}

private fun SdkComboBox.addAndGetSdkItem(sdk: Sdk): SdkListItem {
  SdkConfigurationUtil.addSdk(sdk)
  model.sdksModel.addSdk(sdk)
  reloadModel()
  return findSdkItem(sdk) ?: showInvalidSdkItem(sdk.name)
}

private fun SdkComboBox.findSdkItem(sdk: Sdk): SdkListItem? {
  return model.listModel.findSdkItem(sdk)
}

@TestOnly
fun resolveJdkReferenceInTests(item: SdkListItem?) = resolveJdkReference(item)
