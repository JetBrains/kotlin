// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASK_CONTAINER
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.processReceiverType

class GradleProjectMembersContributor : NonCodeMembersContributor() {

  override fun unwrapMultiprocessor(): Boolean = false

  override fun getParentClassName(): String? = GRADLE_API_PROJECT

  override fun processDynamicElements(qualifierType: PsiType, processor: PsiScopeProcessor, place: PsiElement, state: ResolveState) {
    val taskContainer = createType(GRADLE_API_TASK_CONTAINER, place)
    val delegate = if (qualifierType is GradleProjectAwareType) qualifierType.setType(taskContainer) else taskContainer
    if (!delegate.processReceiverType(processor, state.put(DELEGATED_TYPE, true), place)) {
      return
    }

    if (qualifierType !is GradleProjectAwareType) return
    val file = place.containingFile ?: return
    val extensionsData = GradleExtensionsContributor.getExtensionsFor(file) ?: return
    for (convention in extensionsData.conventions) {
      if (!createType(convention.typeFqn, file).processReceiverType(processor, state, place)) {
        return
      }
    }
  }
}
