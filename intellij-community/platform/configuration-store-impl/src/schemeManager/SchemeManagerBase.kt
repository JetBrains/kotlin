// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.openapi.options.SchemeProcessor
import com.intellij.openapi.options.SchemesManager

abstract class SchemeManagerBase<T : Any, in MUTABLE_SCHEME : T>(internal val processor: SchemeProcessor<T, MUTABLE_SCHEME>) : SchemesManager<T>() {
  /**
   * Schemes can be lazy loaded, so, client should be able to set current scheme by name, not only by instance.
   */
  @Volatile
  internal var currentPendingSchemeName: String? = null

  override var activeScheme: T? = null
    internal set

  override var currentSchemeName: String?
    get() = activeScheme?.let { processor.getSchemeKey(it) } ?: currentPendingSchemeName
    set(schemeName) = setCurrentSchemeName(schemeName, true)

  internal fun processPendingCurrentSchemeName(newScheme: T): Boolean {
    if (processor.getSchemeKey(newScheme) == currentPendingSchemeName) {
      setCurrent(newScheme, false)
      return true
    }
    return false
  }

  override fun setCurrent(scheme: T?, notify: Boolean, processChangeSynchronously: Boolean) {
    currentPendingSchemeName = null

    val oldCurrent = activeScheme
    activeScheme = scheme
    if (notify && oldCurrent !== scheme) {
      processor.onCurrentSchemeSwitched(oldCurrent, scheme, processChangeSynchronously)
    }
  }

  override fun setCurrentSchemeName(schemeName: String?, notify: Boolean) {
    currentPendingSchemeName = schemeName

    val scheme = schemeName?.let { findSchemeByName(it) }
    // don't set current scheme if no scheme by name - pending resolution (see currentSchemeName field comment)
    if (scheme != null || schemeName == null) {
      setCurrent(scheme, notify)
    }
  }
}