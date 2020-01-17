// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.settings

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.codeInspection.ex.InspectionToolRegistrar
import com.intellij.openapi.externalSystem.model.project.settings.ConfigurationData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager

/**
 * Created by Nikita.Skvortsov
 * date: 19.09.2017.
 */
class InspectionsProfileConfigurationHandler: ConfigurationHandler {
  override fun apply(project: Project, modelsProvider: IdeModifiableModelsProvider, configuration: ConfigurationData) {
    val listOfInspectionConfigObjects: List<*> = configuration.find("inspections") as? List<*> ?: return

    val gradleProfileName = "Gradle Imported"
    val profileManager = ProjectInspectionProfileManager.getInstance(project)
    val importedProfile = InspectionProfileImpl(gradleProfileName, InspectionToolRegistrar.getInstance(), profileManager)

    importedProfile.copyFrom(profileManager.getProfile(com.intellij.codeInspection.ex.DEFAULT_PROFILE_NAME))
    importedProfile.initInspectionTools(project)
    val modifiableModel = importedProfile.modifiableModel
    modifiableModel.name = gradleProfileName

    modifiableModel.commit()
    profileManager.addProfile(importedProfile)
    profileManager.setRootProfile(gradleProfileName)
  }
}