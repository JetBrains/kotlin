// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue.quickfix

import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.build.issue.quickfix.OpenFileQuickFix
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleWrapperSettingsOpenQuickFix(private val myProjectPath: String, private val mySearch: String?) : BuildIssueQuickFix {

  override val id: String = "open_gradle_wrapper_settings"

  override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
    showWrapperPropertiesFile(project, myProjectPath, mySearch)
    return completedFuture<Any>(null)
  }

  companion object {
    fun showWrapperPropertiesFile(project: Project, projectPath: String, search: String?) {
      val wrapperPropertiesFile = GradleUtil.findDefaultWrapperPropertiesFile(projectPath) ?: return
      OpenFileQuickFix.showFile(project, wrapperPropertiesFile.toPath(), search)
    }
  }
}
