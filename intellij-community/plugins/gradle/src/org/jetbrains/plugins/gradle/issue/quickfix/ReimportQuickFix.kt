// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue.quickfix

import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.concurrent.CompletableFuture

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class ReimportQuickFix(private val myProjectPath: String) : BuildIssueQuickFix {
  override val id: String = "reimport"
  override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> = requestImport(project, myProjectPath)

  companion object {
    fun requestImport(project: Project, projectPath: String): CompletableFuture<Nothing> {
      val future = CompletableFuture<Nothing>()
      ExternalSystemUtil.refreshProject(projectPath, ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
        .callback(object : ExternalProjectRefreshCallback {
          override fun onSuccess(externalProject: DataNode<ProjectData>?) {
            future.complete(null)
          }

          override fun onFailure(errorMessage: String, errorDetails: String?) {
            future.completeExceptionally(RuntimeException(errorMessage))
          }
        })
      )
      return future
    }
  }
}
