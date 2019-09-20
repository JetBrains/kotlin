// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.util.ProcessingContext
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo

/**
 * @author Vladislav.Soroka
 */
class GradleProjectContributor : GradleMethodContextContributor {
  companion object {
    val projectClosure: GroovyClosurePattern = groovyClosure().inMethod(
      psiMethod(GRADLE_API_PROJECT, "project", "configure", "subprojects", "allprojects")
    ).inMethodResult(saveProjectType)
    val copySpecClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "copy", "copySpec"))
    val fileTreeClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "fileTree"))
    val filesClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "files"))
    val execClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "exec"))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    val context = ProcessingContext()
    if (projectClosure.accepts(closure, context)) {
      val projectType = createType(GRADLE_API_PROJECT, closure)
      return DelegatesToInfo(context[projectTypeKey]?.setType(projectType) ?: projectType, Closure.DELEGATE_FIRST)
    }
    if (copySpecClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_COPY_SPEC, closure), Closure.DELEGATE_FIRST)
    }
    if (fileTreeClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_CONFIGURABLE_FILE_TREE, closure), Closure.DELEGATE_FIRST)
    }
    if (filesClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_FILE_CONFIGURABLE_FILE_COLLECTION, closure), Closure.DELEGATE_FIRST)
    }
    if (execClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_PROCESS_EXEC_SPEC, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }
}
