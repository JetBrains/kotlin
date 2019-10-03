// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.BuildConsoleUtils.getMessageTitle
import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.build.issue.quickfix.OpenFileQuickFix
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.util.PlatformUtils
import com.intellij.util.io.isFile
import org.gradle.initialization.BuildLayoutParameters
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.issue.quickfix.GradleSettingsQuickFix
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler.getRootCauseAndLocation
import org.jetbrains.plugins.gradle.settings.GradleSystemSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import java.nio.file.Paths
import java.util.*
import java.util.function.BiPredicate

/**
 * This issue checker provides quick fixes to deal with known startup issues of the Gradle daemon.
 *
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleDaemonStartupIssueChecker : GradleIssueChecker {

  override fun check(issueData: GradleIssueData): BuildIssue? {
    val rootCause = getRootCauseAndLocation(issueData.error).first
    val rootCauseText = rootCause.toString()
    if (!rootCauseText.startsWith("org.gradle.api.GradleException: Unable to start the daemon process.")) {
      return null
    }

    // JDK compatibility issues should be handled by org.jetbrains.plugins.gradle.issue.IncompatibleGradleJdkIssueChecker
    if(rootCauseText.contains("FAILURE: Build failed with an exception.")) {
      return null
    }

    val quickFixDescription = StringBuilder()
    val quickFixes = ArrayList<BuildIssueQuickFix>()
    val projectGradleProperties = Paths.get(issueData.projectPath, "gradle.properties")
    if (projectGradleProperties.isFile()) {
      val openFileQuickFix = OpenFileQuickFix(projectGradleProperties, "org.gradle.jvmargs")
      quickFixDescription.append(" - <a href=\"${openFileQuickFix.id}\">gradle.properties</a> in project root directory\n")
      quickFixes.add(openFileQuickFix)
    }

    val gradleUserHomeDir = BuildLayoutParameters().gradleUserHomeDir
    val commonGradleProperties = Paths.get(gradleUserHomeDir.path, "gradle.properties")
    if (commonGradleProperties.isFile()) {
      val openFileQuickFix = OpenFileQuickFix(commonGradleProperties, "org.gradle.jvmargs")
      quickFixDescription.append(" - <a href=\"${openFileQuickFix.id}\">gradle.properties</a> in in GRADLE_USER_HOME directory\n")
      quickFixes.add(openFileQuickFix)
    }

    val gradleVmOptions = GradleSystemSettings.getInstance().gradleVmOptions
    if (!gradleVmOptions.isNullOrBlank() && "AndroidStudio" != PlatformUtils.getPlatformPrefix()) { // Android Studio doesn't have Gradle VM options setting
      val gradleSettingsFix = GradleSettingsQuickFix(
        issueData.projectPath, true,
        BiPredicate { _, _ -> gradleVmOptions != GradleSystemSettings.getInstance().gradleVmOptions },
        GradleBundle.message("gradle.settings.text.vm.options")
      )
      quickFixes.add(gradleSettingsFix)
      quickFixDescription.append(" - <a href=\"${gradleSettingsFix.id}\">IDE Gradle VM options</a> \n")
    }

    val issueDescription = StringBuilder(rootCause.message)
    if (quickFixDescription.isNotEmpty()) {
      issueDescription.append("\n-----------------------\n")
      issueDescription.append("Check the JVM arguments defined for the gradle process in:\n")
      issueDescription.append(quickFixDescription)
    }

    val description = issueDescription.toString()
    val title = getMessageTitle(description)
    return object : BuildIssue {
      override val title: String = title
      override val description: String = description
      override val quickFixes = quickFixes
      override fun getNavigatable(project: Project): Navigatable? = null
    }
  }
}
