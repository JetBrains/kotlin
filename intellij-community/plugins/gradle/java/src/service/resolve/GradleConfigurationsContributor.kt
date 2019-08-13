// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo

/**
 * @author Vladislav.Soroka
 * */
class GradleConfigurationsContributor : GradleMethodContextContributor {

  companion object {
    val configurationsClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "configurations"))
    val dependencySubstitutionClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_RESOLUTION_STRATEGY, "dependencySubstitution"))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (configurationsClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(GRADLE_API_CONFIGURATION_CONTAINER, closure), Closure.DELEGATE_FIRST)
    }

    if (dependencySubstitutionClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(GRADLE_API_DEPENDENCY_SUBSTITUTIONS, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }
}
