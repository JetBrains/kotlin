// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.TitledSeparator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.layout.*
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

internal class RemoteTargetDetailsConfigurable(private val project: Project, private val config: RemoteTargetConfiguration)
  : NamedConfigurable<RemoteTargetConfiguration>(true, null) {

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
    val result = JPanel(VerticalLayout(JBUIScale.scale(UIUtil.DEFAULT_VGAP)))
    result.border = JBUI.Borders.empty(4, 10, 6, 10)

    result.add(targetConfigurable.createComponent() ?: throw IllegalStateException())

    config.runtimes.resolvedConfigs().forEach {
      result.add(createRuntimePanel(it))
    }
    result.add(createAddRuntimeHyperlink())
    return result
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
        languageUI(CCFlags.growX, CCFlags.pushX)
      }
    }
  }

  private fun createAddRuntimeHyperlink(): HyperlinkLabel {
    val result = HyperlinkLabel()
    result.setHyperlinkText("Add language runtime")
    result.addHyperlinkListener { e ->
      val types = LanguageRuntimeType.allTypes()
      val popup = JBPopupFactory.getInstance().createListPopup(
        object : BaseListPopupStep<LanguageRuntimeType<*>>("Choose runtime type", types, emptyArray()) {
          override fun getTextFor(runtime: LanguageRuntimeType<*>?) = runtime!!.displayName

          override fun onChosen(selectedValue: LanguageRuntimeType<*>?, finalChoice: Boolean): PopupStep<*>? = doFinalStep {
            selectedValue?.also {
              val newRuntime = it.createDefaultConfig()
              config.runtimes.addConfig(newRuntime)
              forceRefreshUI()
            }
          }
        }
      )
      if (e.inputEvent is MouseEvent) {
        popup.show(RelativePoint(e.inputEvent as MouseEvent))
      }
      else {
        popup.showInCenterOf(result)
      }
    }
    return result
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