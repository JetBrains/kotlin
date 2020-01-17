// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Nls

/**
 * Use this service to resolve an SDK request to a given component allowing
 * the platform to guess or automatically suggest a possible option (or wait
 * for an option to be completed).
 *
 * The lookup process can take some time to resolve. The code can be executed
 * from any thread. There is no guaranty callbacks happen from EDT thread too.
 */
interface SdkLookup {
  fun createBuilder(): SdkLookupBuilder
  fun lookup(lookup: SdkLookupBuilder)

  companion object {
    @JvmStatic
    fun newLookupBuilder() = service<SdkLookup>().createBuilder()
  }
}

interface SdkLookupBuilder {
  /**
   * Terminal operator of the builder to execute the search
   */
  fun executeLookup()

  @Contract(pure = true)
  fun withProject(project: Project?): SdkLookupBuilder

  @Contract(pure = true)
  fun withProgressMessageTitle(@Nls message: String): SdkLookupBuilder

  @Contract(pure = true)
  fun withSdkName(name: String): SdkLookupBuilder

  @Contract(pure = true)
  fun withProjectSdk(): SdkLookupBuilder

  @Contract(pure = true)
  fun withSdkType(sdkType: SdkType): SdkLookupBuilder

  @Contract(pure = true)
  fun withMinSdkVersionInclusive(version: String): SdkLookupBuilder

  @Contract(pure = true)
  fun withMaxSdkVersionExclusive(version: String): SdkLookupBuilder

  @Contract(pure = true)
  fun withSdkHomeFilter(filter: (String) -> Boolean): SdkLookupBuilder

  @Contract(pure = true)
  fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> Boolean): SdkLookupBuilder

  @Contract(pure = true)
  fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> Boolean): SdkLookupBuilder

  /**
   * The [Sdk#sdkType] may not match the proposed [sdkType] if the
   * same named SDK already exists. It is up to this code client
   * to resolve that situation.
   *
   * The callback is executed with [null] if the SDK search
   * failed, cancelled or returned no elements
   */
  @Contract(pure = true)
  fun onSdkResolved(handler: (Sdk?) -> Unit): SdkLookupBuilder
}
