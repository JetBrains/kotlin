// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.actions

import com.intellij.ide.actions.NonTrivialActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces

class ExtractActionGroup : NonTrivialActionGroup() {
  override fun isPopup(place: String): Boolean {
    return place == ActionPlaces.MAIN_MENU
  }
}
