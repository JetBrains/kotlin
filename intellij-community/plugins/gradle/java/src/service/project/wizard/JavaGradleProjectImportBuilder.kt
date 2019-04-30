// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.setupCreatedProject
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.packaging.artifacts.ModifiableArtifactModel
import com.intellij.projectImport.ProjectImportBuilder
import icons.GradleIcons
import org.jetbrains.plugins.gradle.service.project.open.attachGradleProjectAndRefresh
import org.jetbrains.plugins.gradle.service.project.open.setupGradleSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import javax.swing.Icon


class JavaGradleProjectImportBuilder : ProjectImportBuilder<Any>() {

  override fun getName(): String = GradleBundle.message("gradle.name")

  override fun getIcon(): Icon = GradleIcons.Gradle

  override fun getList(): List<Any> = emptyList()

  override fun isMarked(element: Any): Boolean = true

  override fun setList(list: List<Any>) {}

  override fun setOpenProjectSettingsAfter(on: Boolean) {}

  override fun createProject(name: String?, path: String): Project? {
    return setupCreatedProject(super.createProject(name, path))?.also {
      it.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
    }
  }

  override fun commit(project: Project,
                      model: ModifiableModuleModel?,
                      modulesProvider: ModulesProvider?,
                      artifactModel: ModifiableArtifactModel?): List<Module> {
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    val gradleProjectSettings = GradleProjectSettings()
    setupGradleSettings(gradleProjectSettings, fileToImport, project, projectSdk)
    attachGradleProjectAndRefresh(gradleProjectSettings, project)
    ProjectUtil.updateLastProjectLocation(fileToImport)
    project.save()
    return emptyList()
  }
}