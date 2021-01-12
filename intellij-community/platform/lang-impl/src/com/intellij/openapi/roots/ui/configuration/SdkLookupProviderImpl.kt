// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.SdkLookupProvider.SdkInfo
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.AsyncPromise
import java.util.concurrent.atomic.AtomicReference

@ApiStatus.Internal
class SdkLookupProviderImpl : SdkLookupProvider {

  @Volatile
  private var context: SdkLookupContext? = null

  override fun newLookupBuilder(): SdkLookupBuilder {
    return CommonSdkLookupBuilder(lookup = ::lookup)
  }

  @TestOnly
  fun getSdkPromiseForTests() = context?.getSdkPromiseForTests()

  override fun getSdkInfo(): SdkInfo {
    return context?.getSdkInfo() ?: SdkInfo.Undefined
  }

  override fun getSdk(): Sdk? {
    return context?.getSdk()
  }

  override fun blockingGetSdk(): Sdk? {
    return context?.blockingGetSdk()
  }

  override fun onProgress(progressIndicator: ProgressIndicator) {
    context?.onProgress(progressIndicator)
  }

  private fun lookup(builder: CommonSdkLookupBuilder) {
    val context = SdkLookupContext()
    this.context = context
    context.onProgress(builder.progressIndicator)
    val parameters = builder
      .withProgressIndicator(context.progressIndicator)
      .onSdkNameResolved { sdk ->
        context.setSdkInfo(sdk)
        builder.onSdkNameResolved(sdk)
      }
      .onSdkResolved { sdk ->
        context.setSdk(sdk)
        builder.onSdkResolved(sdk)
      }
    invokeAndWaitIfNeeded {
      service<SdkLookup>().lookup(parameters)
    }
  }

  private class SdkLookupContext {
    private val sdk = AsyncPromise<Sdk?>()
    private val sdkInfo = AtomicReference<SdkInfo>(SdkInfo.Unresolved)
    val progressIndicator = ProgressIndicatorBase()

    @TestOnly
    fun getSdkPromiseForTests() = sdk
    fun getSdkInfo(): SdkInfo = sdkInfo.get()
    fun getSdk(): Sdk? = if (getSdkInfo() is SdkInfo.Resolved) sdk.get() else null
    fun blockingGetSdk(): Sdk? = sdk.get()

    fun onProgress(progressIndicator: ProgressIndicator?) {
      if (progressIndicator is ProgressIndicatorEx) {
        this.progressIndicator.addStateDelegate(progressIndicator)
      }
    }

    fun setSdkInfo(sdk: Sdk?) {
      when (sdk) {
        null -> sdkInfo.set(SdkInfo.Unresolved)
        else -> sdkInfo.set(SdkInfo.Resolving(sdk.name, sdk.versionString, sdk.homePath))
      }
    }

    fun setSdk(sdk: Sdk?) {
      when (sdk) {
        null -> sdkInfo.set(SdkInfo.Undefined)
        else -> sdkInfo.set(SdkInfo.Resolved(sdk.name, sdk.versionString, sdk.homePath))
      }
      this.sdk.setResult(sdk)
    }
  }
}