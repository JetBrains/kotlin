// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.search.PsiSymbolReferenceSearchParameters
import com.intellij.model.search.PsiSymbolReferenceSearcher
import com.intellij.model.search.SearchContext
import com.intellij.model.search.SearchService
import com.intellij.model.search.impl.ExternalReferenceMapper
import com.intellij.util.Query
import org.jetbrains.plugins.groovy.GroovyLanguage

class GradleProjectReferenceSearcher : PsiSymbolReferenceSearcher {

  override fun collectSearchRequests(parameters: PsiSymbolReferenceSearchParameters): Collection<Query<out PsiSymbolReference>> {
    val projectSymbol = parameters.symbol as? GradleProjectSymbol ?: return emptyList()
    val projectName = projectSymbol.projectName.takeIf(String::isNotEmpty) ?: return emptyList()
    val query = SearchService.getInstance()
      .searchWord(parameters.project, projectName)
      .inScope(parameters.searchScope)
      .inFilesWithLanguage(GroovyLanguage)
      .inContexts(SearchContext.IN_STRINGS)
      .buildQuery(ExternalReferenceMapper(projectSymbol.createPointer()))
    return listOf(query)
  }
}
