// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.Language
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase

class InlaySettingsTest : LightPlatformTestCase() {

  fun testSettingsPersisted() {
    val settings = InlayHintsSettings()
    val language = object: Language("foobar1") {}
    data class ToPersist(var x: String, var y: Int) {
      @Suppress("unused")
      constructor() : this("", 0)
    }
    val key = SettingsKey<ToPersist>("foo")
    val saved = ToPersist("test", 42)
    settings.storeSettings(key, language, saved)
    settings.changeHintTypeStatus(key, language, false)
    settings.saveLastViewedProviderId("foo")
    val state = settings.state

    val newSettings = InlayHintsSettings()
    newSettings.loadState(state)
    val loaded = newSettings.findSettings(key, language) { ToPersist("asdasd", 23) }
    assertEquals(saved, loaded)
    assertFalse(newSettings.hintsEnabled(key, language))
    assertEquals("foo", newSettings.getLastViewedProviderId())
  }

  fun testLastProviderKey() {
    val settings = InlayHintsSettings()
    assertEquals(null, settings.getLastViewedProviderId())
    settings.saveLastViewedProviderId("k1")
    assertEquals("k1", settings.getLastViewedProviderId())
    settings.saveLastViewedProviderId("k2")
    assertEquals("k2", settings.getLastViewedProviderId())
  }

  fun testGlobalSwitch() {
    val settings = InlayHintsSettings()
    val language = object: Language("foobar2") {}
    assertTrue(settings.hintsEnabled(language)) // hints enabled by default
    settings.setEnabledGlobally(false)
    assertTrue(settings.hintsEnabled(language))
    assertFalse(settings.hintsShouldBeShown(language))
  }

  fun testLanguageSwitch() {
    val settings = InlayHintsSettings()
    val language = object: Language("foobar3") {}
    val key = SettingsKey<Any>("foo")
    assertTrue(settings.hintsEnabled(key, language))
    settings.setHintsEnabledForLanguage(language, false)
    assertTrue(settings.hintsEnabled(key, language))
    assertFalse(settings.hintsShouldBeShown(key, language))
  }
}