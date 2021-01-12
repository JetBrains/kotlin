// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

internal class SdkLookupProviders {
  private val providers = ConcurrentHashMap<SdkLookupProvider.Id, SdkLookupProvider>()

  fun getProvider(providerId: SdkLookupProvider.Id): SdkLookupProvider =
    providers.getOrPut(providerId) { SdkLookupProviderImpl() }

  companion object {
    fun getProvider(project: Project, providerId: SdkLookupProvider.Id) =
      project.service<SdkLookupProviders>().getProvider(providerId)
  }
}