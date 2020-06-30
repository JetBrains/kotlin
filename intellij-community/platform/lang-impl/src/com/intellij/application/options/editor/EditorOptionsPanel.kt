// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.application.options.editor.EditorCaretStopPolicyItem.*
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPass
import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.search.OptionDescription
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationBundle.message
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actions.CaretStopBoundary
import com.intellij.openapi.editor.actions.CaretStopOptionsTransposed
import com.intellij.openapi.editor.actions.CaretStopOptionsTransposed.Companion.fromCaretStopOptions
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.BoundCompositeConfigurable
import com.intellij.openapi.options.Configurable.WithEpDependencies
import com.intellij.openapi.options.SchemeManager
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.profile.codeInspection.ui.ErrorOptionsProvider
import com.intellij.profile.codeInspection.ui.ErrorOptionsProviderEP
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.*
import javax.swing.DefaultComboBoxModel

// @formatter:off
private val codeInsightSettings get() = CodeInsightSettings.getInstance()
private val editorSettings get() = EditorSettingsExternalizable.getInstance()
private val uiSettings get() = UISettings.instance
private val richCopySettings get() = RichCopySettings.getInstance()
private val codeAnalyzerSettings get() = DaemonCodeAnalyzerSettings.getInstance()

private fun String.capitalizeWords(): String = StringUtil.capitalizeWords(this, true)

private val enableWheelFontChange                                      get() = CheckboxDescriptor(if (SystemInfo.isMac) message("checkbox.enable.ctrl.mousewheel.changes.font.size.macos") else message("checkbox.enable.ctrl.mousewheel.changes.font.size"), PropertyBinding(editorSettings::isWheelFontChangeEnabled, editorSettings::setWheelFontChangeEnabled))

private val enableDnD                                                  get() = CheckboxDescriptor(message("checkbox.enable.drag.n.drop.functionality.in.editor"), PropertyBinding(editorSettings::isDndEnabled, editorSettings::setDndEnabled))
private val virtualSpace                                               get() = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.after.end.of.line"), PropertyBinding(editorSettings::isVirtualSpace, editorSettings::setVirtualSpace), groupName = message("checkbox.allow.placement.of.caret.label").capitalizeWords())
private val caretInsideTabs                                            get() = CheckboxDescriptor(message("checkbox.allow.placement.of.caret.inside.tabs"), PropertyBinding(editorSettings::isCaretInsideTabs, editorSettings::setCaretInsideTabs), groupName = message("checkbox.allow.placement.of.caret.label").capitalizeWords())
private val virtualPageAtBottom                                        get() = CheckboxDescriptor(message("checkbox.show.virtual.space.at.file.bottom"), PropertyBinding(editorSettings::isAdditionalPageAtBottom, editorSettings::setAdditionalPageAtBottom))

private val highlightBraces                                            get() = CheckboxDescriptor(message("checkbox.highlight.matched.brace"), codeInsightSettings::HIGHLIGHT_BRACES, groupName = message("group.brace.highlighting"))
private val highlightScope                                             get() = CheckboxDescriptor(message("checkbox.highlight.current.scope"), codeInsightSettings::HIGHLIGHT_SCOPE, groupName = message("group.brace.highlighting"))
private val highlightIdentifierUnderCaret                              get() = CheckboxDescriptor(message("checkbox.highlight.usages.of.element.at.caret"), codeInsightSettings::HIGHLIGHT_IDENTIFIER_UNDER_CARET, groupName = message("group.brace.highlighting"))

private val renameLocalVariablesInplace                                get() = CheckboxDescriptor(message("radiobutton.rename.local.variables.inplace"), PropertyBinding(editorSettings::isVariableInplaceRenameEnabled, editorSettings::setVariableInplaceRenameEnabled), groupName = message("radiogroup.rename.local.variables").capitalizeWords())
private val preselectCheckBox                                          get() = CheckboxDescriptor(message("checkbox.rename.local.variables.preselect"), PropertyBinding(editorSettings::isPreselectRename, editorSettings::setPreselectRename), groupName = message("group.refactorings"))
private val showInlineDialogForCheckBox                                get() = CheckboxDescriptor(message("checkbox.show.inline.dialog.on.local.variable.references"), PropertyBinding(editorSettings::isShowInlineLocalDialog, editorSettings::setShowInlineLocalDialog))

