// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:ApiStatus.Internal

package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.*
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupDecision
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import org.jetbrains.annotations.ApiStatus


fun SdkLookupProvider.nonblockingResolveJdkInfo(projectSdk: Sdk?, jdkReference: String?): SdkInfo {
  return when (jdkReference) {
    USE_JAVA_HOME -> createJdkInfo(JAVA_HOME, getJavaHome())
    USE_PROJECT_JDK -> nonblockingResolveProjectJdkInfo(projectSdk)
    USE_INTERNAL_JAVA -> createSdkInfo(getInternalJdk())
    else -> nonblockingResolveSdkInfoBySdkName(jdkReference)
  }
}

private fun getInternalJdk(): Sdk {
  return ExternalSystemJdkProvider.getInstance().internalJdk
}

private fun SdkLookupProvider.nonblockingResolveProjectJdkInfo(projectSdk: Sdk?): SdkInfo {
  if (projectSdk == null) return SdkInfo.Undefined
  val resolvedSdk = resolveDependentJdk(projectSdk)
  return nonblockingResolveSdkInfo(resolvedSdk)
}

private fun SdkLookupProvider.nonblockingResolveSdkInfo(sdk: Sdk?): SdkInfo {
  if (sdk == null) return SdkInfo.Undefined
  executeSdkLookup(sdk)
  return getSdkInfo()
}

private fun SdkLookupProvider.nonblockingResolveSdkInfoBySdkName(sdkName: String?): SdkInfo {
  if (sdkName == null) return getSdkInfo()
  executeSdkLookup(sdkName)
  return getSdkInfo()
}

fun SdkLookupProvider.nonblockingResolveSdkBySdkName(sdkName: String?): Sdk? {
  if (sdkName == null) return getSdk()
  executeSdkLookup(sdkName)
  return getSdk()
}

private fun SdkLookupProvider.executeSdkLookup(sdk: Sdk?) {
  newLookupBuilder()
    .testSuggestedSdkFirst { sdk }
    .onBeforeSdkSuggestionStarted { SdkLookupDecision.STOP }
    .executeLookup()
}

private fun SdkLookupProvider.executeSdkLookup(sdkName: String) {
  newLookupBuilder()
    .withSdkName(sdkName)
    .onBeforeSdkSuggestionStarted { SdkLookupDecision.STOP }
    .executeLookup()
}

fun createJdkInfo(name: String, homePath: String?): SdkInfo {
  if (homePath == null) return SdkInfo.Undefined
  val type = getJavaSdkType()
  val versionString = type.getVersionString(homePath)
  return SdkInfo.Resolved(name, versionString, homePath)
}

fun createSdkInfo(sdk: Sdk): SdkInfo {
  return SdkInfo.Resolved(sdk.name, sdk.versionString, sdk.homePath)
}
