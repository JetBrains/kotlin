// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.documentation.render.DocRenderManager
import com.intellij.ide.IdeBundle
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.BoundCompositeSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.*

// @formatter:off
private val model = EditorSettingsExternalizable.getInstance()
private val daemonCodeAnalyzerSettings = DaemonCodeAnalyzerSettings.getInstance()
private val uiSettings = UISettings.instance

private val myCbBlinkCaret                            get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.caret.blinking.ms"), PropertyBinding(model::isBlinkCaret, model::setBlinkCaret))
private val myCbBlockCursor                           get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.use.block.caret"), PropertyBinding(model::isBlockCursor, model::setBlockCursor))
private val myCbRightMargin                           get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.right.margin"), PropertyBinding(model::isRightMarginShown, model::setRightMarginShown))
private val myCbShowLineNumbers                       get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.line.numbers"), PropertyBinding(model::isLineNumbersShown, model::setLineNumbersShown))
private val myCbShowMethodSeparators                  get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.method.separators"), daemonCodeAnalyzerSettings::SHOW_METHOD_SEPARATORS.toBinding())
private val myWhitespacesCheckbox                     get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.whitespaces"), PropertyBinding(model::isWhitespacesShown, model::setWhitespacesShown))
private val myLeadingWhitespacesCheckBox              get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.leading.whitespaces"), PropertyBinding(model::isLeadingWhitespacesShown, model::setLeadingWhitespacesShown))
private val myInnerWhitespacesCheckBox                get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.inner.whitespaces"), PropertyBinding(model::isInnerWhitespacesShown, model::setInnerWhitespacesShown))
private val myTrailingWhitespacesCheckBox             get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.trailing.whitespaces"), PropertyBinding(model::isTrailingWhitespacesShown, model::setTrailingWhitespacesShown))
private val myShowVerticalIndentGuidesCheckBox        get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.indent.guides"), PropertyBinding(model::isIndentGuidesShown, model::setIndentGuidesShown))
private val myFocusModeCheckBox                       get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.highlight.only.current.declaration"), PropertyBinding(model::isFocusMode, model::setFocusMode))
private val myCbShowIntentionBulbCheckBox             get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.show.intention.bulb"), PropertyBinding(model::isShowIntentionBulb, model::setShowIntentionBulb))
private val myCodeLensCheckBox                        get() = CheckboxDescriptor(IdeBundle.message("checkbox.show.editor.preview.popup"), uiSettings::showEditorToolTip)
private val myRenderedDocCheckBox                     get() = CheckboxDescriptor(IdeBundle.message("checkbox.show.rendered.doc.comments"), PropertyBinding(model::isDocCommentRenderingEnabled, model::setDocCommentRenderingEnabled))
// @formatter:on

class EditorAppearanceConfigurable : BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
  ApplicationBundle.message("tab.editor.settings.appearance"),
  "reference.settingsdialog.IDE.editor.appearance",
  "editor.preferences.appearance"
), Configurable.WithEpDependencies {
  override fun createPanel(): DialogPanel {
    val model = EditorSettingsExternalizable.getInstance()
    return panel {
      row {
        cell(isFullWidth = true) {
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
      }
      row {
        checkBox(myRenderedDocCheckBox)
      }

      for (configurable in configurables) {
        appendDslConfigurableRow(configurable)
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
    val docRenderingEnabled = EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled

    super.apply()

    EditorOptionsPanel.reinitAllEditors()
    if (showEditorTooltip != UISettings.instance.showEditorToolTip) {
      LafManager.getInstance().repaintUI()
      uiSettings.fireUISettingsChanged()
    }
    if (docRenderingEnabled != EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled) {
      DocRenderManager.resetAllEditorsToDefaultState()
    }

    EditorOptionsPanel.restartDaemons()
    ApplicationManager.getApplication().messageBus.syncPublisher(EditorOptionsListener.APPEARANCE_CONFIGURABLE_TOPIC).changesApplied()
  }

  companion object {
    private val EP_NAME = ExtensionPointName.create<EditorAppearanceConfigurableEP>("com.intellij.editorAppearanceConfigurable")
  }
}