private val cdSmoothScrolling                                          get() = CheckboxDescriptor(message("checkbox.smooth.scrolling"), PropertyBinding(editorSettings::isSmoothScrolling, editorSettings::setSmoothScrolling))
private val cdUseSoftWrapsAtEditor                                     get() = CheckboxDescriptor(message("checkbox.use.soft.wraps.at.editor"), PropertyBinding(editorSettings::isUseSoftWraps, editorSettings::setUseSoftWraps))
private val cdUseCustomSoftWrapIndent                                  get() = CheckboxDescriptor(message("checkbox.use.custom.soft.wraps.indent"), PropertyBinding(editorSettings::isUseCustomSoftWrapIndent, editorSettings::setUseCustomSoftWrapIndent))
private val cdShowSoftWrapsOnlyOnCaretLine                             get() = CheckboxDescriptor(message("checkbox.show.softwraps.only.for.caret.line"), PropertyBinding({ !editorSettings.isAllSoftWrapsShown }, { editorSettings.setAllSoftwrapsShown(!it) }))
private val cdEnsureBlankLineBeforeCheckBox                            get() = CheckboxDescriptor(message("editor.options.line.feed"), PropertyBinding(editorSettings::isEnsureNewLineAtEOF, editorSettings::setEnsureNewLineAtEOF))
private val cdShowQuickDocOnMouseMove                                  get() = CheckboxDescriptor(message("editor.options.quick.doc.on.mouse.hover"), PropertyBinding(editorSettings::isShowQuickDocOnMouseOverElement, editorSettings::setShowQuickDocOnMouseOverElement))
private val cdKeepTrailingSpacesOnCaretLine                            get() = CheckboxDescriptor(message("editor.settings.delete.trailing.spaces.on.caret.line"), PropertyBinding({ !editorSettings.isKeepTrailingSpacesOnCaretLine }, { editorSettings.isKeepTrailingSpacesOnCaretLine = !it }))

// @formatter:on

internal val optionDescriptors: List<OptionDescription>
  get() = listOf(
    myCbHonorCamelHumpsWhenSelectingByClicking,
    enableWheelFontChange,
    enableDnD,
    virtualSpace,
    caretInsideTabs,
    virtualPageAtBottom,
    highlightBraces,
    highlightScope,
    highlightIdentifierUnderCaret,
    renameLocalVariablesInplace,
    preselectCheckBox,
    showInlineDialogForCheckBox
  ).map(CheckboxDescriptor::asUiOptionDescriptor)


class EditorOptionsPanel : BoundCompositeConfigurable<UnnamedConfigurable>(message("title.editor"), ID), WithEpDependencies {
  companion object {
    const val ID = "preferences.editor"

    private fun clearAllIdentifierHighlighters() {
      for (project in ProjectManager.getInstance().openProjects) {
        for (fileEditor in FileEditorManager.getInstance(project).allEditors) {
          if (fileEditor is TextEditor) {
            val document = fileEditor.editor.document
            IdentifierHighlighterPass.clearMyHighlights(document, project)
          }
        }
      }
    }

    @JvmStatic
    fun reinitAllEditors() {
      EditorFactory.getInstance().refreshAllEditors()
    }

    @JvmStatic
    fun restartDaemons() {
      val projects = ProjectManager.getInstance().openProjects
      for (project in projects) {
        DaemonCodeAnalyzer.getInstance(project).settingsChanged()
      }
    }
  }

