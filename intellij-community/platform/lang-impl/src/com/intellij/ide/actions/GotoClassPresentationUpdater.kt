// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions

import com.intellij.ide.IdeBundle
import com.intellij.lang.IdeLanguageCustomization
import com.intellij.navigation.ChooseByNameRegistry
import com.intellij.navigation.GotoClassContributor
import com.intellij.openapi.util.text.StringUtil

object GotoClassPresentationUpdater {
  @JvmStatic
  fun getTabTitle(): String {
    val split = getActionTitle().split("/".toRegex()).take(2).toTypedArray()
    return split[0] + if (split.size > 1) " +" else ""
  }

  @JvmStatic
  fun getTabTitlePluralized(): String = getActionTitlePluralized()[0]

  @JvmStatic
  fun getActionTitle(): String =
    StringUtil.capitalizeWords(getGotoClassContributor()?.elementKind ?: IdeBundle.message("go.to.class.kind.text"), " /", true, true)

  @JvmStatic
  fun getActionTitlePluralized(): List<String> =
    (getGotoClassContributor()?.elementKindsPluralized ?:
     listOf(IdeBundle.message("go.to.class.kind.text.pluralized"))).map { StringUtil.capitalize(it) }

  @JvmStatic
  fun getElementKinds(): Set<String> {
    return getElementKinds { it.elementKind.split("/") }
  }

  @JvmStatic
  fun getElementKindsPluralized(): Set<String> {
    return getElementKinds { it.elementKindsPluralized }
  }

  private fun getGotoClassContributor(): GotoClassContributor? = ChooseByNameRegistry.getInstance().classModelContributors
    .filterIsInstance(GotoClassContributor::class.java)
    .firstOrNull { it.elementLanguage in IdeLanguageCustomization.getInstance().primaryIdeLanguages }

  private fun getElementKinds(transform: (GotoClassContributor) -> Iterable<String>): LinkedHashSet<String> {
    val primaryIdeLanguages = IdeLanguageCustomization.getInstance().primaryIdeLanguages
    return ChooseByNameRegistry.getInstance().classModelContributors
      .filterIsInstance(GotoClassContributor::class.java)
      .sortedBy {
        val index = primaryIdeLanguages.indexOf(it.elementLanguage)
        if (index == -1) primaryIdeLanguages.size else index
      }
      .flatMapTo(LinkedHashSet(), transform)
  }
}
