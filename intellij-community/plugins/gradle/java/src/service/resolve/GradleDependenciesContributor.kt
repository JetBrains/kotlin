// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.StandardPatterns.or
import com.intellij.util.ProcessingContext
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
class GradleDependenciesContributor : GradleMethodContextContributor {

  companion object {
    val dependenciesClosure: GroovyClosurePattern = groovyClosure().inMethod(or(
      psiMethod(GRADLE_API_PROJECT, "dependencies"),
      psiMethod(GRADLE_API_SCRIPT_HANDLER, "dependencies")
    )).inMethodResult(saveProjectType)

    val dependencyClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_DEPENDENCY_HANDLER, "add"))
    val moduleClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_DEPENDENCY_HANDLER, "module"))
    val componentsClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_DEPENDENCY_HANDLER, "components"))
    val modulesClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_DEPENDENCY_HANDLER, "modules"))

    val modulesModuleClosure: GroovyClosurePattern = groovyClosure().inMethod(
      psiMethod(GRADLE_API_COMPONENT_MODULE_METADATA_HANDLER, "module")
    )
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    val context = ProcessingContext()
    if (dependenciesClosure.accepts(closure, context)) {
      val dependencyHandler = TypesUtil.createType(GRADLE_API_DEPENDENCY_HANDLER, closure)
      val delegate = context.get(projectTypeKey)?.setType(dependencyHandler) ?: dependencyHandler
      return DelegatesToInfo(delegate, Closure.DELEGATE_FIRST)
    }
    val fqn = when {
      dependencyClosure.accepts(closure) -> GRADLE_API_ARTIFACTS_MODULE_DEPENDENCY
      moduleClosure.accepts(closure) -> GRADLE_API_ARTIFACTS_CLIENT_MODULE_DEPENDENCY
      componentsClosure.accepts(closure) -> GRADLE_API_COMPONENT_METADATA_HANDLER
      modulesClosure.accepts(closure) -> GRADLE_API_COMPONENT_MODULE_METADATA_HANDLER
      modulesModuleClosure.accepts(closure) -> GRADLE_API_COMPONENT_MODULE_METADATA_DETAILS
      else -> return null
    }
    return DelegatesToInfo(TypesUtil.createType(fqn, closure), Closure.DELEGATE_FIRST)
  }
}
