// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import icons.GradleIcons
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_DEPENDENCY_HANDLER
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleConfiguration
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods

class GradleDependencyHandlerContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String? = GRADLE_API_DEPENDENCY_HANDLER

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
    val objectVarargType = PsiEllipsisType(TypesUtil.getJavaLangObject(place))

    val configurationsMap = if (qualifierType.buildscript) data.buildScriptConfigurations else data.configurations
    val configurations = if (methodName == null) configurationsMap.values else listOf(configurationsMap[methodName] ?: return)
    for (configuration in configurations) {
      val configurationName = configuration.name ?: continue
      val method = GrLightMethodBuilder(manager, configurationName).apply {
        methodKind = dependencyMethodKind
        containingClass = clazz
        returnType = null
        addParameter("dependencyNotation", objectVarargType)
        setBaseIcon(GradleIcons.Gradle)
        putUserData(NonCodeMembersHolder.DOCUMENTATION, configuration.getDescription())
      }
      if (!processor.execute(method, state)) return
    }
  }

  private fun GradleConfiguration.getDescription(): String? {
    if (description == null && scriptClasspath && name == "classpath") {
      return GradleBundle.message("gradle.codeInsigt.buildscript.classpath.description")
    }
    else {
      return description
    }
  }

  companion object {
    const val dependencyMethodKind: String = "gradle:dependencyMethod"
  }
}
