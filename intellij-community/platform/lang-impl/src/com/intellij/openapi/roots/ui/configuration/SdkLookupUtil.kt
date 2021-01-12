// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("SdkLookupUtil")
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.projectRoots.Sdk

/**
 * Finds sdk at everywhere with parameters that defined by [configure]
 *
 * Note: this function block current thread until sdk is resolved
 */
fun lookupSdk(configure: (SdkLookupBuilder) -> SdkLookupBuilder): Sdk? {
  val provider = SdkLookupProviderImpl()
  var builder = provider.newLookupBuilder()
  builder = configure(builder)
  builder.executeLookup()
  return provider.blockingGetSdk()
}