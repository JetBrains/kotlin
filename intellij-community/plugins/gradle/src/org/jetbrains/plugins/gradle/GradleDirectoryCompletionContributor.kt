// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle

import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getExternalRootProjectPath
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import org.jetbrains.plugins.gradle.util.GradleConstants

class GradleDirectoryCompletionContributor : CreateDirectoryCompletionContributor {
  override fun getDescription() = "Gradle Source Sets"

  override fun getVariants(directory: PsiDirectory): Collection<CreateDirectoryCompletionContributor.Variant> {
    val project = directory.project

    val module = ProjectFileIndex.getInstance(project).getModuleForFile(directory.virtualFile) ?: return emptyList()

    val moduleProperties = ExternalSystemModulePropertyManager.getInstance(module)
    if (GradleConstants.SYSTEM_ID.id != moduleProperties.getExternalSystemId()) return emptyList()

    val gradleProject = moduleProperties.getRootProjectPath() ?: return emptyList()

    // avoid iterating all modules unnecessarily: might be expensive on big projects
    val relatedGradleModules by lazy {
      ModuleManager.getInstance(project).modules
        .asSequence()
        .filter { StringUtil.equals(gradleProject, getExternalRootProjectPath(it)) }
        .toSet()
    }

    return (SourceFolderManager.getInstance(project) as SourceFolderManagerImpl)
      .getSourceFoldersUnder({ relatedGradleModules }, directory.virtualFile.url)
      .map { folder ->
        val handler = ModuleSourceRootEditHandler.getEditHandler(folder.second)
        CreateDirectoryCompletionContributor.Variant(VfsUtilCore.urlToPath(folder.first), handler?.rootIcon)
      }
      .toList()
  }
}
