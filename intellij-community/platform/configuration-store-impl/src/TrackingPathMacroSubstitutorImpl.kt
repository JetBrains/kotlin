// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.PathMacroManager
import com.intellij.openapi.components.PathMacroSubstitutor
import com.intellij.openapi.components.TrackingPathMacroSubstitutor
import org.jetbrains.annotations.ApiStatus

internal fun PathMacroManager?.createTrackingSubstitutor(): TrackingPathMacroSubstitutorImpl? = if (this == null) null else TrackingPathMacroSubstitutorImpl(this)

@ApiStatus.Internal
class TrackingPathMacroSubstitutorImpl(internal val macroManager: PathMacroManager) : PathMacroSubstitutor by macroManager, TrackingPathMacroSubstitutor {
  private val lock = Object()

  private val macroToComponentNames = HashMap<String, MutableSet<String>>()
  private val componentNameToMacros = HashMap<String, MutableSet<String>>()

  override fun reset() {
    synchronized(lock) {
      macroToComponentNames.clear()
      componentNameToMacros.clear()
    }
  }

  override fun hashCode() = macroManager.expandMacroMap.hashCode()

  override fun invalidateUnknownMacros(macros: Set<String>) {
    synchronized(lock) {
      for (macro in macros) {
        val componentNames = macroToComponentNames.remove(macro) ?: continue
        for (component in componentNames) {
          componentNameToMacros.remove(component)
        }
      }
    }
  }

  override fun getComponents(macros: Collection<String>): Set<String> {
    synchronized(lock) {
      val result = HashSet<String>()
      for (macro in macros) {
        result.addAll(macroToComponentNames.get(macro) ?: continue)
      }
      return result
    }
  }

  override fun getUnknownMacros(componentName: String?): Set<String> {
    return synchronized(lock) {
      @Suppress("UNCHECKED_CAST")
      if (componentName == null) {
        macroToComponentNames.keys
      }
      else {
        componentNameToMacros.get(componentName) ?: emptySet()
      }
    }
  }

  override fun addUnknownMacros(componentName: String, unknownMacros: Collection<String>) {
    if (unknownMacros.isEmpty()) {
      return
    }

    LOG.info("Registering unknown macros ${unknownMacros.joinToString(", ")} in component $componentName")

    synchronized(lock) {
      for (unknownMacro in unknownMacros) {
        macroToComponentNames.getOrPut(unknownMacro, { HashSet() }).add(componentName)
      }

      componentNameToMacros.getOrPut(componentName, { HashSet() }).addAll(unknownMacros)
    }
  }
}

