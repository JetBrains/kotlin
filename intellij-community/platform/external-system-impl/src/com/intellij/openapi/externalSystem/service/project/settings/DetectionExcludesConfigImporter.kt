/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.framework.detection.DetectionExcludesConfiguration
import com.intellij.framework.detection.impl.FrameworkDetectorRegistry
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.project.Project

class DetectionExcludesConfigImporter: ConfigurationHandler {
  override fun apply(project: Project, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    val excludedIds = configuration.find("frameworkDetectionExcludes") as? List<*> ?: return

    if (excludedIds.isEmpty()) {
      return
    }

    val frameworkTypeIndex = FrameworkDetectorRegistry.getInstance().frameworkTypes.associateBy { it.id }
    val detectExcludes = DetectionExcludesConfiguration.getInstance(project)

    excludedIds.filterIsInstance<String>().forEach {
      frameworkTypeIndex[it]?.let { type -> detectExcludes.addExcludedFramework(type) }
    }
  }
}