// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.actions

import com.google.gson.Gson
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.stats.completion.idString
import com.intellij.stats.storage.factors.LookupStorage
import java.awt.datatransfer.StringSelection

class DumpMLCompletionFeatures : AnAction() {
  data class CommonFeatures(val user: Map<String, String>,
                            val session: Map<String, String>,
                            val context: Map<String, String>)

  data class ElementFeatures(val id: String, val features: Map<String, String>)

  companion object {
    private val gson: Gson = Gson()

    fun getCommonFeatures(lookup: LookupImpl): CommonFeatures? {
      val storage = LookupStorage.get(lookup) ?: return null
      return CommonFeatures(storage.userFactors,
                            storage.sessionFactors.getLastUsedCommonFactors(),
                            storage.contextFactors)
    }

    fun getElementFeatures(lookup: LookupImpl, element: LookupElement): ElementFeatures? {
      val storage = LookupStorage.get(lookup) ?: return null
      val id = element.idString()
      val features = storage.getItemStorage(id).getLastUsedFactors()?.mapValues { it.value.toString() } ?: return null
      return ElementFeatures(id, features)
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val lookup = LookupManager.getActiveLookup(editor) as? LookupImpl ?: return
    val commonFeatures = getCommonFeatures(lookup)
    val result = mapOf(
      "common" to commonFeatures,
      "elements" to lookup.items.mapNotNull { getElementFeatures(lookup, it) }
    )
    val json = gson.toJson(result)

    try {
      CopyPasteManager.getInstance().setContents(StringSelection(json))
    }
    catch (ignore: Exception) {
    }
  }
}