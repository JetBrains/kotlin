// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.statistics

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newBooleanMetric
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.externalSystem.statistics.ExternalSystemUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings

class GradleSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "build.gradle.state"

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val gradleSettings = GradleSettings.getInstance(project)
    val projectsSettings = gradleSettings.linkedProjectsSettings
    if (projectsSettings.isEmpty()) {
      return emptySet()
    }
    val usages = mutableSetOf<MetricEvent>()

    // to have a total users base line to calculate pertentages of settings
    usages.add(newBooleanMetric("hasGradleProject", true))

    // global settings
    usages.add(newBooleanMetric("offlineWork", gradleSettings.isOfflineWork))
    usages.add(newBooleanMetric("hasCustomServiceDirectoryPath", !gradleSettings.serviceDirectoryPath.isNullOrBlank()))
    usages.add(newBooleanMetric("hasCustomGradleVmOptions", !gradleSettings.gradleVmOptions.isNullOrBlank()))
    usages.add(newBooleanMetric("showSelectiveImportDialogOnInitialImport", gradleSettings.showSelectiveImportDialogOnInitialImport()))
    usages.add(newBooleanMetric("storeProjectFilesExternally", gradleSettings.storeProjectFilesExternally))

    // project settings
    for (setting in gradleSettings.linkedProjectsSettings) {
      val projectPath = setting.externalProjectPath
      usages.add(newBooleanMetric("isUseQualifiedModuleNames", setting.isUseQualifiedModuleNames))
      usages.add(newBooleanMetric("createModulePerSourceSet", setting.isResolveModulePerSourceSet))
      usages.add(newMetric("distributionType", setting.distributionType))

      usages.add(newBooleanMetric("isCompositeBuilds", setting.compositeBuild != null))
      usages.add(newBooleanMetric("disableWrapperSourceDistributionNotification", setting.isDisableWrapperSourceDistributionNotification))

      usages.add(ExternalSystemUsagesCollector.getJRETypeUsage("gradleJvmType", setting.gradleJvm))
      usages.add(ExternalSystemUsagesCollector.getJREVersionUsage(project, "gradleJvmVersion", setting.gradleJvm))

      val gradleVersion = setting.resolveGradleVersion()
      if(gradleVersion.isSnapshot) {
        usages.add(newMetric("gradleVersion", anonymizeGradleVersion(gradleVersion.baseVersion) + ".SNAPSHOT"))
      } else {
        usages.add(newMetric("gradleVersion", anonymizeGradleVersion(gradleVersion)))
      }

      usages.add(newBooleanMetric("delegateBuildRun", GradleProjectSettings.isDelegatedBuildEnabled(project, projectPath)))
      usages.add(newMetric("preferredTestRunner", GradleProjectSettings.getTestRunner(project, projectPath)))
    }
    return usages
  }

  private fun anonymizeGradleVersion(version : GradleVersion) : String {
    return Version.parseVersion(version.version)?.toCompactString() ?: "unknown"
  }
}
