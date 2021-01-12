// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.ContextFeatures
import com.intellij.codeInsight.completion.ml.ElementFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.LanguageNamesValidation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import java.util.*

class RecentPlacesFeatures : ElementFeatureProvider {
  override fun getName(): String = "recent_places"

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): Map<String, MLFeatureValue> {
    val inRecentPlaces = StoreRecentPlacesListener.isInRecentPlaces(element.lookupString)
    val inChildrenRecentPlaces = StoreRecentPlacesListener.isInChildrenRecentPlaces(element.lookupString)
    return when {
      inRecentPlaces && inChildrenRecentPlaces -> mapOf(
        "contains" to MLFeatureValue.binary(true),
        "children_contains" to MLFeatureValue.binary(true)
      )
      inRecentPlaces -> mapOf("contains" to MLFeatureValue.binary(true))
      inChildrenRecentPlaces -> mapOf("children_contains" to MLFeatureValue.binary(true))
      else -> emptyMap()
    }
  }

  internal class StoreRecentPlacesListener(private val project: Project) : IdeDocumentHistoryImpl.RecentPlacesListener {
    companion object {
      private val recentPlaces = createFixedSizeSet(20)
      private val childrenRecentPlaces = createFixedSizeSet(100)
      private const val MAX_CHILDREN_PER_PLACE = 10

      private fun createFixedSizeSet(maxSize: Int): MutableSet<String> =
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
          override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>): Boolean {
            return size > maxSize
          }
        })

      fun isInRecentPlaces(value: String) = recentPlaces.contains(value)
      fun isInChildrenRecentPlaces(value: String) = childrenRecentPlaces.contains(value)
    }

    override fun recentPlaceAdded(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) {
      if (ApplicationManager.getApplication().isUnitTestMode || !changePlace.file.isValid) return
      val provider = PsiManager.getInstance(project).findViewProvider(changePlace.file) ?: return
      val namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(provider.baseLanguage)
      val offset = changePlace.caretPosition?.startOffset ?: return
      val element = provider.findElementAt(offset)
      if (element != null && namesValidator.isIdentifier(element.text, project)) {
        recentPlaces.addToTop(element.text)
        val declaration = findDeclaration(element) ?: return
        for (childName in declaration.getChildrenNames().take(MAX_CHILDREN_PER_PLACE))
          childrenRecentPlaces.addToTop(childName)
      }
    }

    override fun recentPlaceRemoved(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) = Unit

    private fun PsiElement.getChildrenNames(): List<String> =
      this.children.filterIsInstance<PsiNamedElement>().mapNotNull { it.name }

    private fun findDeclaration(element: PsiElement): PsiElement? {
      var curElement = element
      while (curElement !is PsiFile) {
        if (curElement is PsiNameIdentifierOwner) return curElement
        curElement = curElement.parent ?: return null
      }
      return null
    }

    private fun<T> MutableSet<T>.addToTop(value: T) {
      this.remove(value)
      this.add(value)
    }
  }
}