// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard.getNewProjectJdk
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.components.StorageScheme
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.wizard.MavenizedStructureWizardStep
import com.intellij.openapi.externalSystem.util.ui.DataView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.layout.*
import icons.GradleIcons
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants
import javax.swing.Icon

class GradleStructureWizardStep(
  private val builder: AbstractGradleModuleBuilder,
  context: WizardContext
) : MavenizedStructureWizardStep<ProjectData>(context) {

  override fun getHelpId() = "Gradle_Archetype_Dialog"

  override fun createView(data: ProjectData) = GradleDataView(data)

  override fun findAllParents(): List<ProjectData> {
    val project = context.project ?: return emptyList()
    return ProjectDataManager.getInstance()
      .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)
      .mapNotNull { it.externalProjectStructure }
      .map { it.data }
  }

  override fun ValidationInfoBuilder.validateGroupId(): ValidationInfo? = null

  override fun ValidationInfoBuilder.validateVersion(): ValidationInfo? = null

  override fun ValidationInfoBuilder.validateName(): ValidationInfo? {
    return validateNameAndArtifactId() ?: superValidateName()
  }

  override fun ValidationInfoBuilder.validateArtifactId(): ValidationInfo? {
    return validateNameAndArtifactId() ?: superValidateArtifactId()
  }

  private fun ValidationInfoBuilder.validateNameAndArtifactId(): ValidationInfo? {
    if (artifactId == entityName) return null
    val presentationName = context.presentationName.capitalize()
    return error(GradleBundle.message("gradle.structure.wizard.name.and.artifact.id.is.different.error", presentationName))
  }

  override fun updateProjectData() {
    context.projectBuilder = builder
    builder.setParentProject(parentData)
    builder.projectId = ProjectId(groupId, artifactId, version)
    builder.isInheritGroupId = parentData?.group == groupId
    builder.isInheritVersion = parentData?.version == version
    builder.name = entityName
    builder.contentEntryPath = location

    builder.moduleFilePath = getModuleFilePath()
    builder.isCreatingNewProject = context.isCreatingNewProject
    builder.moduleJdk = builder.moduleJdk ?: getNewProjectJdk(context)
  }

  private fun getModuleFilePath(): String {
    val moduleFileDirectory = getModuleFileDirectory()
    val moduleFilePath = FileUtil.join(moduleFileDirectory, artifactId + ModuleFileType.DOT_DEFAULT_EXTENSION)
    return FileUtil.toCanonicalPath(moduleFilePath)
  }

  private fun getModuleFileDirectory(): String {
    val projectPath = context.project?.basePath ?: location
    return when (context.projectStorageFormat) {
      StorageScheme.DEFAULT -> location
      StorageScheme.DIRECTORY_BASED -> FileUtil.join(projectPath, Project.DIRECTORY_STORE_FOLDER, "modules")
      else -> projectPath
    }
  }

  override fun _init() {
    builder.name?.let { entityName = it }
    builder.projectId?.let { projectId ->
      projectId.groupId?.let { groupId = it }
      projectId.artifactId?.let { artifactId = it }
      projectId.version?.let { version = it }
    }
  }

  class GradleDataView(override val data: ProjectData) : DataView<ProjectData>() {
    override val location: String = data.linkedExternalProjectPath
    override val icon: Icon = GradleIcons.GradleFile
    override val presentationName: String = data.externalName
    override val groupId: String = data.group ?: ""
    override val version: String = data.version ?: ""
  }
}