  override fun createConfigurables() = ConfigurableWrapper.createConfigurables(GeneralEditorOptionsProviderEP.EP_NAME)
  override fun getDependencies() = setOf(GeneralEditorOptionsProviderEP.EP_NAME)

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(message("group.advanced.mouse.usages")) {
        row { checkBox(enableWheelFontChange) }
        row {
          cell(isFullWidth = true) {
            checkBox(enableDnD)
            commentNoWrap(message("checkbox.enable.drag.n.drop.functionality.in.editor.comment")).withLargeLeftGap()
          }
        }
      }
      titledRow(message("group.soft.wraps")) {
        row {
          val useSoftWraps = checkBox(cdUseSoftWrapsAtEditor)
          textField({ editorSettings.softWrapFileMasks }, { editorSettings.softWrapFileMasks = it })
            .growPolicy(GrowPolicy.MEDIUM_TEXT)
            .applyToComponent { emptyText.text = message("soft.wraps.file.masks.empty.text") }
            .comment(message("soft.wraps.file.masks.hint"), forComponent = true)
            .enableIf(useSoftWraps.selected)
        }
        row {
          val useSoftWrapsIndent = checkBox(cdUseCustomSoftWrapIndent)
          row {
            cell(isFullWidth = true) {
              label(message("label.use.custom.soft.wraps.indent"))
                .enableIf(useSoftWrapsIndent.selected)
              intTextField(editorSettings::getCustomSoftWrapIndent, editorSettings::setCustomSoftWrapIndent, columns = 2)
                .enableIf(useSoftWrapsIndent.selected)
              label(message("label.use.custom.soft.wraps.indent.symbols.suffix"))
            }
          }
        }
        row { checkBox(cdShowSoftWrapsOnlyOnCaretLine) }
      }
      titledRow(message("group.virtual.space")) {
        row {
          cell(isFullWidth = true) {
            label(message("checkbox.allow.placement.of.caret.label"))
            checkBox(virtualSpace).withLargeLeftGap()
            checkBox(caretInsideTabs).withLargeLeftGap()
          }
        }
        row { checkBox(virtualPageAtBottom) }
      }
      titledRow(message("group.caret.movement")) {
        row(message("label.word.move.caret.actions.behavior")) {
          caretStopComboBox(CaretOptionMode.WORD, WordBoundary.values())
        }
        row(message("label.word.move.caret.actions.behavior.at.line.break")) {
          caretStopComboBox(CaretOptionMode.LINE, LineBoundary.values())
        }
      }
      titledRow(message("editor.options.scrolling")) {
        row { checkBox(cdSmoothScrolling) }
        row {
          buttonGroup(editorSettings::isRefrainFromScrolling,
                      editorSettings::setRefrainFromScrolling) {
            checkBoxGroup(message("editor.options.prefer.scrolling.editor.label")) {
              row { radioButton(message("editor.options.prefer.scrolling.editor.canvas.to.keep.caret.line.centered"), value = false) }
              row { radioButton(message("editor.options.prefer.moving.caret.line.to.minimize.editor.scrolling"), value = true) }
            }
          }
        }
      }
      titledRow(message("group.limits")) {
        row(message("editbox.recent.files.limit")) {
          intTextField(uiSettings::recentFilesLimit, range = 1..500, columns = 4)
        }
        row(message("editbox.recent.locations.limit")) {
          intTextField(uiSettings::recentLocationsLimit, range = 1..100, columns = 4)
        }
      }
      titledRow(message("group.richcopy")) {
        row {
          val copyShortcut = ActionManager.getInstance().getKeyboardShortcut(IdeActions.ACTION_COPY)
          val copyShortcutText = copyShortcut?.let { " (" + KeymapUtil.getShortcutText(it) + ")" } ?: ""
          cell(isFullWidth = true) {
            checkBox(CheckboxDescriptor(message("checkbox.enable.richcopy.label", copyShortcutText),
                                        PropertyBinding(richCopySettings::isEnabled, richCopySettings::setEnabled)))
            commentNoWrap(message("checkbox.enable.richcopy.comment")).withLargeLeftGap()
          }
        }
        row {
          cell(isFullWidth = true) {
            label(message("combobox.richcopy.color.scheme"))
            val schemes = listOf(RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER) +
                          EditorColorsManager.getInstance().allSchemes.map { SchemeManager.getBaseName(it) }
            comboBox<String>(
              DefaultComboBoxModel(schemes.toTypedArray()), richCopySettings::getSchemeName, richCopySettings::setSchemeName,
              renderer = SimpleListCellRenderer.create("") {
                when (it) {
                  RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER ->
                    message("combobox.richcopy.color.scheme.active")
                  EditorColorsScheme.DEFAULT_SCHEME_NAME -> EditorColorsScheme.DEFAULT_SCHEME_ALIAS
                  else -> it
                }
              }
            )
          }
        }
      }
      titledRow(message("editor.options.save.files.group")) {
        row {
          var stripTrailing: ComboBox<*>? = null
          cell(isFullWidth = true) {
            label(message("combobox.strip.trailing.spaces.on.save"))
            val model = DefaultComboBoxModel(
              arrayOf(
                EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED,
                EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE,
                EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE
              )
            )
            stripTrailing = comboBox(
              model, editorSettings::getStripTrailingSpaces, editorSettings::setStripTrailingSpaces,
              renderer = SimpleListCellRenderer.create("") {
                when (it) {
                  EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED -> message("combobox.strip.modified.lines")
                  EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE -> message("combobox.strip.all")
                  EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE -> message("combobox.strip.none")
                  else -> it
                }
              }
            ).component
          }
          row {
            checkBox(cdKeepTrailingSpacesOnCaretLine)
              .enableIf(stripTrailing!!.selectedValueMatches { it != EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE })
            largeGapAfter()
          }
        }
        row { checkBox(cdEnsureBlankLineBeforeCheckBox) }
      }
      for (configurable in configurables) {
        appendDslConfigurableRow(configurable)
      }
    }
  }

  override fun apply() {
    val wasModified = isModified

    super.apply()

    if (wasModified) {
      clearAllIdentifierHighlighters()
      reinitAllEditors()
      uiSettings.fireUISettingsChanged()
      restartDaemons()
      ApplicationManager.getApplication().messageBus.syncPublisher(EditorOptionsListener.OPTIONS_PANEL_TOPIC).changesApplied()
    }
  }
}

