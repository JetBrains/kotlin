// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

class GradleProjectExtensionContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String? = GRADLE_API_PROJECT

  override fun processDynamicElements(qualifierType: PsiType,
                                      aClass: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (qualifierType !is GradleProjectAwareType) return

    val processMethods = processor.shouldProcessMethods()
    val processProperties = processor.shouldProcessProperties()
    if (!processMethods && !processProperties) {
      return
    }

    val containingFile = place.containingFile
    val extensionsData = GradleExtensionsContributor.getExtensionsFor(containingFile) ?: return

    val name = processor.getName(state)
    val allExtensions = extensionsData.extensions
    val extensions = if (name == null) allExtensions.values else listOf(allExtensions[name] ?: return)
    if (extensions.isEmpty()) return

    val factory = GroovyPsiElementFactory.getInstance(containingFile.project)
    val manager = containingFile.manager

    for (extension in extensions) {
      val type = factory.createTypeElement(extension.rootTypeFqn, containingFile).type
      if (processProperties) {
        val extensionProperty = GradleExtensionProperty(extension.name, type, containingFile)
        if (!processor.execute(extensionProperty, state)) {
          return
        }
      }
      if (processMethods) {
        val extensionMethod = GrLightMethodBuilder(manager, extension.name).apply {
          returnType = type
          addParameter("configuration", createType(GROOVY_LANG_CLOSURE, containingFile))
        }
        if (!processor.execute(extensionMethod, state)) {
          return
        }
      }
    }
  }
}
