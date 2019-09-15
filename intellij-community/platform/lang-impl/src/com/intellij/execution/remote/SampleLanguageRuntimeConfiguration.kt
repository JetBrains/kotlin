// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

class SampleLanguageRuntimeConfiguration : LanguageRuntimeConfiguration(
  SampleLanguageRuntimeType.TYPE_ID),
                                           PersistentStateComponent<SampleLanguageRuntimeConfiguration.MyState> {
  var homePath: String = ""
  var applicationFolder: String = ""

  override fun getState() = MyState().also {
    it.homePath = this.homePath
    it.applicationFolder = this.applicationFolder
  }

  override fun loadState(state: MyState) {
    this.homePath = state.homePath ?: ""
    this.applicationFolder = state.applicationFolder ?: ""
  }

  class MyState : BaseState() {
    var homePath by string()
    var applicationFolder by string()
  }

}