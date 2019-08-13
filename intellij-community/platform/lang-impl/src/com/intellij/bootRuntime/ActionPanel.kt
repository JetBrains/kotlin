// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime

import java.awt.GridBagLayout
import javax.swing.JPanel

class ActionPanel : JPanel(GridBagLayout()) {

  override fun addNotify() {
    super.addNotify()
    this.minimumSize = preferredSize
  }
}
