// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * @author Konstantin Bulenkov
 */
@State(name = "ProjectViewSharedSettings", storages = [(Storage(value = "projectView.xml"))])
class ProjectViewSharedSettings : PersistentStateComponent<ProjectViewSharedSettings> {
  var flattenPackages: Boolean = false
  var showMembers: Boolean = false
  var sortByType: Boolean = false
  var showModules: Boolean = true
  var flattenModules: Boolean = false
  var showExcludedFiles: Boolean = true
  var showVisibilityIcons: Boolean = false
  var showLibraryContents: Boolean = true
  var hideEmptyPackages: Boolean = true
  var compactDirectories: Boolean = false
  var abbreviatePackages: Boolean = false
  var autoscrollFromSource: Boolean = false
  var autoscrollToSource: Boolean = false
  var foldersAlwaysOnTop: Boolean = true
  var manualOrder: Boolean = false

  override fun getState(): ProjectViewSharedSettings? {
    return this
  }

  override fun loadState(state: ProjectViewSharedSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    val instance: ProjectViewSharedSettings
      get() = ServiceManager.getService(ProjectViewSharedSettings::class.java)
  }
}
