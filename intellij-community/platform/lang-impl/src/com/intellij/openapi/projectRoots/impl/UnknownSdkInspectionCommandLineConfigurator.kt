// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.intellij.ide.CommandLineInspectionProgressReporter
import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.UnknownSdk
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Consumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.file.Path
import kotlin.coroutines.resume

class UnknownSdkInspectionCommandLineConfigurator : CommandLineInspectionProjectConfigurator {
  private val LOG: Logger = logger<UnknownSdkInspectionCommandLineConfigurator>()

  override fun isApplicable(projectPath: Path, logger: CommandLineInspectionProgressReporter): Boolean {
    return !ApplicationManager.getApplication().isUnitTestMode
  }

  override fun configureEnvironment(projectPath: Path, logger: CommandLineInspectionProgressReporter) {
    Registry.get("unknown.sdk").setValue(false) //forbid UnknownSdkTracker post startup activity
  }

  override fun configureProject(project: Project,
                                logger: CommandLineInspectionProgressReporter
  ) {
    runBlocking {
      resolveUnknownSdks(project, ProgressIndicatorBase())
    }
  }

  private suspend fun resolveUnknownSdks(project: Project, indicator: ProgressIndicator) {
    val problems = suspendCancellableCoroutine<List<UnknownSdk>> { cont ->
      UnknownSdkCollector(project).collectSdksPromise(java.util.function.Consumer { (_, resolvableSdks) ->
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
                                        project: Project
  ) {
    val localFix = resolvers.asSequence().mapNotNull { it.proposeLocalFix(problem, indicator) }.firstOrNull()
    if (localFix != null) {
      LOG.info("Found local fix for $problem: ${localFix.existingSdkHome} (${localFix.presentableVersionString})")
      suspendCancellableCoroutine<Unit> { cont ->
        invokeAndWaitIfNeeded {
          UnknownSdkTracker.configureLocalSdk(problem, localFix, Consumer { cont.resume(Unit) })
        }
      }
      return
    }

    val remoteFix = resolvers.asSequence().mapNotNull { it.proposeDownload(problem, indicator) }.firstOrNull()
    if (remoteFix != null) {
      LOG.info("Found remote fix for $problem: ${remoteFix.downloadDescription} (${remoteFix.presentableVersionString})")
      suspendCancellableCoroutine<Unit> { cont ->
        invokeAndWaitIfNeeded {
          UnknownSdkTracker.downloadFix(project, problem, remoteFix, Consumer {}, Consumer { cont.resume(Unit) })
        }
      }
      return
    }

    LOG.warn("No SDK found for $problem")
  }
}
