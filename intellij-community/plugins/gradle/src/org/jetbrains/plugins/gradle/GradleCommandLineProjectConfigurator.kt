// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.CommandLineInspectionProgressReporter
import com.intellij.codeInspection.CommandLineInspectionProjectConfigurator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.refreshProject
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil.refreshProjects
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import org.jetbrains.plugins.gradle.settings.GradleImportHintService
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Path

class GradleCommandLineProjectConfigurator : CommandLineInspectionProjectConfigurator {
  private val LOG = Logger.getInstance(GradleManager::class.java)

  private var wasAlreadyImported = false

  override fun isApplicable(projectPath: Path, logger: CommandLineInspectionProgressReporter): Boolean {
    return true
  }

  override fun configureEnvironment(projectPath: Path, logger: CommandLineInspectionProgressReporter) {
    wasAlreadyImported = projectPath.resolve(".idea").exists()
  }

  override fun configureProject(project: Project, scope: AnalysisScope, logger: CommandLineInspectionProgressReporter) {
    val basePath = project.basePath ?: return

    val state = GradleImportHintService.getInstance(project).state

    if (state.skip) return
    if (state.projectsToImport.isNotEmpty()) {
      for (projectPath in state.projectsToImport) {
        val buildFile = File(basePath).toPath().resolve(projectPath).toFile()
        if (buildFile.exists()) {
          refreshProject(buildFile.absolutePath, ImportSpecBuilder(project, GradleConstants.SYSTEM_ID).use(MODAL_SYNC))
        } else {
          LOG.warn("File for importing gradle project doesn't exist: " + buildFile.absolutePath)
        }
      }
      return
    }
    if (wasAlreadyImported && !GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
      refreshProjects(ImportSpecBuilder(project, GradleConstants.SYSTEM_ID).use(MODAL_SYNC))
      return
    }

    val gradleGroovyDslFile = basePath + "/" + GradleConstants.DEFAULT_SCRIPT_NAME
    val kotlinDslGradleFile = basePath + "/" + GradleConstants.KOTLIN_DSL_SCRIPT_NAME
    if (FileUtil.findFirstThatExist(gradleGroovyDslFile, kotlinDslGradleFile) == null) return

    refreshProject(basePath, ImportSpecBuilder(project, GradleConstants.SYSTEM_ID).use(MODAL_SYNC))
  }
}
