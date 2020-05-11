// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.intellij.analysis.AnalysisScope
import com.intellij.ide.CommandLineInspectionProgressReporter
import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

class SdkConfigurator : CommandLineInspectionProjectConfigurator {
  override fun isApplicable(projectPath: Path, logger: CommandLineInspectionProgressReporter): Boolean {
    return !ApplicationManager.getApplication().isUnitTestMode
  }

  override fun configureEnvironment(projectPath: Path, logger: CommandLineInspectionProgressReporter) {
    Registry.get("unknown.sdk").setValue(false) //forbid UnknownSdkTracker post startup activity
  }

  override fun configureProject(project: Project,
                                scope: AnalysisScope,
                                logger: CommandLineInspectionProgressReporter) {
    runBlocking {
      resolveUnknownSdks(project, ProgressIndicatorBase())
    }
  }
}