class EditorCodeEditingConfigurable : BoundCompositeConfigurable<ErrorOptionsProvider>(message("title.code.editing"), ID), WithEpDependencies {
  companion object {
    const val ID = "preferences.editor.code.editing"
  }

  override fun createConfigurables() = ConfigurableWrapper.createConfigurables(ErrorOptionsProviderEP.EP_NAME)
  override fun getDependencies() = setOf(ErrorOptionsProviderEP.EP_NAME)

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(message("group.brace.highlighting")) {
        row { checkBox(highlightBraces) }
        row { checkBox(highlightScope) }
        row { checkBox(highlightIdentifierUnderCaret) }
      }
      titledRow(message("group.refactorings")) {
        row {
          buttonGroup(editorSettings::isVariableInplaceRenameEnabled,
                      editorSettings::setVariableInplaceRenameEnabled) {
            checkBoxGroup(message("radiogroup.rename.local.variables")) {
              row { radioButton(message("radiobutton.rename.local.variables.inplace"), value = true) }
              row { radioButton(message("radiobutton.rename.local.variables.in.dialog"), value = false) }.largeGapAfter()
            }
          }
        }
        row { checkBox(preselectCheckBox) }
        row { checkBox(showInlineDialogForCheckBox) }
      }
      titledRow(message("group.error.highlighting")) {
        row {
          cell(isFullWidth = true) {
            label(message("editbox.error.stripe.mark.min.height"))
            intTextField(codeAnalyzerSettings::getErrorStripeMarkMinHeight, codeAnalyzerSettings::setErrorStripeMarkMinHeight, columns = 4)
            label(message("editbox.error.stripe.mark.min.height.pixels.suffix"))
          }
        }
        row {
          cell(isFullWidth = true) {
            label(message("editbox.autoreparse.delay"))
            intTextField(codeAnalyzerSettings::getAutoReparseDelay, codeAnalyzerSettings::setAutoReparseDelay, columns = 4)
            label(message("editbox.autoreparse.delay.ms.suffix"))
          }
        }
        row {
          cell(isFullWidth = true) {
            label(message("combobox.next.error.action.goes.to.label"))
            comboBox(
              DefaultComboBoxModel(arrayOf(true, false)),
              codeAnalyzerSettings::isNextErrorActionGoesToErrorsFirst,
              { codeAnalyzerSettings.isNextErrorActionGoesToErrorsFirst = it ?: true },
              renderer = SimpleListCellRenderer.create("") {
                when (it) {
                  true -> message("combobox.next.error.action.goes.to.errors")
                  false -> message("combobox.next.error.action.goes.to.all.problems")
                  else -> it.toString()
                }
              }
            )
          }
        }

        for (configurable in configurables) {
          appendDslConfigurableRow(configurable)
        }
      }
      titledRow(message("group.quick.documentation")) {
        row { checkBox(cdShowQuickDocOnMouseMove) }
      }
      titledRow(message("group.editor.tooltips")) {
        row {
          cell(isFullWidth = true) {
            label(message("editor.options.tooltip.delay"))
            intTextField(editorSettings::getTooltipsDelay, editorSettings::setTooltipsDelay, range = 1..5000, columns = 4)
            label(message("editor.options.ms"))
          }
        }
      }
    }
  }
}

