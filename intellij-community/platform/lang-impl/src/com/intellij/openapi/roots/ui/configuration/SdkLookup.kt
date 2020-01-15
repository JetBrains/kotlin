// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.*
import com.intellij.openapi.projectRoots.impl.UnknownSdkTracker
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.Consumer
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

@Suppress("DataClassPrivateConstructor")
data class SdkLookupBuilder private constructor(
  val project: Project? = null,

  val progressMessageTitle: String? = null,

  val sdkName: String? = null,
  val sdkUseProjectSdk : Boolean = false,

  val sdkType: SdkType? = null,
  val sdkMinVersionInclusive: String? = null,
  val sdkMaxVersionExclusive: String? = null,

  val onLocalSdkSuggested: ((LocalSdkFix) -> Boolean)? = null,
  val onDownloadableSdkSuggested: ((DownloadSdkFix) -> Boolean)? = null,

  /**
   * The [Sdk#sdkType] may not match the proposed [sdkType] if the
   * same named SDK already exists. It is up to this code client
   * to resolve that situation.
   *
   * The callback is executed with [null] if the SDK search
   * failed, cancelled or returned no elements
   */
  val onSdkResolved: ((Sdk?) -> Unit)? = null
) {
  companion object {
    internal fun newBuilder() = SdkLookupBuilder()
  }

  @Contract(pure = true) fun withProject(project: Project?) = copy(project = project)
  @Contract(pure = true) fun withProgressMessageTitle(@Nls message: String) = copy(progressMessageTitle = message)

  @Contract(pure = true) fun withSdkName(name: String) = copy(sdkName = name)
  @Contract(pure = true) fun withProjectSdk() = copy(sdkUseProjectSdk = true)
  @Contract(pure = true) fun withSdkType(sdkType: SdkType) = copy(sdkType = sdkType)
  @Contract(pure = true) fun withMinSdkVersionInclusive(version: String) = copy(sdkMinVersionInclusive = version)
  @Contract(pure = true) fun withMaxSdkVersionExclusive(version: String) = copy(sdkMaxVersionExclusive = version)

  @Contract(pure = true) fun onLocalSdkSuggested(handler: (LocalSdkFix) -> Boolean) = copy(onLocalSdkSuggested = handler)
  @Contract(pure = true) fun onDownloadableSdkSuggested(handler: (DownloadSdkFix) -> Boolean) = copy(onDownloadableSdkSuggested = handler)
  @Contract(pure = true) fun onSdkResolved(handler: (Sdk?) -> Unit) = copy(onSdkResolved = handler)

  fun lookup() = service<SdkLookup>().lookup(copy())
}

internal class SdkLookupImpl : SdkLookup {
  override fun createBuilder() = SdkLookupBuilder.newBuilder()

  override fun lookup(lookup: SdkLookupBuilder): Unit = lookup.copy().run {
    val sdk = runReadAction {
      if (sdkUseProjectSdk) {
        require(sdkName == null) { "sdkName must not be set if sdkUseProjectSdk is configured" }
        requireNotNull(project) { "project must not be null to access a project SDK with sdkUseProjectSdk set" }

        ProjectRootManager.getInstance(project).projectSdk
      } else if (sdkName != null) {
        require(!sdkUseProjectSdk) { "sdkUseProjectSdk must not be set if sdkName is configured" }
        if (sdkType == null) {
          ProjectJdkTable.getInstance().findJdk(sdkName)
        }
        else {
          ProjectJdkTable.getInstance().findJdk(sdkName, sdkType.name)
        }
      } else {
        null
      }
    }

    if (sdk != null) {
      onSdkResolved?.invoke(sdk)
      return
    }

    if (sdkType == null) {
      //it is not possible to suggest everything, if [sdkType] is not specified
      onSdkResolved?.invoke(null)
      return
    }

    val unknownSdk = object: UnknownSdk {
      override fun getSdkMinVersionRequirement() = this@run.sdkMinVersionInclusive
      override fun getSdkMaxVersionRequirement() = this@run.sdkMaxVersionExclusive
      override fun getSdkType() : SdkType = this@run.sdkType
    }

    runWithProgress(onCancelled = { onSdkResolved?.invoke(null) }) { indicator ->
      try {
        val resolvers = UnknownSdkResolver.EP_NAME.iterable
          .mapNotNull { it.createResolver(project, indicator) }

        indicator.checkCanceled()

        val localFix = resolvers
          .asSequence()
          .mapNotNull { it.proposeLocalFix(unknownSdk, indicator) }
          .firstOrNull()

        indicator.checkCanceled()

        if (localFix != null && onLocalSdkSuggested?.invoke(localFix) != false) {
          UnknownSdkTracker.configureLocalSdk(unknownSdk, localFix, Consumer { onSdkResolved?.invoke(it) })
          return@runWithProgress
        }

        indicator.checkCanceled()

        val downloadFix = resolvers
          .asSequence()
          .mapNotNull { it.proposeDownload(unknownSdk, indicator) }
          .firstOrNull()

        indicator.checkCanceled()

        if (downloadFix != null && onDownloadableSdkSuggested?.invoke(downloadFix) != false) {
          UnknownSdkTracker.downloadFix(project, unknownSdk, downloadFix, Consumer { onSdkResolved?.invoke(it) })
          return@runWithProgress
        }

        onSdkResolved?.invoke(null)
      } catch (e: ProcessCanceledException) {
        throw e
      } catch (t: Throwable) {
        Logger
          .getInstance(SdkLookupImpl::class.java)
          .warn("Failed to resolve SDK for ${this@run}. ${t.message}", t)

        onSdkResolved?.invoke(null)
      }
    }
  }

  private fun SdkLookupBuilder.runWithProgress(onCancelled: () -> Unit,
                                               action: (ProgressIndicator) -> Unit) {
    val title = progressMessageTitle ?: "Resolving" + (sdkType?.presentableName ?: "SDK") + "..."
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, true, ALWAYS_BACKGROUND) {
      override fun run(indicator: ProgressIndicator) {
        action(indicator)
      }

      override fun onCancel() {
        onCancelled()
      }
    })
  }
}
