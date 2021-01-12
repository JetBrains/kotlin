// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.codeInsight.navigation.PsiElementNavigationTarget
import com.intellij.model.Pointer
import com.intellij.model.presentation.PresentableSymbol
import com.intellij.model.presentation.SymbolPresentation
import com.intellij.navigation.NavigatableSymbol
import com.intellij.navigation.NavigationTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import icons.GradleIcons
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.plugins.gradle.model.ExternalProject
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache
import org.jetbrains.plugins.gradle.util.GradleBundle

/**
 * Empty [myQualifiedName] means root project.
 */
@Internal
class GradleProjectSymbol(
  private val myQualifiedName: List<String>,
  private val myRootProjectPath: String
) : PresentableSymbol, NavigatableSymbol {

  init {
    require(myRootProjectPath.isNotBlank())
  }

  override fun createPointer(): Pointer<GradleProjectSymbol> = Pointer.hardPointer(this)

  val projectName: String get() = if (myQualifiedName.isEmpty()) ":" else myQualifiedName.last()
  val qualifiedName: String get() = qualifiedName(myQualifiedName)

  private val myPresentation = SymbolPresentation.create(
    GradleIcons.Gradle,
    projectName,
    GradleBundle.message("gradle.project.0", projectName),
    GradleBundle.message("gradle.project.0", qualifiedName)
  )

  override fun getSymbolPresentation(): SymbolPresentation = myPresentation

  override fun getNavigationTargets(project: Project): Collection<NavigationTarget> {
    val psiFile = findBuildFile(project)
    if (psiFile != null) {
      return listOf(PsiElementNavigationTarget(psiFile))
    }
    return emptyList()
  }

  private fun findBuildFile(project: Project): PsiElement? {
    val rootProject = ExternalProjectDataCache.getInstance(project).getRootExternalProject(myRootProjectPath) ?: return null
    val externalProject = if (myQualifiedName.isEmpty()) {
      rootProject
    }
    else {
      myQualifiedName.fold(rootProject as ExternalProject?) { extProject, name ->
        extProject?.childProjects?.get(name)
      } ?: return null
    }

    val buildFile = externalProject.buildFile ?: return null
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(buildFile) ?: return null
    return PsiManager.getInstance(project).findFile(virtualFile)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GradleProjectSymbol

    if (myQualifiedName != other.myQualifiedName) return false
    if (myRootProjectPath != other.myRootProjectPath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = myQualifiedName.hashCode()
    result = 31 * result + myRootProjectPath.hashCode()
    return result
  }

  companion object {

    fun qualifiedName(qualifiedNameParts: List<String>): String {
      return if (qualifiedNameParts.isEmpty()) {
        ":"
      }
      else {
        qualifiedNameParts.joinToString("") { ":$it" }
      }
    }
  }
}
