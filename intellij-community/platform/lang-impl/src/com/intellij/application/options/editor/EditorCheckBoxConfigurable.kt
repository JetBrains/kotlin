// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.ide.ui.search.OptionDescription
import com.intellij.openapi.application.ApplicationBundle.message
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.layout.*

private val editor = EditorSettingsExternalizable.getInstance()

val honorCamelHumpsWhenSelectingByClicking = CheckboxDescriptor(
  message("checkbox.honor.camelhumps.words.settings.on.double.click"),
  PropertyBinding(editor::isMouseClickSelectionHonorsCamelWords, editor::setMouseClickSelectionHonorsCamelWords))

val enableWheelFontChange = CheckboxDescriptor(
  if (SystemInfo.isMac) message("checkbox.enable.ctrl.mousewheel.changes.font.size.macos")
  else message("checkbox.enable.ctrl.mousewheel.changes.font.size"),
  PropertyBinding(editor::isWheelFontChangeEnabled, editor::setWheelFontChangeEnabled))

val enableDnD = CheckboxDescriptor(message("checkbox.enable.drag.n.drop.functionality.in.editor"),
                                   PropertyBinding(editor::isDndEnabled, editor::setDndEnabled))
val virtualSpace = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.after.end.of.line"),
                                      PropertyBinding(editor::isVirtualSpace, editor::setVirtualSpace))
val caretInsideTabs = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.inside.tabs"),
                                         PropertyBinding(editor::isCaretInsideTabs, editor::setCaretInsideTabs))
val virtualPageAtBottom = CheckboxDescriptor(message("checkbox.show.virtual.space.at.file.bottom"),
                                             PropertyBinding(editor::isAdditionalPageAtBottom, editor::setAdditionalPageAtBottom))

val highlightBraces = CheckboxDescriptor(message("checkbox.highlight.matched.brace"), codeInsightSettings::HIGHLIGHT_BRACES)
val highlightScope = CheckboxDescriptor(message("checkbox.highlight.current.scope"), codeInsightSettings::HIGHLIGHT_SCOPE)
val highlightIdentifierUnderCaret = CheckboxDescriptor("Highlight usages of element at caret",
                                                       codeInsightSettings::HIGHLIGHT_IDENTIFIER_UNDER_CARET)

val showNotificationAfterReformatCodeCheckBox = CheckboxDescriptor("Show notification after reformat code action",
                                                                   PropertyBinding(editor::isShowNotificationAfterReformat,
                                                                                   editor::setShowNotificationAfterReformat))
val myShowNotificationAfterOptimizeImportsCheckBox = CheckboxDescriptor("Show notification after optimize imports action",
                                                                        PropertyBinding(editor::isShowNotificationAfterOptimizeImports,
                                                                                        editor::setShowNotificationAfterOptimizeImports))
val renameLocalVariablesInplace = CheckboxDescriptor(message("checkbox.rename.local.variables.inplace"),
                                                     PropertyBinding(editor::isVariableInplaceRenameEnabled,
                                                                     editor::setVariableInplaceRenameEnabled))
val preselectCheckBox = CheckboxDescriptor(message("checkbox.rename.local.variables.preselect"),
                                           PropertyBinding(editor::isPreselectRename, editor::setPreselectRename))
val showInlineDialogForCheckBox = CheckboxDescriptor("Show inline dialog on local variable references",
                                                     PropertyBinding(editor::isShowInlineLocalDialog, editor::setShowInlineLocalDialog))

val optionDescriptors: List<OptionDescription> = listOf(
  honorCamelHumpsWhenSelectingByClicking
  , enableWheelFontChange
  , enableDnD
  , virtualSpace
  , caretInsideTabs
  , virtualPageAtBottom
  , highlightBraces
  , highlightScope
  , highlightIdentifierUnderCaret
  , showNotificationAfterReformatCodeCheckBox
  , myShowNotificationAfterOptimizeImportsCheckBox
  , renameLocalVariablesInplace
  , preselectCheckBox
  , showInlineDialogForCheckBox
).map(CheckboxDescriptor::asOptionDescriptor)

class EditorCheckBoxConfigurable : BoundConfigurable(""), SearchableConfigurable {
  override fun getId() = ""

  override fun createPanel(): DialogPanel {
    return panel {
      row {
        titledRow(message("group.advanced.mouse.usages")) {
          row { checkBox(honorCamelHumpsWhenSelectingByClicking) }
          row { checkBox(enableWheelFontChange) }
          row { checkBox(enableDnD) }
        }
      }
      row {
        titledRow(message("group.virtual.space")) {
          row { checkBox(virtualSpace) }
          row { checkBox(caretInsideTabs) }
          row { checkBox(virtualPageAtBottom) }
        }
      }
      row {
        titledRow(message("group.brace.highlighting")) {
          row { checkBox(highlightBraces) }
          row { checkBox(highlightScope) }
          row { checkBox(highlightIdentifierUnderCaret) }
        }
      }
      row {
        titledRow("Formatting") {
          row { checkBox(showNotificationAfterReformatCodeCheckBox) }
          row { checkBox(myShowNotificationAfterOptimizeImportsCheckBox) }
        }
      }
      row {
        titledRow("Refactorings") {
          row { checkBox(renameLocalVariablesInplace) }
          row { checkBox(preselectCheckBox) }
          row { checkBox(showInlineDialogForCheckBox) }
        }
      }
    }
  }
}