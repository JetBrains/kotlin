// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon

import com.intellij.codeInspection.ex.ApplicationInspectionProfileManager
import com.intellij.codeInspection.ex.DEFAULT_PROFILE_NAME
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.configurationStore.serializeObjectInto
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer.deserializeInto
import org.jdom.Element

@State(name = "DaemonCodeAnalyzerSettings", storages = [Storage("editor.xml"), Storage("editor.codeinsight.xml", deprecated = true)])
open class DaemonCodeAnalyzerSettingsImpl : DaemonCodeAnalyzerSettings(), PersistentStateComponent<Element>, Cloneable {
  override fun isCodeHighlightingChanged(oldSettings: DaemonCodeAnalyzerSettings): Boolean {
    return !JDOMUtil.areElementsEqual((oldSettings as DaemonCodeAnalyzerSettingsImpl).state, state)
  }

  public override fun clone(): DaemonCodeAnalyzerSettingsImpl {
    val settings = DaemonCodeAnalyzerSettingsImpl()
    settings.autoReparseDelay = autoReparseDelay
    settings.myShowAddImportHints = myShowAddImportHints
    settings.SHOW_METHOD_SEPARATORS = SHOW_METHOD_SEPARATORS
    settings.NO_AUTO_IMPORT_PATTERN = NO_AUTO_IMPORT_PATTERN
    return settings
  }

  override fun getState(): Element? {
    val element = Element("state")
    serializeObjectInto(this, element)
    val profile = ApplicationInspectionProfileManager.getInstanceImpl().rootProfileName
    if (DEFAULT_PROFILE_NAME != profile) {
      element.setAttribute("profile", profile)
    }
    return element
  }

  override fun loadState(state: Element) {
    deserializeInto(this, state)
    val profileManager = ApplicationInspectionProfileManager.getInstanceImpl()
    profileManager.converter.storeEditorHighlightingProfile(state, InspectionProfileImpl(InspectionProfileConvertor.OLD_HIGHTLIGHTING_SETTINGS_PROFILE))
    profileManager.setRootProfile(state.getAttributeValue("profile") ?: "Default")
  }
}
