// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.CommonClassNames.JAVA_LANG_CHAR_SEQUENCE
import com.intellij.psi.CommonClassNames.JAVA_UTIL_MAP
import com.intellij.psi.PsiClassType
import com.intellij.psi.util.InheritanceUtil.isInheritor
import com.intellij.util.ProcessingContext
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.gradle.service.resolve.GradleDependencyHandlerContributor.Companion.dependencyMethodKind
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.patterns.*
import org.jetbrains.plugins.groovy.lang.resolve.api.Argument
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.impl.getArguments

/**
 * @author Vladislav.Soroka
 */
class GradleDependenciesContributor : GradleMethodContextContributor {

  companion object {
    val dependenciesClosure: GroovyClosurePattern = groovyClosure().inMethod(or(
      psiMethod(GRADLE_API_PROJECT, "dependencies"),
      psiMethod(GRADLE_API_SCRIPT_HANDLER, "dependencies")
    )).inMethodResult(saveProjectType)

    val dependencyAddClosure: GroovyClosurePattern = groovyClosure().inMethod(
      psiMethod(GRADLE_API_DEPENDENCY_HANDLER, "add")
    )
    val dependencyClosure: GroovyClosurePattern = groovyClosure().inMethod(
      psiMethod(GRADLE_API_DEPENDENCY_HANDLER).withKind(dependencyMethodKind)
    )

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
      dependencyAddClosure.accepts(closure, context) -> fromDependencyNotation(context[closureCallKey], 1)
                                                        ?: GRADLE_API_ARTIFACTS_MODULE_DEPENDENCY
      dependencyClosure.accepts(closure, context) -> fromDependencyNotation(context[closureCallKey], 0)
                                                     ?: GRADLE_API_ARTIFACTS_MODULE_DEPENDENCY
      moduleClosure.accepts(closure) -> GRADLE_API_ARTIFACTS_CLIENT_MODULE_DEPENDENCY
      componentsClosure.accepts(closure) -> GRADLE_API_COMPONENT_METADATA_HANDLER
      modulesClosure.accepts(closure) -> GRADLE_API_COMPONENT_MODULE_METADATA_HANDLER
      modulesModuleClosure.accepts(closure) -> GRADLE_API_COMPONENT_MODULE_METADATA_DETAILS
      else -> return null
    }
    return DelegatesToInfo(TypesUtil.createType(fqn, closure), Closure.DELEGATE_FIRST)
  }

  private fun fromDependencyNotation(call: GrCall?, argumentIndex: Int): String? {
    if (call == null) return null
    val arguments = call.getArguments() ?: return null
    if (arguments.size != argumentIndex + 2) return null
    return fromDependencyNotation(arguments[argumentIndex])
  }

  private fun fromDependencyNotation(notation: Argument): String? {
    val type = notation.type as? PsiClassType ?: return null
    return when {
      isInheritor(type, JAVA_LANG_CHAR_SEQUENCE)
      || isInheritor(type, JAVA_UTIL_MAP) -> GRADLE_API_ARTIFACTS_EXTERNAL_MODULE_DEPENDENCY
      isInheritor(type, GRADLE_API_PROJECT) -> GRADLE_API_ARTIFACTS_PROJECT_DEPENDENCY
      isInheritor(type, GRADLE_API_FILE_FILE_COLLECTION) -> GRADLE_API_ARTIFACTS_SELF_RESOLVING_DEPENDENCY
      else -> null
    }
  }
}
