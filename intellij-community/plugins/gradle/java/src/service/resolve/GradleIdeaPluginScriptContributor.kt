// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import groovy.lang.Closure.DELEGATE_FIRST
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo

class GradleIdeaPluginScriptContributor : GradleMethodContextContributor {

  companion object {
    const val IDEA_MODEL_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModel"
    const val IDEA_PROJECT_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaProject"
    const val IDEA_MODULE_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModule"
    const val IDE_XML_MERGER_FQN: String = "org.gradle.plugins.ide.api.XmlFileContentMerger"
    const val IDEA_MODULE_IML_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModuleIml"
    private val ideaClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "idea"))
    private val ideaProjectClosure = groovyClosure().inMethod(psiMethod(IDEA_MODEL_FQN, "project"))
    private val ideaModuleClosure = groovyClosure().inMethod(psiMethod(IDEA_MODEL_FQN, "module"))
    private val ideaIprClosure = groovyClosure().inMethod(psiMethod(IDEA_PROJECT_FQN, "ipr"))
    private val ideaImlClosure = groovyClosure().inMethod(psiMethod(IDEA_MODULE_FQN, "iml"))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    val fqn = when {
      ideaClosure.accepts(closure) -> IDEA_MODEL_FQN
      ideaProjectClosure.accepts(closure) -> IDEA_PROJECT_FQN
      ideaModuleClosure.accepts(closure) -> IDEA_MODULE_FQN
      ideaIprClosure.accepts(closure) -> IDE_XML_MERGER_FQN
      ideaImlClosure.accepts(closure) -> IDEA_MODULE_IML_FQN
      else -> return null
    }
    return DelegatesToInfo(createType(fqn, closure.containingFile), DELEGATE_FIRST)
  }
}
