// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("ExternalSystemJdkComboBoxUtil")
@file:ApiStatus.Internal

package com.intellij.openapi.externalSystem.service.ui

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import org.jetbrains.annotations.ApiStatus

private val RESOLVING_JDK by lazy { ExternalSystemBundle.message("external.system.java.in.resolving") }

fun SdkComboBox.getSelectedJdkReference(sdkLookupProvider: SdkLookupProvider): String? {
  return sdkLookupProvider.resolveJdkReference(selectedItem)
}

fun SdkComboBox.setSelectedJdkReference(sdkLookupProvider: SdkLookupProvider, jdkReference: String?) {
  when (jdkReference) {
    USE_PROJECT_JDK -> selectedItem = showProjectSdkItem()
    USE_JAVA_HOME -> selectedItem = addJdkReferenceItem(JAVA_HOME, getJavaHome())
    null -> when (val sdkInfo = sdkLookupProvider.getSdkInfo()) {
      SdkInfo.Undefined -> selectedItem = showNoneSdkItem()
      SdkInfo.Unresolved -> selectedItem = addJdkReferenceItem(RESOLVING_JDK, null, true)
      is SdkInfo.Resolving -> selectedItem = addJdkReferenceItem(sdkInfo.name, sdkInfo.versionString, true)
      is SdkInfo.Resolved -> setSelectedSdk(sdkInfo.name)
    }
    else -> return setSelectedSdk(jdkReference)
  }
}

fun SdkComboBox.addJdkReferenceItem(name: String, homePath: String?): SdkListItem {
  val type = getJavaSdkType()
  val versionString = homePath?.let(type::getVersionString)
  val isValid = isValidJdk(homePath)
  return addJdkReferenceItem(name, versionString, isValid)
}

fun SdkComboBox.addJdkReferenceItem(name: String, versionString: String?, isValid: Boolean): SdkListItem {
  val type = getJavaSdkType()
  return addSdkReferenceItem(type, name, versionString, isValid)
}

fun SdkLookupProvider.resolveJdkReference(item: SdkListItem?): String? {
  return when (item) {
    is SdkListItem.ProjectSdkItem -> USE_PROJECT_JDK
    is SdkListItem.SdkItem -> item.sdk.name
    is SdkListItem.InvalidSdkItem -> item.sdkName
    is SdkListItem.SdkReferenceItem -> when (item.name) {
      JAVA_HOME -> USE_JAVA_HOME
      RESOLVING_JDK -> getSdk()?.name
      else -> when (val sdkInfo = getSdkInfo()) {
        SdkInfo.Undefined -> null
        SdkInfo.Unresolved -> null
        is SdkInfo.Resolving -> null
        is SdkInfo.Resolved -> sdkInfo.name
      }
    }
    else -> null
  }
}
