// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor

class GradleConventionsContributor : NonCodeMembersContributor() {

  companion object {
    val conventions = arrayOf(
      GRADLE_API_BASE_PLUGIN_CONVENTION,
      GRADLE_API_JAVA_PLUGIN_CONVENTION,
      GRADLE_API_APPLICATION_PLUGIN_CONVENTION,
      GRADLE_API_WAR_CONVENTION
    )
  }

  /**
   * Plugin conventions are available on the project instance, be it top level reference or `project.something`.
   */
  override fun getParentClassName(): String = GRADLE_API_PROJECT

  override fun processDynamicElements(qualifierType: PsiType, processor: PsiScopeProcessor, place: PsiElement, state: ResolveState) {
    // TODO process conventions from Gradle import instead of hardcoded ones
    processDelegatedDeclarations(processor, state, place, *conventions)
  }
}
