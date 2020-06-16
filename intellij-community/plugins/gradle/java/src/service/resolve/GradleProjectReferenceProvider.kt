// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiExternalReferenceHost
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolReferenceHints
import com.intellij.model.psi.PsiSymbolReferenceProvider
import com.intellij.model.search.SearchRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyMethodCallPattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyElement
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod

@Internal
class GradleProjectReferenceProvider : PsiSymbolReferenceProvider {

  private val myPattern = GroovyPatterns.stringLiteral().withParent(
    groovyElement<GrArgumentList>().withParent(
      GroovyMethodCallPattern.resolvesTo(psiMethod(GradleCommonClassNames.GRADLE_API_PROJECT, "project"))
    )
  )

  /**
   * Handled by [GradleProjectReferenceSearcher].
   */
  override fun getSearchRequests(project: Project, target: Symbol): Collection<SearchRequest> = emptyList()

  override fun getReferences(element: PsiExternalReferenceHost, hints: PsiSymbolReferenceHints): Collection<PsiSymbolReference> {
    if (element !is GrLiteral || !myPattern.accepts(element)) {
      return emptyList()
    }
    val escaper = element.createLiteralTextEscaper()
    val manipulator = ElementManipulators.getManipulator(element)
    val value = StringBuilder()
    val rangeInHost = manipulator.getRangeInElement(element)
    if (!escaper.decode(rangeInHost, value)) {
      return emptyList()
    }
    if (!value.startsWith(":")) {
      return emptyList()
    }
    if (value.toString() == ":") {
      // in this special case we want the root project reference to span over the colon symbol
      return listOf(GradleProjectReference(element, rangeInHost, emptyList()))
    }

    val path = value.split(":").drop(1) // drop first empty string
    val result = ArrayList<PsiSymbolReference>(path.size + 1)

    val rootOffsetInHost = escaper.getOffsetInHost(0, rangeInHost)
    if (rootOffsetInHost >= 0) {
      // add a root project reference just before the first colon, i.e. it has an empty range
      result += GradleProjectReference(element, TextRange(rootOffsetInHost, rootOffsetInHost), emptyList())
    }

    val subPath = ArrayList<String>()
    var currentOffsetInPath = 1 // skip first ":"
    for (part in path) {
      subPath.add(part)
      val partStart = escaper.getOffsetInHost(currentOffsetInPath, rangeInHost)
      currentOffsetInPath += part.length
      val partEnd = escaper.getOffsetInHost(currentOffsetInPath, rangeInHost)
      currentOffsetInPath += 1 // ":"
      if (partStart < 0 || partEnd < 0) {
        continue
      }
      result += GradleProjectReference(element, TextRange(partStart, partEnd), ArrayList(subPath))
    }
    return result
  }
}