private fun <E : EditorCaretStopPolicyItem> Cell.caretStopComboBox(mode: CaretOptionMode, values: Array<E>): CellBuilder<ComboBox<E?>> {
  val model: DefaultComboBoxModel<E?> = SeparatorAwareComboBoxModel()
  var lastWasOsDefault = false
  for (item in values) {
    val isOsDefault = item.osDefault !== OsDefault.NONE
    if (lastWasOsDefault && !isOsDefault) model.addElement(null)
    lastWasOsDefault = isOsDefault
    val insertionIndex = if (item.osDefault.isIdeDefault) 0 else model.size
    model.insertElementAt(item, insertionIndex)
  }

  return component(ComboBox(model))
    .applyToComponent { renderer = SeparatorAwareListItemRenderer() }
    .sizeGroup("caretStopComboBox")
    .withBinding(
      {
        val item = it.selectedItem as? EditorCaretStopPolicyItem
        item?.caretStopBoundary ?: mode.get(CaretStopOptionsTransposed.DEFAULT)
      },
      { it, value -> it.selectedItem = mode.find(value) },
      PropertyBinding(
        {
          val value = fromCaretStopOptions(editorSettings.caretStopOptions)
          mode.get(value)
        },
        {
          val value = fromCaretStopOptions(editorSettings.caretStopOptions)
          editorSettings.caretStopOptions = mode.update(value, it).toCaretStopOptions()
        }
      )
    )
}

private enum class CaretOptionMode {
  WORD {
    override fun find(boundary: CaretStopBoundary): WordBoundary = WordBoundary.itemForBoundary(boundary)
    override fun get(option: CaretStopOptionsTransposed): CaretStopBoundary = option.wordBoundary
    override fun update(option: CaretStopOptionsTransposed, value: CaretStopBoundary): CaretStopOptionsTransposed =
      CaretStopOptionsTransposed(value, option.lineBoundary)
  },
  LINE {
    override fun find(boundary: CaretStopBoundary): LineBoundary = LineBoundary.itemForBoundary(boundary)
    override fun get(option: CaretStopOptionsTransposed): CaretStopBoundary = option.lineBoundary
    override fun update(option: CaretStopOptionsTransposed, value: CaretStopBoundary): CaretStopOptionsTransposed =
      CaretStopOptionsTransposed(option.wordBoundary, value)
  };

  abstract fun find(boundary: CaretStopBoundary): EditorCaretStopPolicyItem
  abstract fun get(option: CaretStopOptionsTransposed): CaretStopBoundary
  abstract fun update(option: CaretStopOptionsTransposed, value: CaretStopBoundary): CaretStopOptionsTransposed
}