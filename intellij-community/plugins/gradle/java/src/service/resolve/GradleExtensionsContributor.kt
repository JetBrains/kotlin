// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleExtensionsData
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

class GradleExtensionsContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String? = GRADLE_API_PROJECT

  override fun processDynamicElements(qualifierType: PsiType,
                                      aClass: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (qualifierType !is GradleProjectAwareType) return
    if (!processor.shouldProcessProperties()) return
    val file = place.containingFile
    val data = getExtensionsFor(file) ?: return
    val name = processor.getName(state)
    val properties = if (name == null) data.findAllProperties() else listOf(data.findProperty(name) ?: return)
    for (property in properties) {
      if (!processor.execute(GradleGroovyProperty(property, file), state)) {
        return
      }
    }
  }

  companion object {
    fun getExtensionsFor(psiElement: PsiElement): GradleExtensionsData? {
      val project = psiElement.project
      val virtualFile = psiElement.containingFile?.originalFile?.virtualFile ?: return null
      val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(virtualFile)
      return GradleExtensionsSettings.getInstance(project).getExtensionsFor(module) ?: return null
    }
  }
}
