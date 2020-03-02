// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType

internal data class SdkLookupBuilderExImpl<T : Any>(
  private val setIdentifier: (id: T?, sdk: Sdk) -> Unit,
  private val getIdentifier: (sdk: Sdk) -> T?,
  private val builder: CommonSdkLookupBuilder,
  private val lookup: (SdkLookupBuilderExImpl<T>) -> Unit
) : SdkLookupBuilderEx<T>, SdkLookupParameters by builder {

  constructor(
    setIdentifier: (id: T?, sdk: Sdk) -> Unit,
    getIdentifier: (sdk: Sdk) -> T?,
    lookup: (SdkLookupBuilderExImpl<T>) -> Unit
  ) : this(
    setIdentifier,
    getIdentifier,
    CommonSdkLookupBuilder(lookup = { }),
    lookup
  )

  override fun testSuggestedSdkFirst(id: T?, getSdk: () -> Sdk?) =
    copy(builder = builder.testSuggestedSdksFirst(sequence { yield(getSdk()?.also { setIdentifier(id, it) }) }))

  override fun onSdkNameResolved(handler: (T?, Sdk?) -> Unit) =
    copy(builder = builder.onSdkNameResolved { handler(it?.let(getIdentifier), it) })

  override fun onSdkResolved(handler: (T?, Sdk?) -> Unit) =
    copy(builder = builder.onSdkResolved { handler(it?.let(getIdentifier), it) })

  override fun withProject(project: Project?) =
    copy(builder = builder.withProject(project))

  override fun withProgressIndicator(indicator: ProgressIndicator) =
    copy(builder = builder.withProgressIndicator(indicator))

  override fun withProgressMessageTitle(message: String) =
    copy(builder = builder.withProgressMessageTitle(message))

  override fun testSuggestedSdksFirst(sdks: Sequence<Sdk?>) =
    copy(builder = builder.testSuggestedSdksFirst(sdks))

  override fun withSdkName(name: String) =
    copy(builder = builder.withSdkName(name))

  override fun withSdkType(sdkType: SdkType) =
    copy(builder = builder.withSdkType(sdkType))

  override fun withVersionFilter(filter: (String) -> Boolean) =
    copy(builder = builder.withVersionFilter(filter))

  override fun withSdkHomeFilter(filter: (String) -> Boolean) =
    copy(builder = builder.withSdkHomeFilter(filter))

  override fun onBeforeSdkSuggestionStarted(handler: () -> SdkLookupDecision) =
    copy(builder = builder.onBeforeSdkSuggestionStarted(handler))

  override fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> SdkLookupDecision) =
    copy(builder = builder.onLocalSdkSuggested(handler))

  override fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> SdkLookupDecision) =
    copy(builder = builder.onDownloadableSdkSuggested(handler))

  override fun onSdkNameResolved(callback: (Sdk?) -> Unit) =
    copy(builder = builder.onSdkNameResolved(callback))

  override fun onSdkResolved(handler: (Sdk?) -> Unit) =
    copy(builder = builder.onSdkResolved(handler))

  override fun executeLookup() = lookup(this)
}