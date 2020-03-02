// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
interface SdkLookupBuilderEx<T : Any> : SdkLookupBuilder, SdkLookupParameters {

  fun testSuggestedSdkFirst(id: T? = null, getSdk: () -> Sdk?): SdkLookupBuilderEx<T>

  fun onSdkNameResolved(handler: (T?, Sdk?) -> Unit): SdkLookupBuilderEx<T>

  fun onSdkResolved(handler: (T?, Sdk?) -> Unit): SdkLookupBuilderEx<T>

  override fun withProgressIndicator(indicator: ProgressIndicator): SdkLookupBuilderEx<T>
  override fun withProject(project: Project?): SdkLookupBuilderEx<T>
  override fun withProgressMessageTitle(message: String): SdkLookupBuilderEx<T>
  override fun testSuggestedSdksFirst(sdks: Sequence<Sdk?>): SdkLookupBuilderEx<T>
  override fun withSdkName(name: String): SdkLookupBuilderEx<T>
  override fun withSdkType(sdkType: SdkType): SdkLookupBuilderEx<T>
  override fun withVersionFilter(filter: (String) -> Boolean): SdkLookupBuilderEx<T>
  override fun withSdkHomeFilter(filter: (String) -> Boolean): SdkLookupBuilderEx<T>
  override fun onBeforeSdkSuggestionStarted(handler: () -> SdkLookupDecision): SdkLookupBuilderEx<T>
  override fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> SdkLookupDecision): SdkLookupBuilderEx<T>
  override fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> SdkLookupDecision): SdkLookupBuilderEx<T>
  override fun onSdkNameResolved(callback: (Sdk?) -> Unit): SdkLookupBuilderEx<T>
  override fun onSdkResolved(handler: (Sdk?) -> Unit): SdkLookupBuilderEx<T>
}