// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.annotations.ApiStatus
import java.util.*

@ApiStatus.Experimental
class SdkLookupBuilderEx<T> {
  private val suggestedSdks = IdentityHashMap<Sdk, T>()

  fun SdkLookupBuilder.testSuggestedSdkFirst(id: T, getSdk: () -> Sdk?) = testSuggestedSdkFirst {
    getSdk()?.also {
      suggestedSdks[it] = id
    }
  }

  fun SdkLookupBuilder.onSdkNameResolved(callback: (T?, Sdk?) -> Unit) = onSdkNameResolved {
    callback(suggestedSdks[it], it)
  }

  fun SdkLookupBuilder.onSdkResolved(callback: (T?, Sdk?) -> Unit) = onSdkResolved {
    callback(suggestedSdks[it], it)
  }

  companion object {
    operator fun <T : Any> invoke(action: SdkLookupBuilderEx<T>.() -> Unit) {
      return SdkLookupBuilderEx<T>().action()
    }
  }
}