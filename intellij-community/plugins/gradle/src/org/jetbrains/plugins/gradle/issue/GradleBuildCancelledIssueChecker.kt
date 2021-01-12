// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler.getRootCauseAndLocation
import java.util.function.Consumer

/**
 * This issue checker provides common handling of the errors caused by build cancellation.
 *
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleBuildCancelledIssueChecker : GradleIssueChecker {

  override fun check(issueData: GradleIssueData): BuildIssue? {
    if (issueData.error !is ProcessCanceledException) {
      val rootCause = getRootCauseAndLocation(issueData.error).first
      if (!rootCause.toString().contains("Build cancelled.")) return null
    }

    val description = "Build cancelled"
    val title = "Build cancelled"
    return object : BuildIssue {
      override val title: String = title
      override val description: String = description
      override val quickFixes = emptyList<BuildIssueQuickFix>()
      override fun getNavigatable(project: Project): Navigatable? = null
    }
  }

  override fun consumeBuildOutputFailureMessage(message: String,
                                                failureCause: String,
                                                stacktrace: String?,
                                                location: FilePosition?,
                                                parentEventId: Any,
                                                messageConsumer: Consumer<in BuildEvent>): Boolean {
    // Build cancellation errors should be handled by GradleBuildCancelledIssueChecker.check method based on exceptions come from Gradle TAPI
    if (failureCause.contains("Build cancelled.")) return true
    return false
  }
}
