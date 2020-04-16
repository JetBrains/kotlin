// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.UnknownSdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import kotlin.coroutines.resume
import kotlinx.coroutines.*
import java.util.function.Consumer

private val LOG: Logger = Logger.getInstance(SdkConfigurator::class.java)

internal suspend fun resolveUnknownSdks(project: Project, indicator: ProgressIndicator) {
  val problems = suspendCancellableCoroutine<List<UnknownSdk>> { cont ->
    UnknownSdkCollector(project).collectSdksPromise(Consumer { (_, resolvableSdks) ->
      cont.resume(resolvableSdks)
    })
  }

  val resolvers = UnknownSdkResolver.EP_NAME.extensions.mapNotNull {
    it.createResolver(project, indicator)
  }

  for (problem in problems) {
    resolveUnknownSdk(resolvers, problem, indicator, project)
  }
}

private suspend fun resolveUnknownSdk(resolvers: List<UnknownSdkResolver.UnknownSdkLookup>,
                                      problem: UnknownSdk,
                                      indicator: ProgressIndicator,
                                      project: Project) {
  val localFix = resolvers.asSequence().mapNotNull { it.proposeLocalFix(problem, indicator) }.firstOrNull()
  if (localFix != null) {
    LOG.info("Found local fix for $problem: ${localFix.existingSdkHome} (${localFix.versionString})")
    suspendCancellableCoroutine<Unit> { cont ->
      invokeAndWaitIfNeeded {
        UnknownSdkTracker.configureLocalSdk(problem, localFix, com.intellij.util.Consumer { cont.resume(Unit) })
      }
    }
    return
  }

  val remoteFix = resolvers.asSequence().mapNotNull { it.proposeDownload(problem, indicator) }.firstOrNull()
  if (remoteFix != null) {
    LOG.info("Found remote fix for $problem: ${remoteFix.downloadDescription} (${remoteFix.versionString})")
    suspendCancellableCoroutine<Unit> { cont ->
      invokeAndWaitIfNeeded {
        UnknownSdkTracker.downloadFix(project, problem, remoteFix, com.intellij.util.Consumer {},
                                      com.intellij.util.Consumer { cont.resume(Unit) })
      }
    }
    return
  }

  LOG.warn("No SDK found for $problem")
}