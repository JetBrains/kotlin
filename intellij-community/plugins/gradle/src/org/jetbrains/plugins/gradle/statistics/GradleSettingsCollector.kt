// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.statistics

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.internal.statistic.utils.getEnumUsage
import com.intellij.openapi.externalSystem.statistics.ExternalSystemUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.gradle.state"

  override fun getUsages(project: Project): Set<UsageDescriptor> {
    val gradleSettings = GradleSettings.getInstance(project)
    val projectsSettings = gradleSettings.linkedProjectsSettings
    if (projectsSettings.isEmpty()) {
      return emptySet()
    }
    val usages = mutableSetOf<UsageDescriptor>()

    // to have a total users base line to calculate pertentages of settings
    usages.add(getBooleanUsage("hasGradleProject", true))

    // global settings
    usages.add(getBooleanUsage("offlineWork", gradleSettings.isOfflineWork))
    usages.add(getBooleanUsage("hasCustomServiceDirectoryPath", !gradleSettings.serviceDirectoryPath.isNullOrBlank()))
    usages.add(getBooleanUsage("hasCustomGradleVmOptions", !gradleSettings.gradleVmOptions.isNullOrBlank()))
    usages.add(getBooleanUsage("showSelectiveImportDialogOnInitialImport", gradleSettings.showSelectiveImportDialogOnInitialImport()))
    usages.add(getBooleanUsage("storeProjectFilesExternally", gradleSettings.storeProjectFilesExternally))

    // project settings
    for (setting in gradleSettings.linkedProjectsSettings) {
      val projectPath = setting.externalProjectPath
      usages.add(getBooleanUsage("isUseQualifiedModuleNames", setting.isUseQualifiedModuleNames))
      usages.add(getBooleanUsage("createModulePerSourceSet", setting.isResolveModulePerSourceSet))
      usages.add(getEnumUsage("distributionType", setting.distributionType))

      usages.add(getYesNoUsage("isCompositeBuilds", setting.compositeBuild != null))
      usages.add(getBooleanUsage("disableWrapperSourceDistributionNotification", setting.isDisableWrapperSourceDistributionNotification))

      usages.add(ExternalSystemUsagesCollector.getJRETypeUsage("gradleJvmType", setting.gradleJvm))
      usages.add(ExternalSystemUsagesCollector.getJREVersionUsage(project, "gradleJvmVersion", setting.gradleJvm))

      val gradleVersion = setting.resolveGradleVersion()
      if(gradleVersion.isSnapshot) {
        usages.add(UsageDescriptor("gradleVersion." + anonymizeGradleVersion(gradleVersion.baseVersion) + ".SNAPSHOT", 1))
      } else {
        usages.add(UsageDescriptor("gradleVersion." + anonymizeGradleVersion(gradleVersion), 1))
      }

      usages.add(getBooleanUsage("delegateBuildRun",
                                 GradleProjectSettings.isDelegatedBuildEnabled(project, projectPath)))
      usages.add(getEnumUsage("preferredTestRunner",
                              GradleProjectSettings.getTestRunner(project, projectPath)))
    }
    return usages
  }

  private fun anonymizeGradleVersion(version : GradleVersion) : String {
    return Version.parseVersion(version.version)?.toCompactString() ?: "unknown"
  }

  private fun getYesNoUsage(key: String, value: Boolean): UsageDescriptor {
    return UsageDescriptor(key + if (value) ".yes" else ".no", 1)
  }
}
