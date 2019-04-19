// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime

import com.intellij.bootRuntime.BundleState.*
import com.intellij.bootRuntime.bundles.Local
import com.intellij.bootRuntime.bundles.Runtime
import com.intellij.bootRuntime.command.Cleanup
import com.intellij.bootRuntime.command.RuntimeCommand
import com.intellij.bootRuntime.command.CommandFactory
import com.intellij.bootRuntime.command.CommandFactory.Type.*
import com.intellij.bootRuntime.command.CommandFactory.produce
import com.intellij.bootRuntime.command.UseDefault
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBOptionButton
import com.intellij.util.ui.UIUtil
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.JButton
import javax.swing.SwingUtilities

class Controller(val project: Project, val actionPanel:ActionPanel, val model: Model) {

  init {
    CommandFactory.initialize(project, this)
  }

  fun updateRuntime() {
    runtimeSelected(model.selectedBundle)
  }

  // the fun is supposed to be invoked on the combobox selection
  fun runtimeSelected(runtime:Runtime) {
    model.updateBundle(runtime)
    actionPanel.removeAll()
    val list = runtimeStateToActions(runtime, model.currentState()).toList()

    val job = JBOptionButton(list.firstOrNull(), list.subList(1, list.size).toTypedArray())

    val constraint = GridBagConstraints()
    constraint.insets = Insets(0,0,0, 0)
    constraint.weightx = 1.0
    constraint.anchor = GridBagConstraints.WEST

    val resetButton = JButton(UseDefault(project, this))
    resetButton.toolTipText = "Reset boot Runtime to the default one"
    resetButton.isEnabled = BinTrayUtil.getJdkConfigFilePath().exists()


    actionPanel.add(resetButton, constraint)
    val cleanButton = JButton(Cleanup(project, this))
    cleanButton.toolTipText = "Remove all installed runtimes"
    actionPanel.add(cleanButton, constraint )

    constraint.anchor = GridBagConstraints.EAST

    actionPanel.rootPane?.defaultButton = job

    actionPanel.add(job, constraint)
    actionPanel.repaint()
    actionPanel.revalidate()
  }

  private fun runtimeStateToActions(runtime:Runtime, currentState: BundleState) : List<RuntimeCommand> {
    return when (currentState) {
      REMOTE -> listOf(produce(REMOTE_INSTALL, runtime), produce(DOWNLOAD, runtime))
      DOWNLOADED -> listOf(produce(INSTALL, runtime), produce(DELETE, runtime))
      EXTRACTED -> listOf(produce(INSTALL, runtime), produce(DELETE, runtime))
      UNINSTALLED -> listOf(produce(INSTALL, runtime), produce(DELETE, runtime))
      INSTALLED -> listOf(produce(UNINSTALL, runtime))
    }
  }

  fun add(local: Local) {
    model.bundles.add(local)
    model.selectedBundle = local
  }

  fun restart() {
    ApplicationManager.getApplication().invokeLater {
      SwingUtilities.getWindowAncestor(actionPanel).dispose()
      ApplicationManagerEx.getApplicationEx().restart(true)
    }
  }

  fun noRuntimeSelected() {
    UIUtil.uiTraverser(actionPanel).filter(JButton::class.java).forEach{b -> b.isEnabled = false}
  }
}
