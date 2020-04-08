/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.util.ui.StatusText
import java.awt.Graphics
import java.awt.LayoutManager
import javax.swing.JPanel

class PanelWithStatusText(
    layout: LayoutManager,
    private val statusText: String,
    var isStatusTextVisible: Boolean = false
) : JPanel(layout) {

    override fun paint(g: Graphics?) {
        super.paint(g)
        statusTextComponent.paint(this, g)
    }

    private val statusTextComponent = object : StatusText(this) {
        override fun isStatusVisible(): Boolean = isStatusTextVisible

        init {
            appendText(statusText)
        }
    }
}
