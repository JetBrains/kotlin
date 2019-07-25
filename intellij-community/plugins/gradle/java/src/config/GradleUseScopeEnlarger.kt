// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.Processor
import org.jetbrains.plugins.gradle.service.GradleBuildClasspathManager
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * @author Vladislav.Soroka
 */
class GradleUseScopeEnlarger : UseScopeEnlarger() {
  override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
    return try {
      getScope(element)
    }
    catch (e: IndexNotReadyException) {
      null
    }

  }

  companion object {
    private fun getScope(element: PsiElement): SearchScope? {
      val virtualFile = PsiUtilCore.getVirtualFile(element.containingFile) ?: return null

      val fileIndex = ProjectRootManager.getInstance(element.project).fileIndex
      val module = fileIndex.getModuleForFile(virtualFile) ?: return null
      if (!isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return null

      val rootProjectPath = getExternalRootProjectPath(module) ?: return null
      return if (!isApplicable(element, module, rootProjectPath, virtualFile, fileIndex)) null
      else object : GlobalSearchScope(element.project) {
        override fun contains(file: VirtualFile): Boolean {
          return GradleConstants.EXTENSION == file.extension || file.name.endsWith(GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION)
        }
        override fun isSearchInModuleContent(aModule: Module) = rootProjectPath == getExternalRootProjectPath(module)
        override fun isSearchInLibraries() = false
      }
    }

    private fun isApplicable(element: PsiElement,
                             module: Module,
                             rootProjectPath: String,
                             virtualFile: VirtualFile,
                             fileIndex: ProjectFileIndex): Boolean {
      val projectPath = getExternalProjectPath(module) ?: return false
      if (projectPath.endsWith("/buildSrc")) return true
      val sourceRoot = fileIndex.getSourceRootForFile(virtualFile)
      return sourceRoot in GradleBuildClasspathManager.getInstance(element.project).getModuleClasspathEntries(rootProjectPath)
    }

    fun search(element: PsiMember, consumer: Processor<PsiReference>) {
      val scope: SearchScope = ReadAction.compute<SearchScope, RuntimeException> { getScope(element) } ?: return
      val newParams = ReferencesSearch.SearchParameters(element, scope, true)
      ReferencesSearch.search(newParams).forEach(consumer)
    }
  }
}
