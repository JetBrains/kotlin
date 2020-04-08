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
import kotlin.collections.LinkedHashMap

class RecentPlacesFeatures : ElementFeatureProvider {
  override fun getName(): String = "common_recent_places"

  override fun calculateFeatures(element: LookupElement,
                                 location: CompletionLocation,
                                 contextFeatures: ContextFeatures): MutableMap<String, MLFeatureValue> {
    val features = mutableMapOf<String, MLFeatureValue>()
    if (StoreRecentPlacesListener.recentPlaces.contains(element.lookupString))
      features["contains"] = MLFeatureValue.binary(true)
    if (StoreRecentPlacesListener.childrenRecentPlaces.contains(element.lookupString))
      features["children_contains"] = MLFeatureValue.binary(true)
      return features
  }

  class StoreRecentPlacesListener(private val project: Project) : IdeDocumentHistoryImpl.RecentPlacesListener {
    companion object {
      val recentPlaces = createFixedSizeSet(20)
      val childrenRecentPlaces = createFixedSizeSet(100)

      private fun createFixedSizeSet(maxSize: Int): MutableSet<String> =
        Collections.newSetFromMap(object : LinkedHashMap<String, Boolean>() {
          override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>): Boolean {
            return size > maxSize
          }
        })
    }

    override fun recentPlaceAdded(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) {
      if (ApplicationManager.getApplication().isUnitTestMode) return
      val provider = PsiManager.getInstance(project).findViewProvider(changePlace.file) ?: return
      val namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(provider.baseLanguage)
      val offset = changePlace.caretPosition?.startOffset ?: return
      val element = provider.findElementAt(offset)
      if (element != null && namesValidator.isIdentifier(element.text, project)) {
        recentPlaces.addToTop(element.text)
        val declaration = findDeclaration(element) ?: return
        for(child in declaration.children) {
          if (child is PsiNamedElement) {
            val name = child.name ?: continue
            childrenRecentPlaces.addToTop(name)
          }
        }
      }
    }

    override fun recentPlaceRemoved(changePlace: IdeDocumentHistoryImpl.PlaceInfo, isChanged: Boolean) = Unit

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