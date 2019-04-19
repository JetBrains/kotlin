// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.ui

import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Window
import javax.swing.JDialog
import javax.swing.JPanel

@JvmOverloads
fun dialog(title: String,
           owner: Window? = null,
           modalityType: Dialog.ModalityType = Dialog.ModalityType.APPLICATION_MODAL,
           northPanel: JPanel = JPanel(),
           southPanel: JPanel = JPanel(),
           centerPanel: JPanel = JPanel(),
           westPanel: JPanel = JPanel(),
           eastPanel: JPanel = JPanel()
           ) : JDialog {
  return object : JDialog(owner, title, modalityType) {

    private val contentPanel = JPanel(BorderLayout())

    init {
      contentPane = contentPanel
      contentPanel.add(northPanel, BorderLayout.NORTH)
      contentPanel.add(southPanel, BorderLayout.SOUTH)
      contentPanel.add(centerPanel, BorderLayout.CENTER)
      contentPanel.add(westPanel, BorderLayout.WEST)
      contentPanel.add(eastPanel, BorderLayout.EAST)

      pack()
      setLocationRelativeTo(owner)
      isVisible = true
    }
  }
}