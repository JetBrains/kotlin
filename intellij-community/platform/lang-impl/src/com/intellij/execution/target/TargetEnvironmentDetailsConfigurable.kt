// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.target

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.DropDownLink
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.layout.*
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

internal class TargetEnvironmentDetailsConfigurable(private val project: Project, private val config: TargetEnvironmentConfiguration)
  : NamedConfigurable<TargetEnvironmentConfiguration>(true, null) {

  private val targetConfigurable: Configurable = config.getTargetType().createConfigurable(project, config)
  private val runtimeConfigurables = mutableListOf<Configurable>()

  override fun getBannerSlogan(): String = config.displayName

  override fun getIcon(expanded: Boolean): Icon? = config.getTargetType().icon

  override fun isModified(): Boolean = allConfigurables().any { it.isModified }

  override fun getDisplayName(): String = config.displayName

  override fun apply() = allConfigurables().forEach { it.apply() }

  override fun setDisplayName(name: String) {
    config.displayName = name
  }

  override fun disposeUIResources() {
    super.disposeUIResources()
    allConfigurables().forEach { it.disposeUIResources() }
  }

  override fun getEditableObject() = config

  override fun createOptionsPanel(): JComponent {
    val panel = JPanel(VerticalLayout(JBUIScale.scale(UIUtil.DEFAULT_VGAP)))
    panel.border = JBUI.Borders.empty(0, 10, 10, 10)

    panel.add(targetConfigurable.createComponent() ?: throw IllegalStateException())

    config.runtimes.resolvedConfigs().forEach {
      panel.add(createRuntimePanel(it))
    }
    panel.add(createAddRuntimeHyperlink())
    return JBScrollPane(panel).also {
      it.border = JBUI.Borders.empty()
    }
  }

  private fun createRuntimePanel(runtime: LanguageRuntimeConfiguration): JPanel {
    return panel {
      row {
        val separator = TitledSeparator(runtime.getRuntimeType().configurableDescription)
        separator(CCFlags.growX, CCFlags.pushX)
        gearButton(DuplicateRuntimeAction(runtime), RemoveRuntimeAction(runtime))
      }
      row {
        val languageUI = runtime.getRuntimeType().createConfigurable(project, runtime)
          .also { runtimeConfigurables.add(it) }
          .let {
            it.createComponent() ?: throw IllegalStateException("for runtime: $runtime")
          }
        languageUI(CCFlags.growX)
      }
    }
  }

  private fun createAddRuntimeHyperlink(): JLabel {
    class Item(val type: LanguageRuntimeType<*>?) {
      override fun toString(): String {
        return type?.displayName ?: "Add language runtime"
      }
    }

    return DropDownLink<Item>(Item(null), LanguageRuntimeType.EXTENSION_NAME.extensionList.map { Item(it) }, {
      val newRuntime = it.type?.createDefaultConfig() ?: return@DropDownLink
      config.runtimes.addConfig(newRuntime)
      forceRefreshUI()
    }, false)
  }

  private fun allConfigurables() = sequenceOf(targetConfigurable) + runtimeConfigurables.asSequence()

  override fun resetOptionsPanel() {
    runtimeConfigurables.clear()
    super.resetOptionsPanel()
  }

  private fun forceRefreshUI() {
    resetOptionsPanel()
    createComponent()?.revalidate()
  }

  private abstract inner class ChangeRuntimeActionBase(protected val runtime: LanguageRuntimeConfiguration, text: String) : AnAction(text)

  private inner class DuplicateRuntimeAction(runtime: LanguageRuntimeConfiguration) : ChangeRuntimeActionBase(runtime, "Duplicate") {
    override fun actionPerformed(e: AnActionEvent) {
      val copy = runtime.getRuntimeType().duplicateConfig(runtime)
      config.runtimes.addConfig(copy)
      forceRefreshUI()
    }
  }

  private inner class RemoveRuntimeAction(runtime: LanguageRuntimeConfiguration) : ChangeRuntimeActionBase(runtime, "Remove") {
    override fun actionPerformed(e: AnActionEvent) {
      config.runtimes.removeConfig(runtime)
      forceRefreshUI()
    }
  }
}