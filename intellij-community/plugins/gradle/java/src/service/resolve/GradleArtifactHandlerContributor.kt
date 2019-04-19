// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import icons.GradleIcons.Gradle
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_ARTIFACT_HANDLER
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.getJavaLangObject
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods

class GradleArtifactHandlerContributor : NonCodeMembersContributor() {

  companion object {
    const val ourMethodKind: String = "gradle:artifactsConfigurationMethod"
  }

  override fun getParentClassName(): String? = GRADLE_API_ARTIFACT_HANDLER

  override fun processDynamicElements(qualifierType: PsiType,
                                      clazz: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (qualifierType !is GradleProjectAwareType) return
    if (clazz == null) return
    if (!processor.shouldProcessMethods()) return

    val data = GradleExtensionsContributor.getExtensionsFor(place) ?: return
    val methodName = processor.getName(state)
    val manager = place.manager
    val objectVarargType = PsiEllipsisType(getJavaLangObject(place))

    val configurationsMap = if (qualifierType.buildscript) data.buildScriptConfigurations else data.configurations
    // The null method name means we are in completion, and in this case all available declarations should be fed into the processor.
    val configurations = if (methodName == null) configurationsMap.values else listOf(configurationsMap[methodName] ?: return)
    for (configuration in configurations) {
      val configurationName = configuration.name ?: continue
      val method = GrLightMethodBuilder(manager, configurationName).apply {
        methodKind = ourMethodKind
        containingClass = clazz
        returnType = null
        addParameter("artifactNotation", objectVarargType)
        setBaseIcon(Gradle)
      }
      if (!processor.execute(method, state)) return
    }
  }
}
