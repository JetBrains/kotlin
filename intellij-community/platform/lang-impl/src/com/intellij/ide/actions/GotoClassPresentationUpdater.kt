// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions

import com.intellij.ide.IdeBundle
import com.intellij.lang.IdeLanguageCustomization
import com.intellij.navigation.ChooseByNameRegistry
import com.intellij.navigation.GotoClassContributor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.text.StringUtil

class GotoClassPresentationUpdater : PreloadingActivity() {
  override fun preload(indicator: ProgressIndicator) {
    //we need to change the template presentation to show the proper text for the action in Settings | Keymap
    val presentation = ActionManager.getInstance().getAction("GotoClass").templatePresentation
    presentation.text = getActionTitle() + "..."
    presentation.description = IdeBundle.message("go.to.class.action.description", getElementKinds().joinToString("/"))
  }

  companion object {
    @JvmStatic
    fun getTabTitle(pluralize: Boolean): String {
      val split = getActionTitle().split("/".toRegex()).take(2).toTypedArray()
      return if (pluralize) StringUtil.pluralize(split[0]) else split[0] + if (split.size > 1) " +" else ""
    }

    @JvmStatic
    fun getActionTitle(): String {
      val primaryIdeLanguages = IdeLanguageCustomization.getInstance().primaryIdeLanguages
      val mainContributor = ChooseByNameRegistry.getInstance().classModelContributors
                              .filterIsInstance(GotoClassContributor::class.java)
                              .firstOrNull { it.elementLanguage in primaryIdeLanguages }
      val text = mainContributor?.elementKind ?: IdeBundle.message("go.to.class.kind.text")
      return StringUtil.capitalizeWords(text, " /", true, true)
    }

    @JvmStatic
    fun getElementKinds(): Set<String> {
      val primaryIdeLanguages = IdeLanguageCustomization.getInstance().primaryIdeLanguages
      return ChooseByNameRegistry.getInstance().classModelContributors
        .filterIsInstance(GotoClassContributor::class.java)
        .sortedBy {
          val index = primaryIdeLanguages.indexOf(it.elementLanguage)
          if (index == -1) primaryIdeLanguages.size else index
        }
        .flatMapTo(LinkedHashSet()) { it.elementKind.split("/") }
    }
  }
}
