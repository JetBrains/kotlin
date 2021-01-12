// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.codeInsight.CodeInsightSettings.*
import com.intellij.codeInsight.editorActions.SmartBackspaceMode
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler
import com.intellij.openapi.options.*
import com.intellij.openapi.options.ex.ConfigurableWrapper
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.layout.*
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.DefaultComboBoxModel

// @formatter:off
private val editorSettings = EditorSettingsExternalizable.getInstance()
private val codeInsightSettings = getInstance()

private val myCbSmartHome                                      get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.smart.home"), PropertyBinding(editorSettings::isSmartHome, editorSettings::setSmartHome))
private val myCbSmartEnd                                       get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.smart.end.on.blank.line"), codeInsightSettings::SMART_END_ACTION.toBinding())
private val myCbInsertPairBracket                              get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.insert.pair.bracket"), codeInsightSettings::AUTOINSERT_PAIR_BRACKET.toBinding())
private val myCbInsertPairQuote                                get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.insert.pair.quote"), codeInsightSettings::AUTOINSERT_PAIR_QUOTE.toBinding())
private val myCbReformatBlockOnTypingRBrace                    get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.reformat.on.typing.rbrace"), codeInsightSettings::REFORMAT_BLOCK_ON_RBRACE.toBinding())
private val myCbCamelWords                                     get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.use.camelhumps.words"), PropertyBinding(editorSettings::isCamelWords, editorSettings::setCamelWords))
private val myCbSurroundSelectionOnTyping                      get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.surround.selection.on.typing.quote.or.brace"), codeInsightSettings::SURROUND_SELECTION_ON_QUOTE_TYPED.toBinding())
private val myCbTabExistsBracketsAndQuotes                     get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.tab.exists.brackets.and.quotes"), codeInsightSettings::TAB_EXITS_BRACKETS_AND_QUOTES.toBinding())
private val myCbEnableAddingCaretsOnDoubleCtrlArrows           get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.enable.double.ctrl", KeyEvent.getKeyText(ModifierKeyDoubleClickHandler.getMultiCaretActionModifier())), PropertyBinding(editorSettings::addCaretsOnDoubleCtrl, editorSettings::setAddCaretsOnDoubleCtrl))
private val myCbSmartIndentOnEnter                             get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.smart.indent"), codeInsightSettings::SMART_INDENT_ON_ENTER.toBinding())
private val myCbInsertPairCurlyBraceOnEnter                    get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.insert.pair.curly.brace"), codeInsightSettings::INSERT_BRACE_ON_ENTER.toBinding())
private val myCbInsertJavadocStubOnEnter                       get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.javadoc.stub.after.slash.star.star"), codeInsightSettings::JAVADOC_STUB_ON_ENTER.toBinding())
internal val myCbHonorCamelHumpsWhenSelectingByClicking        get() = CheckboxDescriptor(ApplicationBundle.message("checkbox.honor.camelhumps.words.settings.on.double.click"), PropertyBinding(editorSettings::isMouseClickSelectionHonorsCamelWords, editorSettings::setMouseClickSelectionHonorsCamelWords))
// @formatter:on

internal val editorSmartKeysOptionDescriptors
  get() = listOf(
    myCbSmartHome,
    myCbSmartEnd,
    myCbInsertPairBracket,
    myCbInsertPairQuote,
    myCbReformatBlockOnTypingRBrace,
    myCbCamelWords,
    myCbSurroundSelectionOnTyping,
    myCbTabExistsBracketsAndQuotes,
    myCbEnableAddingCaretsOnDoubleCtrlArrows,
    myCbSmartIndentOnEnter,
    myCbInsertPairCurlyBraceOnEnter,
    myCbInsertJavadocStubOnEnter,
    myCbHonorCamelHumpsWhenSelectingByClicking
  ).map(CheckboxDescriptor::asUiOptionDescriptor)

@NonNls
const val ID = "editor.preferences.smartKeys"

/**
 * To provide additional options in Editor | Smart Keys section register implementation of {@link com.intellij.openapi.options.UnnamedConfigurable} in the plugin.xml:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorSmartKeysConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author yole
 */
