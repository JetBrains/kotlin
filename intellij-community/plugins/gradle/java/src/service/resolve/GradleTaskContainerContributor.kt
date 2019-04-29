// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASK_CONTAINER
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

class GradleTaskContainerContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String? = GRADLE_API_TASK_CONTAINER

  override fun processDynamicElements(qualifierType: PsiType,
                                      aClass: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (qualifierType !is GradleProjectAwareType) return
    if (!processor.shouldProcessProperties()) return
    val file = place.containingFile ?: return
    val data = GradleExtensionsContributor.getExtensionsFor(file) ?: return
    val name = processor.getName(state)
    val tasks = if (name == null) data.tasksMap.values else listOf(data.tasksMap[name] ?: return)
    for (task in tasks) {
      val property = GradleTaskProperty(task, file)
      if (!processor.execute(property, state)) return
    }
  }
}
