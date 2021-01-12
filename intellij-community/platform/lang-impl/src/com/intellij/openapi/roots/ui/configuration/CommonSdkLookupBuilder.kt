// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.NlsContexts.ProgressTitle
import org.jetbrains.annotations.Nls

internal data class CommonSdkLookupBuilder(
  override val project: Project? = null,

  @Nls
  override val progressMessageTitle: @ProgressTitle String? = null,
  override val progressIndicator: ProgressIndicator? = null,

  override val sdkName: String? = null,

  override val sdkType: SdkType? = null,

  override val onBeforeSdkSuggestionStarted: () -> SdkLookupDecision = { SdkLookupDecision.CONTINUE },
  override val onLocalSdkSuggested: (UnknownSdkLocalSdkFix) -> SdkLookupDecision = { SdkLookupDecision.CONTINUE },
  override val onDownloadableSdkSuggested: (UnknownSdkDownloadableSdkFix) -> SdkLookupDecision = { SdkLookupDecision.CONTINUE },

  override val sdkHomeFilter: ((String) -> Boolean)? = null,
  override val versionFilter: ((String) -> Boolean)? = null,

  override val testSdkSequence: Sequence<Sdk?> = emptySequence(),

  override val onSdkNameResolved: (Sdk?) -> Unit = { },
  override val onSdkResolved: (Sdk?) -> Unit = { },

  private val lookup: (CommonSdkLookupBuilder) -> Unit
) : SdkLookupBuilder, SdkLookupParameters {
  override fun withProject(project: Project?) =
    copy(project = project)

  override fun withProgressMessageTitle(@Nls message: @ProgressTitle String) =
    copy(progressMessageTitle = message)

  override fun withSdkName(name: String) =
    copy(sdkName = name)

  override fun withSdkType(sdkType: SdkType) =
    copy(sdkType = sdkType)

  override fun withVersionFilter(filter: (String) -> Boolean) =
    copy(versionFilter = filter)

  override fun withSdkHomeFilter(filter: (String) -> Boolean) =
    copy(sdkHomeFilter = filter)

  override fun withProgressIndicator(indicator: ProgressIndicator) =
    copy(progressIndicator = indicator)

  override fun testSuggestedSdksFirst(sdks: Sequence<Sdk?>) =
    copy(testSdkSequence = testSdkSequence + sdks)

  override fun testSuggestedSdkFirst(sdk: () -> Sdk?) =
    copy(testSdkSequence = testSdkSequence + generateSequence(sdk) { null })

  override fun onBeforeSdkSuggestionStarted(handler: () -> SdkLookupDecision) =
    copy(onBeforeSdkSuggestionStarted = handler)

  override fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> SdkLookupDecision) =
    copy(onLocalSdkSuggested = handler)

  override fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> SdkLookupDecision) =
    copy(onDownloadableSdkSuggested = handler)

  override fun onSdkResolved(handler: (Sdk?) -> Unit) =
    copy(onSdkResolved = handler)

  override fun onSdkNameResolved(callback: (Sdk?) -> Unit) =
    copy(onSdkNameResolved = callback)

  override fun executeLookup() = lookup(this)
}