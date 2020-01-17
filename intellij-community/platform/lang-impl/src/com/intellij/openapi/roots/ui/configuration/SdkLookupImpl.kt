// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.UnknownSdkTracker
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.util.Consumer
import org.jetbrains.annotations.Nls
import java.util.function.Predicate

private data class SdkLookupBuilderImpl(
  val project: Project? = null,

  val progressMessageTitle: String? = null,
  val progressIndicator: ProgressIndicator? = null,

  val sdkName: String? = null,
  val sdkUseProjectSdk : Boolean = false,

  val sdkType: SdkType? = null,
  val sdkMinVersionInclusive: String? = null,
  val sdkMaxVersionExclusive: String? = null,

  val onBeforeSdkSuggestionStarted: () -> Boolean = { true },
  val onLocalSdkSuggested: (UnknownSdkLocalSdkFix) -> Boolean = { true },
  val onDownloadableSdkSuggested: (UnknownSdkDownloadableSdkFix) -> Boolean = { true },

  val sdkHomeFilter: (String) -> Boolean = { true },
  val onSdkResolved: (Sdk?) -> Unit = { }
) : SdkLookupBuilder {
  override fun withProject(project: Project?) = copy(project = project)
  override fun withProgressMessageTitle(@Nls message: String) = copy(progressMessageTitle = message)
  override fun withSdkName(name: String) = copy(sdkName = name)
  override fun withProjectSdk() = copy(sdkUseProjectSdk = true)
  override fun withSdkType(sdkType: SdkType) = copy(sdkType = sdkType)
  override fun withMinSdkVersionInclusive(version: String) = copy(sdkMinVersionInclusive = version)
  override fun withMaxSdkVersionExclusive(version: String) = copy(sdkMaxVersionExclusive = version)
  override fun withSdkHomeFilter(filter: (String) -> Boolean) = copy(sdkHomeFilter = filter)
  override fun onBeforeSdkSuggestionStarted(handler: () -> Boolean) = copy(onBeforeSdkSuggestionStarted = handler)

  override fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> Boolean) = copy(onLocalSdkSuggested = handler)
  override fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> Boolean) = copy(onDownloadableSdkSuggested = handler)
  override fun onSdkResolved(handler: (Sdk?) -> Unit) = copy(onSdkResolved = handler)
  override fun executeLookup() = service<SdkLookup>().lookup(copy())
  override fun withProgressIndicator(indicator: ProgressIndicator) = copy(progressIndicator = indicator)

  fun sdkHomeFilter(sdk: Sdk): Boolean {
    val sdkHome = sdk.homePath ?: return false
    return sdkHomeFilter(sdkHome)
  }
}

internal class SdkLookupImpl : SdkLookup {
  override fun createBuilder() : SdkLookupBuilder = SdkLookupBuilderImpl()

  override fun lookup(lookup: SdkLookupBuilder): Unit = (lookup as SdkLookupBuilderImpl).copy().run {
    val rootProgressIndicator = ProgressIndicatorBase()

    if (progressIndicator is ProgressIndicatorEx) {
      rootProgressIndicator.addStateDelegate(progressIndicator)
    }

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

    if (sdk != null && (sdkType == null || sdk.sdkType == sdkType)) {
      val disposable = Disposable {}

      val onDownloadCompleted = Consumer<Boolean> { onSucceeded ->
        Disposer.dispose(disposable)

        if (onSucceeded && sdkHomeFilter(sdk)) {
          onSdkResolved(sdk)
        } else {
          continueSdkLookupWithSuggestions(rootProgressIndicator)
        }
      }

      val isDownloading = SdkDownloadTracker
        .getInstance()
        .tryRegisterDownloadingListener(
          sdk,
          disposable,
          rootProgressIndicator,
          onDownloadCompleted)

      if (isDownloading) {
        return@run
      }

      if (sdkHomeFilter(sdk)) {
        onSdkResolved(sdk)
        return@run
      }
    }

    continueSdkLookupWithSuggestions(rootProgressIndicator)
  }

