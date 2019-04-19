// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo


/**
 * @author Vladislav.Soroka
 */
class GradleIdeaPluginScriptContributor : GradleMethodContextContributor {

  companion object {
    const val IDEA_METHOD: String = "idea"
    const val IDEA_MODEL_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModel"
    const val IDEA_PROJECT_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaProject"
    const val IDEA_MODULE_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModule"
    const val IDEA_MODULE_IML_FQN: String = "org.gradle.plugins.ide.idea.model.IdeaModuleIml"
    const val IDE_XML_MERGER_FQN: String = "org.gradle.plugins.ide.api.XmlFileContentMerger"
    const val IDE_FILE_MERGER_FQN: String = "org.gradle.plugins.ide.api.FileContentMerger"
    const val IDEA_XML_MODULE_FQN: String = "org.gradle.plugins.ide.idea.model.Module"
    const val IDEA_XML_PROJECT_FQN: String = "org.gradle.plugins.ide.idea.model.Project"
    val ideaClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, IDEA_METHOD))
    val ideaProjectClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDEA_MODEL_FQN, "project"))
    val ideaIprClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDEA_PROJECT_FQN, "ipr"))
    val ideaModuleClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDEA_MODEL_FQN, "module"))
    val ideaImlClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDEA_MODULE_FQN, "iml"))
    val ideaBeforeMergedClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDE_FILE_MERGER_FQN, "beforeMerged"))
    val ideaWhenMergedClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(IDE_FILE_MERGER_FQN, "whenMerged"))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (ideaClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(IDEA_MODEL_FQN, closure), Closure.DELEGATE_FIRST)
    }
    if (ideaImlClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(IDEA_MODULE_IML_FQN, closure), Closure.DELEGATE_FIRST)
    }
    if (ideaProjectClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(IDEA_PROJECT_FQN, closure), Closure.DELEGATE_FIRST)
    }
    if (ideaModuleClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(IDEA_MODULE_FQN, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }

  override fun process(methodCallInfo: List<String>, processor: PsiScopeProcessor, state: ResolveState, place: PsiElement): Boolean {
    val ideaExtension = GradleExtensionsSettings.GradleExtension().apply {
      name = IDEA_METHOD
      rootTypeFqn = IDEA_MODEL_FQN
    }
    if (!processExtension(processor, state, place, ideaExtension)) return false

    if (psiElement().inside(ideaIprClosure).inside(ideaProjectClosure).accepts(place)) {
      if (GradleResolverUtil.processDeclarations(processor, state, place, IDE_XML_MERGER_FQN)) return false
    }
    return true
  }
}