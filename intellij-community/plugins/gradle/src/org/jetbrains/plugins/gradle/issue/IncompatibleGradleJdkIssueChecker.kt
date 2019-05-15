// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue

import com.intellij.build.issue.BuildIssue
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.util.PlatformUtils.getPlatformPrefix
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.issue.quickfix.GradleSettingsQuickFix
import org.jetbrains.plugins.gradle.issue.quickfix.GradleVersionQuickFix
import org.jetbrains.plugins.gradle.issue.quickfix.GradleWrapperSettingsOpenQuickFix
import org.jetbrains.plugins.gradle.issue.quickfix.ReimportQuickFix
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionErrorHandler.getRootCauseAndLocation
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.util.*
import java.util.function.BiPredicate

/**
 * This issue checker provides quick fixes for known compatibility issues with Gradle and Java:
 * 1. Gradle versions less than 4.7 do not support JEP-322 (Java starting with JDK 10-ea build 36), see https://github.com/gradle/gradle/issues/4503
 * 2. Gradle versions less than 4.8 fails on JDK 11+ (due to dependency on Unsafe::defineClass which is removed in JDK 11), see https://github.com/gradle/gradle/issues/4860
 * 3. Gradle versions less than 4.7 can not be used by the IDE running on Java 9+, see https://github.com/gradle/gradle/issues/8431, https://github.com/gradle/gradle/issues/3355
 *
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class IncompatibleGradleJdkIssueChecker : GradleIssueChecker {

  override fun check(issueData: GradleIssueData): BuildIssue? {
    val rootCause = getRootCauseAndLocation(issueData.error).first
    val rootCauseText = rootCause.toString()
    val isToolingClientIssue = rootCauseText.startsWith("java.lang.IllegalArgumentException: Could not determine java version from")
    if (!isToolingClientIssue && !rootCauseText.startsWith(
        "org.gradle.api.GradleException: Could not determine Java version using executable")) {
      return null
    }

    val quickFixes: MutableList<BuildIssueQuickFix>
    quickFixes = ArrayList()
    val issueDescription = StringBuilder(rootCause.message)
    var gradleVersionUsed: GradleVersion? = null
    val gradleMinimumVersionRequired = GradleVersion.version("4.8.1")
    if (issueData.buildEnvironment != null) {
      gradleVersionUsed = GradleVersion.version(issueData.buildEnvironment.gradle.gradleVersion)
    }

    val gradleVersionString = if (gradleVersionUsed != null) gradleVersionUsed.version else "version"
    if (isToolingClientIssue) {
      issueDescription.append(
        "\n\nThe project uses Gradle $gradleVersionString which is incompatible with IDE running under Java 10 or newer.")
    }
    else {
      issueDescription.append("\n\nThe project uses Gradle $gradleVersionString which is incompatible with Java 10 or newer.")
    }
    issueDescription.append("\nYou can:\n")
    val wrapperPropertiesFile = GradleUtil.findDefaultWrapperPropertiesFile(issueData.projectPath)
    if (wrapperPropertiesFile == null || isToolingClientIssue || gradleVersionUsed != null && gradleVersionUsed.baseVersion < gradleMinimumVersionRequired) {
      val gradleVersionFix = GradleVersionQuickFix(issueData.projectPath, gradleMinimumVersionRequired, true)
      issueDescription.append(" - <a href=\"${gradleVersionFix.id}\">Upgrade Gradle wrapper to ${gradleMinimumVersionRequired.version} version " +
                              "and re-import the project</a>\n")
      quickFixes.add(gradleVersionFix)
    }
    else {
      val wrapperSettingsOpenQuickFix = GradleWrapperSettingsOpenQuickFix(issueData.projectPath, "distributionUrl")
      val reimportQuickFix = ReimportQuickFix(issueData.projectPath)
      issueDescription.append(" - <a href=\"${wrapperSettingsOpenQuickFix.id}\">Open Gradle wrapper settings</a>, " +
                              "upgrade version to 4.8.1 or newer and <a href=\"${reimportQuickFix.id}\">reimport the project</a>\n")
      quickFixes.add(wrapperSettingsOpenQuickFix)
      quickFixes.add(reimportQuickFix)
    }
    if (!isToolingClientIssue && "AndroidStudio" != getPlatformPrefix()) { // Android Studio doesn't have Gradle JVM setting
      val gradleSettingsFix = GradleSettingsQuickFix(
        issueData.projectPath, true,
        BiPredicate { oldSettings, currentSettings ->
          oldSettings.gradleJvm != currentSettings.gradleJvm
        },
        GradleBundle.message("gradle.settings.text.jvm.path")
      )
      quickFixes.add(gradleSettingsFix)
      issueDescription.append(" - Use Java 8 as Gradle JVM: <a href=\"${gradleSettingsFix.id}\">Fix Gradle settings</a> \n")
    }

    return object : BuildIssue {
      override val description: String = issueDescription.toString()
      override val quickFixes = quickFixes
    }
  }
}
