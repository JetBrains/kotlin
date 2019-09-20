// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.issue.quickfix.ReimportQuickFix.Companion.requestImport
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.util.concurrent.CompletableFuture

@ApiStatus.Internal
abstract class UnresolvedDependencyIssue(dependencyName: String) : BuildIssue {
  override val title: String = "Could not resolve $dependencyName"
  override fun getNavigatable(project: Project): Navigatable? = null

  fun buildDescription(failureMessage: String?, isOfflineMode: Boolean, offlineModeQuickFixText: String): String {
    val issueDescription = StringBuilder(failureMessage?.trim())
    val noRepositoriesDefined = failureMessage?.contains("no repositories are defined") ?: false

    issueDescription.append("\n\nPossible solution:\n")
    when {
      isOfflineMode && !noRepositoriesDefined -> issueDescription.append(
        " - <a href=\"$offlineQuickFixId\">$offlineModeQuickFixText</a>\n")
      else -> issueDescription.append(
        " - Declare repository providing the artifact, see the documentation at $declaringRepositoriesLink\n")
    }
    return issueDescription.toString()
  }

  companion object {
    internal const val offlineQuickFixId = "disable_offline_mode"
    private const val declaringRepositoriesLink = "https://docs.gradle.org/current/userguide/declaring_repositories.html"
  }
}

@ApiStatus.Experimental
class UnresolvedDependencySyncIssue(dependencyName: String,
                                    failureMessage: String?,
                                    projectPath: String,
                                    isOfflineMode: Boolean) : UnresolvedDependencyIssue(dependencyName) {
  override val quickFixes = if (isOfflineMode) listOf<BuildIssueQuickFix>(DisableOfflineAndReimport(projectPath)) else emptyList()
  override val description: String = buildDescription(failureMessage, isOfflineMode, "Disable offline mode and reimport the project")

  inner class DisableOfflineAndReimport(private val projectPath: String) : BuildIssueQuickFix {
    override val id = offlineQuickFixId
    override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
      GradleSettings.getInstance(project).isOfflineWork = false
      val environment = LangDataKeys.EXECUTION_ENVIRONMENT.getData(dataProvider)
      if (environment != null) {
        return CompletableFuture.runAsync { ExecutionUtil.restart(environment) }
      }
      return requestImport(project, projectPath)
    }
  }
}

@ApiStatus.Experimental
class UnresolvedDependencyBuildIssue(dependencyName: String,
                                     failureMessage: String?,
                                     isOfflineMode: Boolean) : UnresolvedDependencyIssue(dependencyName) {
  override val quickFixes = if (isOfflineMode) listOf<BuildIssueQuickFix>(DisableOfflineAndReimport()) else emptyList()
  override val description: String = buildDescription(failureMessage, isOfflineMode, "Disable offline mode and rerun the build")

  inner class DisableOfflineAndReimport : BuildIssueQuickFix {
    override val id = offlineQuickFixId
    override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
      GradleSettings.getInstance(project).isOfflineWork = false
      val environment = LangDataKeys.EXECUTION_ENVIRONMENT.getData(dataProvider)
      if (environment != null) {
        return CompletableFuture.runAsync { ExecutionUtil.restart(environment) }
      }
      return CompletableFuture.completedFuture(null)
    }
  }
}