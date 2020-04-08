// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
interface SdkLookupProvider {

  fun newLookupBuilder(): SdkLookupBuilder

  fun getSdkInfo(): SdkInfo

  fun getSdk(): Sdk?

  fun blockingGetSdk(): Sdk?

  fun onProgress(progressIndicator: ProgressIndicator)

  sealed class SdkInfo {
    object Undefined : SdkInfo()
    object Unresolved : SdkInfo()
    data class Resolving(val name: String, val versionString: String?, val homePath: String?) : SdkInfo()
    data class Resolved(val name: String, val versionString: String?, val homePath: String?) : SdkInfo()
  }

  interface Id

  companion object {
    @JvmStatic
    fun getInstance(project: Project, providerId: Id): SdkLookupProvider {
      return SdkLookupProviders.getProvider(project, providerId)
    }
  }
}