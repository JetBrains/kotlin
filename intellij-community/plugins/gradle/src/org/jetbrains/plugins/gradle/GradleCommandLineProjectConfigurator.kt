// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.CommandLineInspectionProgressReporter
import com.intellij.codeInspection.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.util.io.exists
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.nio.file.Path

/**
 * @author yole
 */
class GradleCommandLineProjectConfigurator : CommandLineInspectionProjectConfigurator {
  private var initialImport = false

  override fun isApplicable(projectPath: Path, logger: CommandLineInspectionProgressReporter): Boolean {
    return GradleConstants.KNOWN_GRADLE_FILES.any { projectPath.resolve(it).exists() }
  }

  override fun configureEnvironment(projectPath: Path, logger: CommandLineInspectionProgressReporter) {
    initialImport = !projectPath.resolve(".idea").exists()
  }

  override fun configureProject(project: Project, scope: AnalysisScope, logger: CommandLineInspectionProgressReporter) {
    val basePath = project.basePath
    if (!initialImport && basePath != null) {
      ExternalSystemUtil.refreshProject(basePath,
                                        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID).use(ProgressExecutionMode.MODAL_SYNC))
    }
  }
}