  private fun SdkLookupBuilderImpl.continueSdkLookupWithSuggestions(rootProgressIndicator: ProgressIndicatorBase) {
    if (sdkType == null) {
      //it is not possible to suggest everything, if [sdkType] is not specified
      onSdkResolved.invoke(null)
      return
    }

    val versionPredicate = if (sdkMinVersionInclusive != null || sdkMaxVersionExclusive != null) {
      val vCmp = sdkType.versionStringComparator()
      Predicate<String> { version ->
        ((sdkMinVersionInclusive == null || vCmp.compare(sdkMinVersionInclusive, version) <= 0))
        &&
        ((sdkMaxVersionExclusive == null || vCmp.compare(version, sdkMaxVersionExclusive) > 0))
      }
    } else {
      Predicate { true }
    }

    if (!onBeforeSdkSuggestionStarted()) {
      onSdkResolved(null)
      return
    }

    val unknownSdk = object: UnknownSdk {
      override fun getSdkType() : SdkType = this@continueSdkLookupWithSuggestions.sdkType
      override fun getSdkVersionStringPredicate() = versionPredicate
      override fun toString() = "SdkLookup{${sdkType.presentableName}, version in [$sdkMinVersionInclusive, $sdkMaxVersionExclusive) }"
    }

    runWithProgress(rootProgressIndicator, onCancelled = { onSdkResolved(null) }) { indicator ->
      try {
        val resolvers = UnknownSdkResolver.EP_NAME.iterable
          .mapNotNull { it.createResolver(project, indicator) }

        indicator.checkCanceled()

        val localFix = resolvers
          .asSequence()
          .mapNotNull { it.proposeLocalFix(unknownSdk, indicator) }
          .firstOrNull()

        indicator.checkCanceled()

        if (localFix != null && onLocalSdkSuggested.invoke(localFix) && sdkHomeFilter(localFix.existingSdkHome)) {
          UnknownSdkTracker.configureLocalSdk(unknownSdk, localFix, Consumer {
            if (it != null && sdkHomeFilter(it)) {
              onSdkResolved(it)
            } else {
              onSdkResolved(null)
            }
          })
          return@runWithProgress
        }

        indicator.checkCanceled()

        val downloadFix = resolvers
          .asSequence()
          .mapNotNull { it.proposeDownload(unknownSdk, indicator) }
          .firstOrNull()

        indicator.checkCanceled()

        if (downloadFix != null && onDownloadableSdkSuggested.invoke(downloadFix)) {
          UnknownSdkTracker.downloadFix(project, unknownSdk, downloadFix, Consumer { sdk ->
            if (sdk != null && sdkHomeFilter(sdk)) {
              onSdkResolved(sdk)
            } else {
              onSdkResolved(null)
            }
          })
          return@runWithProgress
        }

        onSdkResolved(null)
      } catch (e: ProcessCanceledException) {
        throw e
      } catch (t: Throwable) {
        Logger
          .getInstance(SdkLookupImpl::class.java)
          .warn("Failed to resolve SDK for ${this@continueSdkLookupWithSuggestions}. ${t.message}", t)

        onSdkResolved(null)
      }
    }
  }

  private fun SdkLookupBuilderImpl.runWithProgress(rootProgressIndicator: ProgressIndicatorBase,
                                                   onCancelled: () -> Unit,
                                                   action: (ProgressIndicator) -> Unit) {
    val title = progressMessageTitle ?: "Resolving" + (sdkType?.presentableName ?: "SDK") + "..."
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, true, ALWAYS_BACKGROUND) {
      override fun run(indicator: ProgressIndicator) {
        val middleMan = object : ProgressIndicatorBase() {
          override fun delegateProgressChange(action: IndicatorAction) {
            action.execute(indicator as ProgressIndicatorEx)
          }
        }

        rootProgressIndicator.addStateDelegate(middleMan)

        try {
          action(indicator)
        } finally {
          rootProgressIndicator.removeStateDelegate(middleMan)
        }
      }

      override fun onCancel() {
        onCancelled()
      }
    })
  }
}
