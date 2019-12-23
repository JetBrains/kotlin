// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("ExternalSystemJdkComboBoxUtil")

package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.InvalidSdkException
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem

fun SdkComboBox.getSelectedJdkReference(): String? {
  return when (val it = selectedItem) {
    is SdkListItem.ProjectSdkItem -> ExternalSystemJdkUtil.USE_PROJECT_JDK
    is SdkListItem.SdkItem -> it.sdk.name
    is SdkListItem.InvalidSdkItem -> it.sdkName
    else -> null
  }
}

fun SdkComboBox.setSelectedJdkReference(jdkReference: String?) {
  selectedItem = when (jdkReference) {
    null -> showNoneSdkItem()
    ExternalSystemJdkUtil.USE_PROJECT_JDK -> showProjectSdkItem()
    else -> resolveSdkItem(jdkReference)
  }
}

private fun SdkComboBox.resolveSdkItem(selectedJdkReference: String): SdkListItem {
  try {
    val selectedJdk = ExternalSystemJdkUtil.resolveJdkName(model.sdksModel.projectSdk, selectedJdkReference)
    val selectedSdkItem = selectedJdk?.let { model.listModel.findSdkItem(selectedJdk) }
    if (selectedSdkItem == null) return showInvalidSdkItem(selectedJdkReference)
    return selectedSdkItem
  }
  catch (ex: InvalidSdkException) {
    return showInvalidSdkItem(selectedJdkReference)
  }
}