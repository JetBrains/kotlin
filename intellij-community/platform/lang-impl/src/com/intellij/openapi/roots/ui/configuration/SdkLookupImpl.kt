// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.progress.util.ProgressIndicatorListenerAdapter
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.UnknownSdkTracker
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.util.Consumer
import org.jetbrains.annotations.Nls
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

private data class SdkLookupBuilderImpl(
  val project: Project? = null,

  val progressMessageTitle: String? = null,
  val progressIndicator: ProgressIndicator? = null,

  val sdkName: String? = null,

  val sdkType: SdkType? = null,

  val onBeforeSdkSuggestionStarted: () -> Boolean = { true },
  val onLocalSdkSuggested: (UnknownSdkLocalSdkFix) -> Boolean = { true },
  val onDownloadableSdkSuggested: (UnknownSdkDownloadableSdkFix) -> Boolean = { true },

  val sdkHomeFilter: ((String) -> Boolean)? = null,
  val versionFilter: ((String) -> Boolean)? = null,

  val testSdkSequence: Sequence<Sdk?> = emptySequence(),

  private val onSdkNameResolved: (Sdk?) -> Unit = { },
  private val onSdkResolved: (Sdk?) -> Unit = { }
) : SdkLookupBuilder {
  override fun withProject(project: Project?) = copy(project = project)
  override fun withProgressMessageTitle(@Nls message: String) = copy(progressMessageTitle = message)
  override fun withSdkName(name: String) = copy(sdkName = name)
  override fun withSdkType(sdkType: SdkType) = copy(sdkType = sdkType)
  override fun withVersionFilter(filter: (String) -> Boolean) = copy(versionFilter = filter)
  override fun withSdkHomeFilter(filter: (String) -> Boolean) = copy(sdkHomeFilter = filter)
  override fun withProgressIndicator(indicator: ProgressIndicator) = copy(progressIndicator = indicator)

  override fun testSuggestedSdksFirst(sdks: Sequence<Sdk?>) = copy(testSdkSequence = testSdkSequence + sdks)

  override fun onBeforeSdkSuggestionStarted(handler: () -> SdkLookupDecision) = copy(onBeforeSdkSuggestionStarted = { handler() == SdkLookupDecision.CONTINUE })
  override fun onLocalSdkSuggested(handler: (UnknownSdkLocalSdkFix) -> SdkLookupDecision) = copy(onLocalSdkSuggested = { handler(it) == SdkLookupDecision.CONTINUE})
  override fun onDownloadableSdkSuggested(handler: (UnknownSdkDownloadableSdkFix) -> SdkLookupDecision) = copy(onDownloadableSdkSuggested = { handler(it) == SdkLookupDecision.CONTINUE})

  override fun onSdkResolved(handler: (Sdk?) -> Unit) = copy(onSdkResolved = handler)
  override fun onSdkNameResolved(callback: (Sdk?) -> Unit) = copy(onSdkNameResolved = callback)

  override fun executeLookup() = service<SdkLookup>().lookup(copy())

  private val sdkNameCallbackExecuted = AtomicBoolean(false)
  private val sdkCallbackExecuted = AtomicBoolean(false)

  val onSdkNameResolvedConsumer = Consumer<Sdk?> { onSdkResolved(it) }
  val onSdkResolvedConsumer = Consumer<Sdk?> { onSdkResolved(it) }

  fun onSdkNameResolved(sdk: Sdk?) {
    if (!sdkNameCallbackExecuted.compareAndSet(false, true)) return
    onSdkNameResolved.invoke(sdk)
  }

  fun onSdkResolved(sdk: Sdk?) {
    onSdkNameResolved(sdk)

    if (!sdkCallbackExecuted.compareAndSet(false, true)) return

    if (sdk != null && !checkSdkHomeAndVersion(sdk)) {
      onSdkResolved.invoke(null)
    } else {
      onSdkResolved.invoke(sdk)
    }
  }

  fun checkSdkHomeAndVersion(sdk: Sdk?): Boolean {
    val sdkHome = sdk?.homePath ?: return false
    return sdkHomeFilter?.invoke(sdkHome) != false && checkSdkVersion(sdk)
  }

  fun checkSdkVersion(sdk: Sdk?) : Boolean {
    val versionString = sdk?.versionString ?: return false
    return versionFilter?.invoke(versionString) != false
  }
}

internal class SdkLookupImpl : SdkLookup {
  private val LOG = logger<SdkLookupImpl>()

  override fun createBuilder() : SdkLookupBuilder = SdkLookupBuilderImpl()

