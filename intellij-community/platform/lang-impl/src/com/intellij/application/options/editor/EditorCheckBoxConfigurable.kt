// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.ide.ui.search.OptionDescription
import com.intellij.openapi.application.ApplicationBundle.message
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.layout.*

// @formatter:off
private val editor = EditorSettingsExternalizable.getInstance()
private val codeInsightSettings = CodeInsightSettings.getInstance()

private val honorCamelHumpsWhenSelectingByClicking                     get() = CheckboxDescriptor(message("checkbox.honor.camelhumps.words.settings.on.double.click"), PropertyBinding(editor::isMouseClickSelectionHonorsCamelWords, editor::setMouseClickSelectionHonorsCamelWords))

private val enableWheelFontChange                                      get() = CheckboxDescriptor(if (SystemInfo.isMac) message("checkbox.enable.ctrl.mousewheel.changes.font.size.macos") else message("checkbox.enable.ctrl.mousewheel.changes.font.size"), PropertyBinding(editor::isWheelFontChangeEnabled, editor::setWheelFontChangeEnabled))

private val enableDnD                                                  get() = CheckboxDescriptor(message("checkbox.enable.drag.n.drop.functionality.in.editor"), PropertyBinding(editor::isDndEnabled, editor::setDndEnabled))
private val virtualSpace                                               get() = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.after.end.of.line"), PropertyBinding(editor::isVirtualSpace, editor::setVirtualSpace))
private val caretInsideTabs                                            get() = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.inside.tabs"), PropertyBinding(editor::isCaretInsideTabs, editor::setCaretInsideTabs))
private val virtualPageAtBottom                                        get() = CheckboxDescriptor(message("checkbox.show.virtual.space.at.file.bottom"), PropertyBinding(editor::isAdditionalPageAtBottom, editor::setAdditionalPageAtBottom))

private val highlightBraces                                            get() = CheckboxDescriptor(message("checkbox.highlight.matched.brace"), codeInsightSettings::HIGHLIGHT_BRACES)
private val highlightScope                                             get() = CheckboxDescriptor(message("checkbox.highlight.current.scope"), codeInsightSettings::HIGHLIGHT_SCOPE)
private val highlightIdentifierUnderCaret                              get() = CheckboxDescriptor(message("checkbox.highlight.usages.of.element.at.caret"), codeInsightSettings::HIGHLIGHT_IDENTIFIER_UNDER_CARET)

private val showNotificationAfterReformatCodeCheckBox                  get() = CheckboxDescriptor(message("checkbox.show.notification.after.reformat.code.action"), PropertyBinding(editor::isShowNotificationAfterReformat, editor::setShowNotificationAfterReformat))
private val myShowNotificationAfterOptimizeImportsCheckBox             get() = CheckboxDescriptor(message("checkbox.show.notification.after.optimize.imports.action"), PropertyBinding(editor::isShowNotificationAfterOptimizeImports, editor::setShowNotificationAfterOptimizeImports))
private val renameLocalVariablesInplace                                get() = CheckboxDescriptor(message("checkbox.rename.local.variables.inplace"), PropertyBinding(editor::isVariableInplaceRenameEnabled, editor::setVariableInplaceRenameEnabled))
private val preselectCheckBox                                          get() = CheckboxDescriptor(message("checkbox.rename.local.variables.preselect"), PropertyBinding(editor::isPreselectRename, editor::setPreselectRename))
private val showInlineDialogForCheckBox                                get() = CheckboxDescriptor(message("checkbox.show.inline.dialog.on.local.variable.references"), PropertyBinding(editor::isShowInlineLocalDialog, editor::setShowInlineLocalDialog))
// @formatter:on

internal val optionDescriptors: List<OptionDescription> = listOf(
  honorCamelHumpsWhenSelectingByClicking,
  enableWheelFontChange,
  enableDnD,
  virtualSpace,
  caretInsideTabs,
  virtualPageAtBottom,
  highlightBraces,
  highlightScope,
  highlightIdentifierUnderCaret,
  showNotificationAfterReformatCodeCheckBox,
  myShowNotificationAfterOptimizeImportsCheckBox,
  renameLocalVariablesInplace,
  preselectCheckBox,
  showInlineDialogForCheckBox
).map(CheckboxDescriptor::asOptionDescriptor)

class EditorCheckBoxConfigurable : BoundConfigurable(""), SearchableConfigurable {
  override fun getId() = ""

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(message("group.advanced.mouse.usages")) {
        row { checkBox(honorCamelHumpsWhenSelectingByClicking) }
        row { checkBox(enableWheelFontChange) }
        row { checkBox(enableDnD) }
      }
      titledRow(message("group.virtual.space")) {
        row { checkBox(virtualSpace) }
        row { checkBox(caretInsideTabs) }
        row { checkBox(virtualPageAtBottom) }
      }
      titledRow(message("group.brace.highlighting")) {
        row { checkBox(highlightBraces) }
        row { checkBox(highlightScope) }
        row { checkBox(highlightIdentifierUnderCaret) }
      }
      titledRow(message("group.formatting")) {
        row { checkBox(showNotificationAfterReformatCodeCheckBox) }
        row { checkBox(myShowNotificationAfterOptimizeImportsCheckBox) }
      }
      titledRow(message("group.refactorings")) {
        row { checkBox(renameLocalVariablesInplace) }
        row { checkBox(preselectCheckBox) }
        row { checkBox(showInlineDialogForCheckBox) }
      }
    }
  }
}