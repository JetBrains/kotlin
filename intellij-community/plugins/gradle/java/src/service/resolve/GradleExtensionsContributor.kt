// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Ref
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.CommonClassNames.*
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.ElementClassHint
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.util.ProcessingContext
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.*
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder.DOCUMENTATION
import org.jetbrains.plugins.groovy.lang.parser.GroovyElementTypes.COMPOSITE_LSHIFT_SIGN
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.path.GrMethodCallExpressionImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.groovyBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DELEGATES_TO_TYPE_KEY
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DELEGATES_TO_STRATEGY_KEY
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

/**
 * @author Vladislav.Soroka
 */
class GradleExtensionsContributor : GradleMethodContextContributor {

  override fun process(methodCallInfo: MutableList<String>,
                       processor: PsiScopeProcessor,
                       state: ResolveState,
                       place: PsiElement): Boolean {
    val extensionsData = getExtensionsFor(place) ?: return true
    val classHint = processor.getHint(ElementClassHint.KEY)
    val shouldProcessMethods = ResolveUtil.shouldProcessMethods(classHint)
    val shouldProcessProperties = ResolveUtil.shouldProcessProperties(classHint)
    val groovyPsiManager = GroovyPsiManager.getInstance(place.project)
    val resolveScope = place.resolveScope
    val name = processor.getName(state)

    if (psiElement().inside(closureInLeftShiftMethod).accepts(place)) {
      if (!GradleResolverUtil.processDeclarations(processor, state, place, GRADLE_API_DEFAULT_TASK)) {
        return false
      }
    }

    if (place.getUserData(RESOLVED_CODE).let { it == null || !it }) {
      if (psiElement().withAncestor(2, groovyClosure().with(object : PatternCondition<GrClosableBlock?>("withDelegatesToInfo") {
        override fun accepts(t: GrClosableBlock, context: ProcessingContext?): Boolean {
          return getDelegatesToInfo(t) != null
        }
      })).accepts(place)) {
        return true
      }

      if (name != null && place is GrReferenceExpression && !place.isQualified) {
        val propExecutionResult = extensionsData.findProperty(name)?.let {
          if (!shouldProcessMethods && shouldProcessProperties) {
            val docRef = Ref.create<String>()
            val variable = object : GrLightVariable(place.manager, name, it.typeFqn, place) {
              override fun getNavigationElement(): PsiElement {
                val navigationElement = super.getNavigationElement()
                navigationElement.putUserData(DOCUMENTATION, docRef.get())
                return navigationElement
              }
            }
            val doc = getDocumentation(it, variable)
            docRef.set(doc)
            place.putUserData(DOCUMENTATION, doc)
            return processor.execute(variable, state)
          }
          else if (shouldProcessMethods && it.typeFqn == GROOVY_LANG_CLOSURE) {
            val returnClass = groovyPsiManager.createTypeByFQClassName(GROOVY_LANG_CLOSURE, resolveScope) ?: return true
            val methodBuilder = GrLightMethodBuilder(place.manager, name).apply {
              returnType = returnClass
              addOptionalParameter("args", JAVA_LANG_OBJECT)
            }
            return processor.execute(methodBuilder, state)
          }
          true
        }
        if (propExecutionResult != null && propExecutionResult) return false
      }
    }

    return true
  }

  companion object {
    val closureInLeftShiftMethod: GroovyClosurePattern = groovyClosure().withTreeParent(
      groovyBinaryExpression().with(object : PatternCondition<GrBinaryExpression?>("leftShiftCondition") {
        override fun accepts(t: GrBinaryExpression, context: ProcessingContext?): Boolean {
          return t.operationTokenType == COMPOSITE_LSHIFT_SIGN
        }
      }))

    fun getExtensionsFor(psiElement: PsiElement): GradleExtensionsSettings.GradleExtensionsData? {
      val project = psiElement.project
      val virtualFile = psiElement.containingFile?.originalFile?.virtualFile ?: return null
      val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(virtualFile)
      return GradleExtensionsSettings.getInstance(project).getExtensionsFor(module) ?: return null
    }

    fun getDocumentation(gradleProp: GradleExtensionsSettings.TypeAware,
                         lightVariable: GrLightVariable): String? {
      if (gradleProp is GradleProp) {
        return getDocumentation(gradleProp, lightVariable)
      }
      else if (gradleProp is GradleTask) {
        return getDocumentation(gradleProp, lightVariable)
      }
      else {
        return null
      }
    }

    fun getDocumentation(gradleProp: GradleProp,
                         lightVariable: GrLightVariable): String {
      val buffer = StringBuilder()
      buffer.append("<PRE>")
      JavaDocInfoGenerator.generateType(buffer, lightVariable.type, lightVariable, true)
      buffer.append(" " + gradleProp.name)
      val hasInitializer = !gradleProp.value.isNullOrBlank()
      if (hasInitializer) {
        buffer.append(" = ")
        val longString = gradleProp.value!!.toString().length > 100
        if (longString) {
          buffer.append("<blockquote>")
        }
        buffer.append(gradleProp.value)
        if (longString) {
          buffer.append("</blockquote>")
        }
      }
      buffer.append("</PRE>")
      if (hasInitializer) {
        buffer.append("<br><b>Initial value has been got during last import</b>")
      }
      return buffer.toString()
    }

    fun getDocumentation(gradleTask: GradleTask, lightVariable: GrLightVariable): String {
      return getDocumentation(gradleTask, lightVariable.type, lightVariable)
    }

    fun getDocumentation(gradleTask: GradleTask, type: PsiType, context: PsiElement): String {
      val buffer = StringBuilder()
      buffer.append("<PRE>")
      JavaDocInfoGenerator.generateType(buffer, type, context, true)
      buffer.append(" " + gradleTask.name)
      buffer.append("</PRE>")
      if (!gradleTask.description.isNullOrBlank()) {
        buffer.append(gradleTask.description)
      }
      return buffer.toString()
    }
  }
}

fun processExtension(processor: PsiScopeProcessor,
                     state: ResolveState,
                     place: PsiElement,
                     extension: GradleExtension): Boolean {
  val classHint = processor.getHint(ElementClassHint.KEY)
  val shouldProcessMethods = ResolveUtil.shouldProcessMethods(classHint)
  val extensionClosure = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, extension.name))
  val psiElement = psiElement()
  if (psiElement.inside(extensionClosure).accepts(place)) {
    if (shouldProcessMethods && !GradleResolverUtil.processDeclarations(processor, state, place, extension.rootTypeFqn)) {
      return false
    }
  }
  return true
}
