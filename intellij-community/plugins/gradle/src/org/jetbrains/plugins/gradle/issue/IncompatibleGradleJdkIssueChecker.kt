// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueChecker
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.util.PlatformUtils.getPlatformPrefix
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.issue.quickfix.GradleSettingsQuickFix
import org.jetbrains.plugins.gradle.issue.quickfix.GradleVersionQuickFix
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.util.*
import java.util.function.BiPredicate

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class IncompatibleGradleJdkIssueChecker : BuildIssueChecker<GradleIssueData> {

  override fun check(issueData: GradleIssueData): BuildIssue? {
    val rootCause = issueData.rootCause
    if (!rootCause.toString().startsWith("org.gradle.api.GradleException: Could not determine Java version using executable")) {
      return null
    }

    val myQuickFixes: MutableList<BuildIssueQuickFix>
    myQuickFixes = ArrayList()
    val issueDescription = StringBuilder(rootCause.message)
    var gradleVersionUsed: GradleVersion? = null
    val gradleMinimumVersionRequired = GradleVersion.version("4.7")
    if (issueData.buildEnvironment != null) {
      gradleVersionUsed = GradleVersion.version(issueData.buildEnvironment.gradle.gradleVersion)
    }

    val gradleVersionString = if (gradleVersionUsed != null) gradleVersionUsed.version else "version"
    issueDescription.append("\n\nThe project uses Gradle $gradleVersionString which is incompatible with Java 10 or newer." +
                            "\nYou can:\n")
    if (gradleVersionUsed != null && gradleVersionUsed.baseVersion < gradleMinimumVersionRequired) {
      val gradleVersionFix = GradleVersionQuickFix(issueData.projectPath, gradleMinimumVersionRequired, true)
      issueDescription.append(" - <a href=\"${gradleVersionFix.id}\">Upgrade Gradle to 4.7 version and re-import the project</a>\n")
      myQuickFixes.add(gradleVersionFix)
    }
    else {
      val wrapperPropertiesFile = GradleUtil.findDefaultWrapperPropertiesFile(issueData.projectPath)
      if (wrapperPropertiesFile != null) {
        val gradleVersionFix = GradleVersionQuickFix(issueData.projectPath, gradleMinimumVersionRequired, false)
        issueDescription.append(" - <a href=\"${gradleVersionFix.id}\">Open Gradle wrapper settings and upgrade version>\n")
        myQuickFixes.add(gradleVersionFix)
      }
      else {
        issueDescription.append(" - Try upgrade Gradle\n")
      }
    }
    if ("AndroidStudio" != getPlatformPrefix()) { // Android Studio doesn't have Gradle JVM setting
      val gradleSettingsFix = GradleSettingsQuickFix(
        issueData.projectPath, true,
        BiPredicate { oldSettings, currentSettings ->
          oldSettings.gradleJvm != currentSettings.gradleJvm
        },
        GradleBundle.message("gradle.settings.text.jvm.path")
      )
      myQuickFixes.add(gradleSettingsFix)
      issueDescription.append(" - Use Java 8 as Gradle JVM: <a href=\"${gradleSettingsFix.id}\">Fix Gradle settings</a> \n")
    }

    return object : BuildIssue {
      override val description: String = issueDescription.toString()
      override val quickFixes = myQuickFixes
    }
  }
}
