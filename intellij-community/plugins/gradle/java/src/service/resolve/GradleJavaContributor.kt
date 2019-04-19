// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.PsiJavaElementPattern
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.PsiElement
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
 */
class GradleJavaContributor : GradleMethodContextContributor {

  companion object {
    val sourceSetsClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_JAVA_PLUGIN_CONVENTION, "sourceSets"))
    val sourceDirectorySetClosure: PsiJavaElementPattern.Capture<PsiElement> = psiElement().andOr(
      groovyClosure().inMethod(or(psiMethod(GRADLE_API_SOURCE_SET, "java"),
                                  psiMethod(GRADLE_API_SOURCE_SET, "resources"))))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (sourceSetsClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(GRADLE_API_SOURCE_SET_CONTAINER, closure), Closure.DELEGATE_FIRST)
    }
    if (sourceDirectorySetClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(GRADLE_API_SOURCE_DIRECTORY_SET, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }
}
