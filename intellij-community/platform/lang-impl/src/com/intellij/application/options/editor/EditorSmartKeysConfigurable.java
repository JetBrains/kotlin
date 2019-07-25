// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.SmartBackspaceMode;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;

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
public class EditorSmartKeysConfigurable extends CompositeConfigurable<UnnamedConfigurable> implements EditorOptionsProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.application.options.editor.EditorSmartKeysConfigurable");
  private static final ExtensionPointName<EditorSmartKeysConfigurableEP> EP_NAME = ExtensionPointName.create("com.intellij.editorSmartKeysConfigurable");

  private JCheckBox myCbSmartHome;
  private JCheckBox myCbSmartEnd;
  private JCheckBox myCbInsertPairBracket;
  private JCheckBox myCbInsertPairQuote;
  private JCheckBox myCbCamelWords;
  private JCheckBox myCbSmartIndentOnEnter;
  private JComboBox myReformatOnPasteCombo;
  private JPanel myRootPanel;
  private JPanel myAddonPanel;
  private JCheckBox myCbInsertPairCurlyBraceOnEnter;
  private JCheckBox myCbInsertJavadocStubOnEnter;
  private JCheckBox myCbSurroundSelectionOnTyping;
  private JCheckBox myCbReformatBlockOnTypingRBrace;
  private JComboBox mySmartBackspaceCombo;
  private JCheckBox myCbEnableAddingCaretsOnDoubleCtrlArrows;
  private JCheckBox myCbTabExistsBracketsAndQuotes;
  private boolean myAddonsInitialized = false;

  private static final String NO_REFORMAT = ApplicationBundle.message("combobox.paste.reformat.none");
  private static final String INDENT_BLOCK = ApplicationBundle.message("combobox.paste.reformat.indent.block");
  private static final String INDENT_EACH_LINE = ApplicationBundle.message("combobox.paste.reformat.indent.each.line");
  private static final String REFORMAT_BLOCK = ApplicationBundle.message("combobox.paste.reformat.reformat.block");

  private static final String OFF = ApplicationBundle.message("combobox.smart.backspace.off");
  private static final String SIMPLE = ApplicationBundle.message("combobox.smart.backspace.simple");
  private static final String SMART = ApplicationBundle.message("combobox.smart.backspace.smart");

  public EditorSmartKeysConfigurable() {
    myReformatOnPasteCombo.addItem(NO_REFORMAT);
    myReformatOnPasteCombo.addItem(INDENT_BLOCK);
    myReformatOnPasteCombo.addItem(INDENT_EACH_LINE);
    myReformatOnPasteCombo.addItem(REFORMAT_BLOCK);

    mySmartBackspaceCombo.addItem(OFF);
    mySmartBackspaceCombo.addItem(SIMPLE);
    mySmartBackspaceCombo.addItem(SMART);

    myCbInsertJavadocStubOnEnter.setVisible(hasAnyDocAwareCommenters());
    
    myCbEnableAddingCaretsOnDoubleCtrlArrows.setText(
      ApplicationBundle.message("checkbox.enable.double.ctrl",
                                KeyEvent.getKeyText(ModifierKeyDoubleClickHandler.getMultiCaretActionModifier())));
  }

  private static boolean hasAnyDocAwareCommenters() {
    final Collection<Language> languages = Language.getRegisteredLanguages();
    for (Language language : languages) {
      final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
      if (commenter instanceof CodeDocumentationAwareCommenter) {
        final CodeDocumentationAwareCommenter docCommenter = (CodeDocumentationAwareCommenter)commenter;
        if (docCommenter.getDocumentationCommentLinePrefix() != null) {
          return true;
        }
      }
    }
    return false;
  }

  @NotNull
  @Override
  protected List<UnnamedConfigurable> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(EP_NAME);
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Smart Keys";
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.smartkey";
  }

  @Override
  public JComponent createComponent() {
    if (!myAddonsInitialized) {
      myAddonsInitialized = true;
      for (UnnamedConfigurable provider : getConfigurables()) {
        myAddonPanel
          .add(provider.createComponent(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                                  GridBagConstraints.NONE, new Insets(0, 0, 15, 0), 0, 0));
      }
    }
    return myRootPanel;
  }

  @Override
  public void reset() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    // Paste
    switch(codeInsightSettings.REFORMAT_ON_PASTE){
      case CodeInsightSettings.NO_REFORMAT:
        myReformatOnPasteCombo.setSelectedItem(NO_REFORMAT);
      break;

      case CodeInsightSettings.INDENT_BLOCK:
        myReformatOnPasteCombo.setSelectedItem(INDENT_BLOCK);
      break;

      case CodeInsightSettings.INDENT_EACH_LINE:
        myReformatOnPasteCombo.setSelectedItem(INDENT_EACH_LINE);
      break;

      case CodeInsightSettings.REFORMAT_BLOCK:
        myReformatOnPasteCombo.setSelectedItem(REFORMAT_BLOCK);
      break;
    }

    myCbSmartHome.setSelected(editorSettings.isSmartHome());
    myCbSmartEnd.setSelected(codeInsightSettings.SMART_END_ACTION);

    myCbSmartIndentOnEnter.setSelected(codeInsightSettings.SMART_INDENT_ON_ENTER);
    myCbInsertPairCurlyBraceOnEnter.setSelected(codeInsightSettings.INSERT_BRACE_ON_ENTER);
    myCbInsertJavadocStubOnEnter.setSelected(codeInsightSettings.JAVADOC_STUB_ON_ENTER);

    myCbInsertPairBracket.setSelected(codeInsightSettings.AUTOINSERT_PAIR_BRACKET);
    myCbInsertPairQuote.setSelected(codeInsightSettings.AUTOINSERT_PAIR_QUOTE);
    myCbReformatBlockOnTypingRBrace.setSelected(codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE);
    myCbCamelWords.setSelected(editorSettings.isCamelWords());

    myCbSurroundSelectionOnTyping.setSelected(codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED);
    myCbTabExistsBracketsAndQuotes.setSelected(codeInsightSettings.TAB_EXITS_BRACKETS_AND_QUOTES);
    myCbEnableAddingCaretsOnDoubleCtrlArrows.setSelected(editorSettings.addCaretsOnDoubleCtrl());

    SmartBackspaceMode backspaceMode = codeInsightSettings.getBackspaceMode();
    switch (backspaceMode) {
      case OFF:
        mySmartBackspaceCombo.setSelectedItem(OFF);
        break;
      case INDENT:
        mySmartBackspaceCombo.setSelectedItem(SIMPLE);
        break;
      case AUTOINDENT:
        mySmartBackspaceCombo.setSelectedItem(SMART);
        break;
      default:
        LOG.error("Unexpected smart backspace mode value: " + backspaceMode);
    }

    super.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    editorSettings.setSmartHome(myCbSmartHome.isSelected());
    codeInsightSettings.SMART_END_ACTION = myCbSmartEnd.isSelected();
    codeInsightSettings.SMART_INDENT_ON_ENTER = myCbSmartIndentOnEnter.isSelected();
    codeInsightSettings.INSERT_BRACE_ON_ENTER = myCbInsertPairCurlyBraceOnEnter.isSelected();
    codeInsightSettings.JAVADOC_STUB_ON_ENTER = myCbInsertJavadocStubOnEnter.isSelected();
    codeInsightSettings.AUTOINSERT_PAIR_BRACKET = myCbInsertPairBracket.isSelected();
    codeInsightSettings.AUTOINSERT_PAIR_QUOTE = myCbInsertPairQuote.isSelected();
    codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE = myCbReformatBlockOnTypingRBrace.isSelected();
    codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED = myCbSurroundSelectionOnTyping.isSelected();
    codeInsightSettings.TAB_EXITS_BRACKETS_AND_QUOTES = myCbTabExistsBracketsAndQuotes.isSelected();
    editorSettings.setCamelWords(myCbCamelWords.isSelected());
    codeInsightSettings.REFORMAT_ON_PASTE = getReformatPastedBlockValue();
    codeInsightSettings.setBackspaceMode(getSmartBackspaceModeValue());
    editorSettings.setAddCaretsOnDoubleCtrl(myCbEnableAddingCaretsOnDoubleCtrlArrows.isSelected());
    
    super.apply();
    ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorOptionsListener.SMART_KEYS_CONFIGURABLE_TOPIC).changesApplied();
  }

  @Override
  public boolean isModified() {
    if (super.isModified()) return true;
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();

    boolean isModified = getReformatPastedBlockValue() != codeInsightSettings.REFORMAT_ON_PASTE;
    isModified |= isModified(myCbSmartHome, editorSettings.isSmartHome());
    isModified |= isModified(myCbSmartEnd, codeInsightSettings.SMART_END_ACTION);

    isModified |= isModified(myCbSmartIndentOnEnter, codeInsightSettings.SMART_INDENT_ON_ENTER);
    isModified |= isModified(myCbInsertPairCurlyBraceOnEnter, codeInsightSettings.INSERT_BRACE_ON_ENTER);
    isModified |= isModified(myCbInsertJavadocStubOnEnter, codeInsightSettings.JAVADOC_STUB_ON_ENTER);

    isModified |= isModified(myCbInsertPairBracket, codeInsightSettings.AUTOINSERT_PAIR_BRACKET);
    isModified |= isModified(myCbInsertPairQuote, codeInsightSettings.AUTOINSERT_PAIR_QUOTE);
    isModified |= isModified(myCbReformatBlockOnTypingRBrace, codeInsightSettings.REFORMAT_BLOCK_ON_RBRACE);
    isModified |= isModified(myCbCamelWords, editorSettings.isCamelWords());

    isModified |= isModified(myCbSurroundSelectionOnTyping, codeInsightSettings.SURROUND_SELECTION_ON_QUOTE_TYPED);
    
    isModified |= isModified(myCbEnableAddingCaretsOnDoubleCtrlArrows, editorSettings.addCaretsOnDoubleCtrl());

    isModified |= (getSmartBackspaceModeValue() != codeInsightSettings.getBackspaceMode());

    isModified |= isModified(myCbTabExistsBracketsAndQuotes, codeInsightSettings.TAB_EXITS_BRACKETS_AND_QUOTES);

    return isModified;

  }

  @MagicConstant(intValues = {CodeInsightSettings.NO_REFORMAT, CodeInsightSettings.INDENT_BLOCK, CodeInsightSettings.INDENT_EACH_LINE, CodeInsightSettings.REFORMAT_BLOCK})
  private int getReformatPastedBlockValue(){
    Object selectedItem = myReformatOnPasteCombo.getSelectedItem();
    if (NO_REFORMAT.equals(selectedItem)){
      return CodeInsightSettings.NO_REFORMAT;
    }
    else if (INDENT_BLOCK.equals(selectedItem)){
      return CodeInsightSettings.INDENT_BLOCK;
    }
    else if (INDENT_EACH_LINE.equals(selectedItem)){
      return CodeInsightSettings.INDENT_EACH_LINE;
    }
    else if (REFORMAT_BLOCK.equals(selectedItem)){
      return CodeInsightSettings.REFORMAT_BLOCK;
    }
    else{
      LOG.assertTrue(false);
      return -1;
    }
  }
  
  private SmartBackspaceMode getSmartBackspaceModeValue() {
    Object selectedItem = mySmartBackspaceCombo.getSelectedItem();
    if (OFF.equals(selectedItem)){
      return SmartBackspaceMode.OFF;
    }
    else if (SIMPLE.equals(selectedItem)){
      return SmartBackspaceMode.INDENT;
    }
    else if (SMART.equals(selectedItem)){
      return SmartBackspaceMode.AUTOINDENT;
    }
    else{
      LOG.error("Unexpected smart backspace item value: " + selectedItem);
      return SmartBackspaceMode.OFF;
    }
  }

  @Override
  @NotNull
  public String getId() {
    return "editor.preferences.smartKeys";
  }
}
