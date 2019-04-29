// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.codeInsight

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Ref
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import icons.ExternalSystemIcons
import icons.GradleIcons
import org.jetbrains.plugins.gradle.service.resolve.GradleExtensionsContributor
import org.jetbrains.plugins.gradle.service.resolve.GradleExtensionsContributor.Companion.getDocumentation
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable

/**
 * @author Vladislav.Soroka
 */
class PropertiesTasksCompletionContributor : AbstractGradleCompletionContributor() {
  class PropertiesTasksCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(params: CompletionParameters,
                                context: ProcessingContext,
                                result: CompletionResultSet) {
      val position = params.position

      val prevSibling = position.prevSibling
      if (prevSibling is ASTNode && prevSibling.elementType == GroovyTokenTypes.mDOT) return

      val extensionsData = GradleExtensionsContributor.getExtensionsFor(position) ?: return
      for (gradleProp in extensionsData.findAllProperties()) {
        val docRef = Ref.create<String>()
        val propVar = object : GrLightVariable(position.manager, gradleProp.name, gradleProp.typeFqn, position) {
          override fun getNavigationElement(): PsiElement {
            val navigationElement = super.getNavigationElement()
            navigationElement.putUserData(NonCodeMembersHolder.DOCUMENTATION, docRef.get())
            return navigationElement
          }
        }
        docRef.set(getDocumentation(gradleProp, propVar))
        val elementBuilder = LookupElementBuilder.create(propVar, gradleProp.name)
          .withIcon(AllIcons.Nodes.Property)
          .withPresentableText(gradleProp.name)
          .withTailText("  via ext", true)
          .withTypeText(propVar.type.presentableText, GradleIcons.Gradle, false)
        result.addElement(elementBuilder)
      }
    }
  }

  init {
    extend(CompletionType.BASIC, PATTERN, PropertiesTasksCompletionProvider())
  }

  companion object {
    private val PATTERN = psiElement().and(GRADLE_FILE_PATTERN).withParent(GrReferenceExpression::class.java)
  }
}