class EditorSmartKeysConfigurable : Configurable.WithEpDependencies, BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
  ApplicationBundle.message("group.smart.keys"),
  "reference.settingsdialog.IDE.editor.smartkey",
  ID
), SearchableConfigurable.Parent {
  override fun createPanel(): DialogPanel {
    return panel {
      row {
        checkBox(myCbSmartHome)
      }
      row {
        checkBox(myCbSmartEnd)
      }
      row {
        checkBox(myCbInsertPairBracket)
      }
      row {
        checkBox(myCbInsertPairQuote)
      }
      row {
        checkBox(myCbReformatBlockOnTypingRBrace)
      }
      row {
        checkBox(myCbCamelWords)
      }
      row {
        checkBox(myCbHonorCamelHumpsWhenSelectingByClicking)
      }
      row {
        checkBox(myCbSurroundSelectionOnTyping)
      }
      row {
        checkBox(myCbEnableAddingCaretsOnDoubleCtrlArrows)
      }
      row {
        checkBox(myCbTabExistsBracketsAndQuotes)
      }
      titledRow(ApplicationBundle.message("group.enter.title")) {
        row {
          checkBox(myCbSmartIndentOnEnter)
        }
        row {
          checkBox(myCbInsertPairCurlyBraceOnEnter)
        }
        if (hasAnyDocAwareCommenters()) {
          row {
            checkBox(myCbInsertJavadocStubOnEnter)
          }
        }
      }
      row(ApplicationBundle.message("combobox.smart.backspace")) {
        comboBox(
          EnumComboBoxModel(SmartBackspaceMode::class.java),
          PropertyBinding(codeInsightSettings::getBackspaceMode, codeInsightSettings::setBackspaceMode).toNullable(),
          renderer = listCellRenderer { value, _, _ ->
            setText(when(value) {
              SmartBackspaceMode.OFF -> ApplicationBundle.message("combobox.smart.backspace.off")
              SmartBackspaceMode.INDENT -> ApplicationBundle.message("combobox.smart.backspace.simple")
              SmartBackspaceMode.AUTOINDENT -> ApplicationBundle.message("combobox.smart.backspace.smart")
              else -> ""
            })
          })
      }
      row(ApplicationBundle.message("combobox.paste.reformat")) {
        comboBox(
          DefaultComboBoxModel(arrayOf(NO_REFORMAT, INDENT_BLOCK, INDENT_EACH_LINE, REFORMAT_BLOCK)),
          codeInsightSettings::REFORMAT_ON_PASTE,
          renderer = listCellRenderer { value, _, _ ->
            setText(when(value) {
              NO_REFORMAT -> ApplicationBundle.message("combobox.paste.reformat.none")
              INDENT_BLOCK -> ApplicationBundle.message("combobox.paste.reformat.indent.block")
              INDENT_EACH_LINE -> ApplicationBundle.message("combobox.paste.reformat.indent.each.line")
              REFORMAT_BLOCK -> ApplicationBundle.message("combobox.paste.reformat.reformat.block")
              else -> ""
            })
          }
        )
      }
      for (configurable in configurables) {
        appendDslConfigurableRow(configurable)
      }
    }
  }

  private fun hasAnyDocAwareCommenters(): Boolean {
    return Language.getRegisteredLanguages().any {
      val commenter = LanguageCommenters.INSTANCE.forLanguage(it)
      commenter is CodeDocumentationAwareCommenter && commenter.documentationCommentLinePrefix != null
    }
  }

  override fun apply() {
    super.apply()
    ApplicationManager.getApplication().messageBus.syncPublisher(EditorOptionsListener.SMART_KEYS_CONFIGURABLE_TOPIC).changesApplied()
  }

  private val allConfigurables: List<UnnamedConfigurable> by lazy { ConfigurableWrapper.createConfigurables(EP_NAME) }

  override fun createConfigurables(): List<UnnamedConfigurable> {
    return allConfigurables.filterNot { it is Configurable }
  }

  override fun getConfigurables(): Array<Configurable> {
    return allConfigurables.filterIsInstance<Configurable>().toTypedArray()
  }

  override fun hasOwnContent() = true

  override fun getDependencies() = listOf(EP_NAME)
  
  companion object {
    private val EP_NAME = ExtensionPointName.create<EditorSmartKeysConfigurableEP>("com.intellij.editorSmartKeysConfigurable")
  }
}
