// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.EmptyIcon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

internal class RunConfigurableTreeRenderer(private val runManager: RunManagerImpl) : ColoredTreeCellRenderer() {
  override fun customizeCellRenderer(tree: JTree, value: Any, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
    if (value !is DefaultMutableTreeNode) {
      return
    }

    val userObject = value.userObject
    var isShared: Boolean? = null
    val name = getUserObjectName(userObject)
    when {
      userObject is ConfigurationType -> {
        append(name, if ((value.parent as DefaultMutableTreeNode).isRoot) SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
        icon = userObject.icon
      }
      userObject === TEMPLATES_NODE_USER_OBJECT -> {
        append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        icon = AllIcons.General.Settings
      }
      userObject is String -> {
        // folder
        append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        icon = AllIcons.Nodes.Folder
      }
      userObject is ConfigurationFactory -> {
        append(name)
        icon = userObject.icon
      }
      else -> {
        var configuration: RunnerAndConfigurationSettings? = null
        if (userObject is SingleConfigurationConfigurable<*>) {
          val configurationSettings: RunnerAndConfigurationSettings = userObject.settings
          configuration = configurationSettings
          isShared = userObject.isStoreProjectConfiguration
          icon = ProgramRunnerUtil.getConfigurationIcon(configurationSettings, !userObject.isValid)
        }
        else if (userObject is RunnerAndConfigurationSettingsImpl) {
          val settings = userObject as RunnerAndConfigurationSettings
          isShared = settings.isShared
          icon = runManager.getConfigurationIcon(settings)
          configuration = settings
        }
        if (configuration != null) {
          append(name, if (configuration.isTemporary) SimpleTextAttributes.GRAY_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
      }
    }

    if (isShared == null) {
      iconTextGap = 2
    }
    else {
      icon = LayeredIcon(icon, if (isShared) AllIcons.Nodes.Shared else EmptyIcon.ICON_16)
      iconTextGap = 0
    }
  }
}