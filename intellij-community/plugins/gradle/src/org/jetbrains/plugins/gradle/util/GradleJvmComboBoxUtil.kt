// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("GradleJvmComboBoxUtil")
@file:ApiStatus.Internal

package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.getJavaHome
import com.intellij.openapi.externalSystem.service.ui.addJdkReferenceItem
import com.intellij.openapi.externalSystem.service.ui.resolveJdkReference
import com.intellij.openapi.externalSystem.service.ui.setSelectedJdkReference
import com.intellij.openapi.roots.ui.configuration.SdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import org.jetbrains.annotations.ApiStatus


fun SdkComboBox.getSelectedGradleJvmReference(sdkLookupProvider: SdkLookupProvider): String? {
  return sdkLookupProvider.resolveGradleJvmReference(selectedItem)
}

fun SdkComboBox.setSelectedGradleJvmReference(sdkLookupProvider: SdkLookupProvider, externalProjectPath: String?, jdkReference: String?) {
  when (jdkReference) {
    USE_GRADLE_JAVA_HOME -> selectedItem = addJdkReferenceItem(GRADLE_JAVA_HOME_PROPERTY, getGradleJavaHome(externalProjectPath))
    else -> setSelectedJdkReference(sdkLookupProvider, jdkReference)
  }
}

fun SdkComboBox.addUsefulGradleJvmReferences(externalProjectPath: String?) {
  addGradleJavaHomeReferenceItem(externalProjectPath)
  addJavaHomeReferenceItem()
}

fun SdkLookupProvider.resolveGradleJvmReference(item: SdkListItem?): String? {
  return when (item) {
    is SdkListItem.SdkReferenceItem -> when (item.name) {
      GRADLE_JAVA_HOME_PROPERTY -> USE_GRADLE_JAVA_HOME
      else -> resolveJdkReference(item)
    }
    else -> resolveJdkReference(item)
  }
}

private fun SdkComboBox.addGradleJavaHomeReferenceItem(externalProjectPath: String?) {
  if (externalProjectPath == null) return
  val gradleJavaHome = getGradleJavaHome(externalProjectPath) ?: return
  addJdkReferenceItem(GRADLE_JAVA_HOME_PROPERTY, gradleJavaHome)
}

private fun SdkComboBox.addJavaHomeReferenceItem() {
  val javaHome = getJavaHome() ?: return
  addJdkReferenceItem(JAVA_HOME, javaHome)
}