  override fun lookup(lookup: SdkLookupBuilder): Unit = (lookup as SdkLookupBuilderImpl).copy().run {
    val rootProgressIndicator = ProgressIndicatorBase()

    if (progressIndicator is ProgressIndicatorEx) {
      rootProgressIndicator.addStateDelegate(progressIndicator)
    }

    val sdk = sequence {
      val namedSdk = runReadAction {
        sdkName?.let {
          when (sdkType) {
            null -> ProjectJdkTable.getInstance().findJdk(sdkName)
            else -> ProjectJdkTable.getInstance().findJdk(sdkName, sdkType.name)
          }
        }
      }

      //include currently downloading Sdks
      yieldAll(SdkDownloadTracker.getInstance().findDownloadingSdks(sdkName))

      yield(namedSdk)

      yieldAll(testSdkSequence)
    }
      .filterNotNull()
      .filter { candidate -> sdkType == null || candidate.sdkType == sdkType }
      .filter { checkSdkVersion(it) }
      .firstOrNull()

    if (sdk != null) {
      val disposable = Disposable {}
      val onDownloadCompleted = Consumer<Boolean> { onSucceeded ->
        Disposer.dispose(disposable)

        val finalSdk = when {
          onSucceeded && checkSdkHomeAndVersion(sdk) ->  sdk
          else -> null
        }

        onSdkResolved(finalSdk)
      }

      val isDownloading = SdkDownloadTracker
        .getInstance()
        .tryRegisterDownloadingListener(
          sdk,
          disposable,
          rootProgressIndicator,
          onDownloadCompleted)

      if (isDownloading) {
        return@run onSdkNameResolved(sdk)
      }

      Disposer.dispose(disposable)
      if (checkSdkHomeAndVersion(sdk)) {
        return@run onSdkResolved(sdk)
      }
    }

    continueSdkLookupWithSuggestions(rootProgressIndicator)
  }

  private fun SdkLookupBuilderImpl.continueSdkLookupWithSuggestions(rootProgressIndicator: ProgressIndicatorBase) {
    if (sdkType == null) {
      //it is not possible to suggest everything, if [sdkType] is not specified
      onSdkResolved(null)
      return
    }

    if (!onBeforeSdkSuggestionStarted()) {
      onSdkResolved(null)
      return
    }

    val unknownSdk = object: UnknownSdk {
      val versionPredicate = versionFilter?.let { Predicate<String> { versionFilter.invoke(it) } }

      override fun getSdkName() = this@continueSdkLookupWithSuggestions.sdkName
      override fun getSdkType() : SdkType = this@continueSdkLookupWithSuggestions.sdkType
      override fun getSdkVersionStringPredicate() = versionPredicate
      override fun getSdkHomePredicate() = sdkHomeFilter?.let { filter -> Predicate<String> { path -> filter(path) } }
      override fun toString() = "SdkLookup{${sdkType.presentableName}, ${versionPredicate} }"
    }

    runWithProgress(rootProgressIndicator, onCancelled = { onSdkResolved(null) }) { indicator ->
      try {
        val resolvers = UnknownSdkResolver.EP_NAME.iterable
          .mapNotNull { it.createResolver(project, indicator) }

        indicator.checkCanceled()

        if (tryLocalFix(resolvers, unknownSdk, indicator)) return@runWithProgress

        indicator.checkCanceled()

        if (tryDownloadableFix(resolvers, unknownSdk, indicator)) return@runWithProgress

        indicator.checkCanceled()

        onSdkResolved(null)
      } catch (e: ProcessCanceledException) {
        onSdkResolved(null)
        throw e
      } catch (t: Throwable) {
        LOG.warn("Failed to resolve SDK for ${this@continueSdkLookupWithSuggestions}. ${t.message}", t)

        onSdkResolved(null)
      }
    }
  }

  private fun SdkLookupBuilderImpl.tryLocalFix(resolvers: List<UnknownSdkLookup>,
                                               unknownSdk: UnknownSdk,
                                               indicator: ProgressIndicator): Boolean {
    val localFix = resolvers
                     .asSequence()
                     .mapNotNull { it.proposeLocalFix(unknownSdk, indicator) }
                     .filter { onLocalSdkSuggested.invoke(it) }
                     .filter { versionFilter?.invoke(it.versionString) != false }
                     .filter { sdkHomeFilter?.invoke(it.existingSdkHome) != false }
                     .firstOrNull() ?: return false

    indicator.checkCanceled()
    UnknownSdkTracker.configureLocalSdk(unknownSdk, localFix, onSdkResolvedConsumer)
    return true
  }

  private fun SdkLookupBuilderImpl.tryDownloadableFix(resolvers: List<UnknownSdkLookup>,
                                                      unknownSdk: UnknownSdk,
                                                      indicator: ProgressIndicator): Boolean {
    val downloadFix = resolvers
                        .asSequence()
                        .mapNotNull { it.proposeDownload(unknownSdk, indicator) }
                        .filter { onDownloadableSdkSuggested.invoke(it) }
                        .filter { versionFilter?.invoke(it.versionString) != false }
                        .firstOrNull() ?: return false

    indicator.checkCanceled()
    UnknownSdkTracker.downloadFix(project, unknownSdk, downloadFix, onSdkNameResolvedConsumer, onSdkResolvedConsumer)
    return true
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

        object: ProgressIndicatorListenerAdapter() {
          override fun cancelled() {
            rootProgressIndicator.cancel()
          }
        }.installToProgress(indicator as ProgressIndicatorEx)

        rootProgressIndicator.addStateDelegate(middleMan)

        try {
          action(rootProgressIndicator)
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
