// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.configurationStore.deserializeInto
import com.intellij.configurationStore.serialize
import com.intellij.lang.Language
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import org.jdom.Element

@State(name = "InlayHintsSettings")
class InlayHintsSettings : PersistentStateComponent<InlayHintsSettings.State> {
  private var myState = State()
  private val lock = Any()

  class State {
    var disabledHintProviderIds: MutableSet<String> = hashSetOf()
    // We can't store Map<String, Any> directly, because values deserialized as Object
    var settingsMapElement = Element("settingsMapElement")
  }

  private val myCachedSettingsMap: MutableMap<String, Any> = hashMapOf()

  fun changeHintTypeStatus(key: SettingsKey<*>, language: Language, enable: Boolean) = synchronized(lock) {
    val id = key.getFullId(language)
    if (enable) {
      myState.disabledHintProviderIds.remove(id)
    } else {
      myState.disabledHintProviderIds.add(id)
    }
  }

  fun invertHintTypeStatus(key: SettingsKey<*>, language: Language) = synchronized(lock) {
    val id = key.getFullId(language)
    if (id in myState.disabledHintProviderIds) {
      myState.disabledHintProviderIds.remove(id)
    }
    else {
      myState.disabledHintProviderIds.add(id)
    }
  }

  /**
   * @param uninitSettings is a setting, that was obtained from createSettings method of provider
   */
  fun <T: Any> findSettings(key: SettingsKey<T>, language: Language, uninitSettings: T): T = synchronized(lock) {
    val fullId = key.getFullId(language)
    @Suppress("UNCHECKED_CAST")
    return getSettingCached(fullId, uninitSettings) as T
  }

  fun <T: Any> storeSettings(key: SettingsKey<T>, language: Language, value: T) = synchronized(lock){
    val fullId = key.getFullId(language)
    myCachedSettingsMap[fullId] = value as Any
    val element = myState.settingsMapElement.clone()
    element.removeChild(fullId)
    val serialized = value.serialize()
    if (serialized != null) {
      val storeElement = Element(fullId)
      val wrappedSettingsElement = storeElement.addContent(serialized)
      myState.settingsMapElement = element.addContent(wrappedSettingsElement)
      element.sortAttributes(compareBy { it.name })
    } else {
      myState.settingsMapElement = element
    }
  }

  fun hintsEnabled(key: SettingsKey<*>, language: Language) : Boolean = synchronized(lock) {
    return key.getFullId(language) !in myState.disabledHintProviderIds
  }

  override fun getState(): State = synchronized(lock) {
    return myState
  }

  override fun loadState(state: State) = synchronized(lock) {
    val elementChanged = myState.settingsMapElement != state.settingsMapElement
    if (elementChanged) {
      myCachedSettingsMap.clear()
    }
    myState = state
  }

  // may return parameter settings object or cached object
  private fun getSettingCached(id: String, settings: Any): Any {
    val cachedValue = myCachedSettingsMap[id]
    if (cachedValue != null) return cachedValue
    return getSettingNotCached(id, settings)
  }

  private fun getSettingNotCached(id: String, settings: Any): Any {
    val state = myState.settingsMapElement
    val settingsElement = state.getChild(id) ?: return settings
    val settingsElementChildren= settingsElement.children
    if (settingsElementChildren.isEmpty()) return settings
    settingsElementChildren.first().deserializeInto(settings)
    myCachedSettingsMap[id] = settings
    return settings
  }
}

/**
 * Similar to Key, but it also requires language to be unique
 */
@Suppress("unused")
data class SettingsKey<T>(val id: String) {
  fun getFullId(language: Language) : String = language.id + "." + id
}