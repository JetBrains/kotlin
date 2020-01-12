// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.ide.IdeBundle
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.BoundCompositeConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*

// @formatter:off
private val model = EditorSettingsExternalizable.getInstance()
private val daemonCodeAnalyzerSettings = DaemonCodeAnalyzerSettings.getInstance()
private val uiSettings = UISettings.instance

val myCbBlinkCaret                      = CheckboxDescriptor(ApplicationBundle.message("checkbox.caret.blinking.ms"), PropertyBinding(model::isBlinkCaret, model::setBlinkCaret))
val myCbBlockCursor                     = CheckboxDescriptor(ApplicationBundle.message("checkbox.use.block.caret"), PropertyBinding(model::isBlockCursor, model::setBlockCursor))
val myCbRightMargin                     = CheckboxDescriptor(ApplicationBundle.message("checkbox.right.margin"), PropertyBinding(model::isRightMarginShown, model::setRightMarginShown))
val myCbShowLineNumbers                 = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.line.numbers"), PropertyBinding(model::isLineNumbersShown, model::setLineNumbersShown))
val myCbShowMethodSeparators            = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.method.separators"), daemonCodeAnalyzerSettings::SHOW_METHOD_SEPARATORS.toBinding())
val myWhitespacesCheckbox               = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.whitespaces"), PropertyBinding(model::isWhitespacesShown, model::setWhitespacesShown))
val myLeadingWhitespacesCheckBox        = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.leading.whitespaces"), PropertyBinding(model::isLeadingWhitespacesShown, model::setLeadingWhitespacesShown))
val myInnerWhitespacesCheckBox          = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.inner.whitespaces"), PropertyBinding(model::isInnerWhitespacesShown, model::setInnerWhitespacesShown))
val myTrailingWhitespacesCheckBox       = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.trailing.whitespaces"), PropertyBinding(model::isTrailingWhitespacesShown, model::setTrailingWhitespacesShown))
val myShowVerticalIndentGuidesCheckBox  = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.indent.guides"), PropertyBinding(model::isIndentGuidesShown, model::setIndentGuidesShown))
val myFocusModeCheckBox                 = CheckboxDescriptor(ApplicationBundle.message("checkbox.highlight.only.current.declaration"), PropertyBinding(model::isFocusMode, model::setFocusMode))
val myCbShowIntentionBulbCheckBox       = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.intention.bulb"), PropertyBinding(model::isShowIntentionBulb, model::setShowIntentionBulb))
val myCodeLensCheckBox                  = CheckboxDescriptor(IdeBundle.message("checkbox.show.editor.preview.popup"), uiSettings::showEditorToolTip)
// @formatter:on

class EditorAppearanceConfigurable : BoundCompositeConfigurable<UnnamedConfigurable>(
  ApplicationBundle.message("tab.editor.settings.appearance"),
  "reference.settingsdialog.IDE.editor.appearance"
), Configurable.WithEpDependencies {
  override fun createPanel(): DialogPanel {
    val model = EditorSettingsExternalizable.getInstance()
    return panel {
      row {
        cell {
          val cbBlinkCaret = checkBox(myCbBlinkCaret)
          intTextField(model::getBlinkPeriod, model::setBlinkPeriod, columns = 5).enableIf(cbBlinkCaret.selected)
        }
      }
      row {
        checkBox(myCbBlockCursor)
      }
      row {
        checkBox(myCbRightMargin)
      }
      row {
        checkBox(myCbShowLineNumbers)
      }
      row {
        checkBox(myCbShowMethodSeparators)
      }
      row {
        val cbWhitespace = checkBox(myWhitespacesCheckbox)
        row {
          checkBox(myLeadingWhitespacesCheckBox).enableIf(cbWhitespace.selected)
        }
        row {
          checkBox(myInnerWhitespacesCheckBox).enableIf(cbWhitespace.selected)
        }
        row {
          checkBox(myTrailingWhitespacesCheckBox).enableIf(cbWhitespace.selected)
        }
      }
      row {
        checkBox(myShowVerticalIndentGuidesCheckBox)
      }
      if (ApplicationManager.getApplication().isInternal) {
        row {
          checkBox(myFocusModeCheckBox)
        }
      }
      row {
        checkBox(myCbShowIntentionBulbCheckBox)
      }
      row {
        checkBox(myCodeLensCheckBox)
      }.largeGapAfter()

      for (configurable in configurables) {
        row {
          configurable.createComponent()?.invoke(growX)
        }.largeGapAfter()
      }
    }
  }

  override fun createConfigurables(): List<UnnamedConfigurable> {
    return ConfigurableWrapper.createConfigurables(EP_NAME)
  }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> {
    return listOf(EP_NAME)
  }

  override fun apply() {
    val showEditorTooltip = UISettings.instance.showEditorToolTip

    super.apply()

    EditorOptionsPanel.reinitAllEditors()
    if (showEditorTooltip != UISettings.instance.showEditorToolTip) {
      LafManager.getInstance().repaintUI()
      uiSettings.fireUISettingsChanged()
    }

    EditorOptionsPanel.restartDaemons()
    ApplicationManager.getApplication().messageBus.syncPublisher(EditorOptionsListener.APPEARANCE_CONFIGURABLE_TOPIC).changesApplied()
  }

  companion object {
    private val EP_NAME = ExtensionPointName.create<EditorAppearanceConfigurableEP>("com.intellij.editorAppearanceConfigurable")
  }
}

