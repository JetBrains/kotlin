// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex

import com.intellij.codeInspection.InspectionProfile
import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Property

internal class VisibleTreeStateComponent : BaseState() {
  @get:Property(surroundWithTag = false)
  @get:MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false)
  var profileNameToState by map<String, VisibleTreeState>()

  fun getVisibleTreeState(profile: InspectionProfile) = profileNameToState.getOrPut(profile.name) {
    incrementModificationCount()
    VisibleTreeState()
  }
}
