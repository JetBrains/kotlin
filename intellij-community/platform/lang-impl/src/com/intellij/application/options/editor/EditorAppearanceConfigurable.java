// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.hints.InlayParameterHintsExtension;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.codeInsight.hints.settings.ParameterNameHintsConfigurable;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * To provide additional options in Editor | Appearance section register implementation of {@link UnnamedConfigurable} in the plugin.xml:
 * <p/>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;editorAppearanceConfigurable instance="class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p>
 * A new instance of the specified class will be created each time then the Settings dialog is opened
 *
 * @author yole
 */
public class EditorAppearanceConfigurable extends CompositeConfigurable<UnnamedConfigurable> implements EditorOptionsProvider {
  private static final ExtensionPointName<EditorAppearanceConfigurableEP> EP_NAME = ExtensionPointName.create("com.intellij.editorAppearanceConfigurable");
  private JPanel myRootPanel;
  private JCheckBox myCbBlinkCaret;
  private JCheckBox myCbBlockCursor;
  private JCheckBox myCbRightMargin;
  private JCheckBox myCbShowLineNumbers;
  private JCheckBox myCbShowWhitespaces;
  private JCheckBox myLeadingWhitespacesCheckBox;
  private JCheckBox myInnerWhitespacesCheckBox;
  private JCheckBox myTrailingWhitespacesCheckBox;
  private JTextField myBlinkIntervalField;
  private JPanel myAddonPanel;
  private JCheckBox myCbShowMethodSeparators;
  //private JCheckBox myAntialiasingInEditorCheckBox;
  private JCheckBox myShowCodeLensInEditorCheckBox;
  private JCheckBox myShowVerticalIndentGuidesCheckBox;
  private JBCheckBox myCbShowIntentionBulbCheckBox;
  private JPanel myParameterHintsSettingsPanel;
  private JBCheckBox myShowParameterNameHints;
  private JButton myConfigureParameterHintsButton;
  private JBCheckBox myFocusModeCheckBox;

  //private JCheckBox myUseLCDRendering;

  public EditorAppearanceConfigurable() {
    //myAntialiasingInEditorCheckBox.addActionListener(new ActionListener() {
    //  @Override
    //  public void actionPerformed(ActionEvent e) {
    //    myUseLCDRendering.setEnabled(myAntialiasingInEditorCheckBox.isSelected());
    //  }
    //});

    myCbBlinkCaret.addActionListener((e) -> myBlinkIntervalField.setEnabled(myCbBlinkCaret.isSelected()));
    myCbShowWhitespaces.addActionListener((e) -> updateWhitespaceCheckboxesState());

    myFocusModeCheckBox.setVisible(ApplicationManager.getApplication().isInternal());

    initInlaysPanel();
  }

  private void initInlaysPanel() {
    boolean isInlayProvidersAvailable = InlayParameterHintsExtension.INSTANCE.hasAnyExtensions();
    myParameterHintsSettingsPanel.setVisible(isInlayProvidersAvailable);
    if (!isInlayProvidersAvailable) return;

    myConfigureParameterHintsButton.addActionListener(e -> {
      ParameterNameHintsConfigurable configurable = new ParameterNameHintsConfigurable();
      configurable.show();
    });
  }

  private void setParameterNameHintsSettings(EditorSettingsExternalizable settings) {
    settings.setShowParameterNameHints(myShowParameterNameHints.isSelected());
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
  }

  private void updateWhitespaceCheckboxesState() {
    boolean enabled = myCbShowWhitespaces.isSelected();
    myLeadingWhitespacesCheckBox.setEnabled(enabled);
    myInnerWhitespacesCheckBox.setEnabled(enabled);
    myTrailingWhitespacesCheckBox.setEnabled(enabled);
  }

  @Override
  public void reset() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();

    myCbShowMethodSeparators.setSelected(DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS);
    myCbBlinkCaret.setSelected(editorSettings.isBlinkCaret());
    myBlinkIntervalField.setText(Integer.toString(editorSettings.getBlinkPeriod()));
    myBlinkIntervalField.setEnabled(editorSettings.isBlinkCaret());
    myCbRightMargin.setSelected(editorSettings.isRightMarginShown());
    myCbShowLineNumbers.setSelected(editorSettings.isLineNumbersShown());
    myCbBlockCursor.setSelected(editorSettings.isBlockCursor());
    myCbShowWhitespaces.setSelected(editorSettings.isWhitespacesShown());
    myLeadingWhitespacesCheckBox.setSelected(editorSettings.isLeadingWhitespacesShown());
    myInnerWhitespacesCheckBox.setSelected(editorSettings.isInnerWhitespacesShown());
    myTrailingWhitespacesCheckBox.setSelected(editorSettings.isTrailingWhitespacesShown());
    myShowVerticalIndentGuidesCheckBox.setSelected(editorSettings.isIndentGuidesShown());
    myFocusModeCheckBox.setSelected(editorSettings.isFocusMode());
    myCbShowIntentionBulbCheckBox.setSelected(editorSettings.isShowIntentionBulb());
    //myAntialiasingInEditorCheckBox.setSelected(UISettings.getInstance().ANTIALIASING_IN_EDITOR);
    //myUseLCDRendering.setSelected(UISettings.getInstance().USE_LCD_RENDERING_IN_EDITOR);
    myShowCodeLensInEditorCheckBox.setSelected(UISettings.getInstance().getShowEditorToolTip());

    updateWhitespaceCheckboxesState();

    myShowParameterNameHints.setSelected(editorSettings.isShowParameterNameHints());

    super.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    editorSettings.setBlinkCaret(myCbBlinkCaret.isSelected());
    try {
      editorSettings.setBlinkPeriod(Integer.parseInt(myBlinkIntervalField.getText()));
    }
    catch (NumberFormatException ignore) {
    }

    editorSettings.setBlockCursor(myCbBlockCursor.isSelected());
    editorSettings.setRightMarginShown(myCbRightMargin.isSelected());
    editorSettings.setLineNumbersShown(myCbShowLineNumbers.isSelected());
    editorSettings.setWhitespacesShown(myCbShowWhitespaces.isSelected());
    editorSettings.setLeadingWhitespacesShown(myLeadingWhitespacesCheckBox.isSelected());
    editorSettings.setInnerWhitespacesShown(myInnerWhitespacesCheckBox.isSelected());
    editorSettings.setTrailingWhitespacesShown(myTrailingWhitespacesCheckBox.isSelected());
    editorSettings.setIndentGuidesShown(myShowVerticalIndentGuidesCheckBox.isSelected());
    editorSettings.setFocusMode(myFocusModeCheckBox.isSelected());
    editorSettings.setShowIntentionBulb(myCbShowIntentionBulbCheckBox.isSelected());
    setParameterNameHintsSettings(editorSettings);

    EditorOptionsPanel.reinitAllEditors();

    DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = myCbShowMethodSeparators.isSelected();

    UISettings uiSettings = UISettings.getInstance();
    boolean uiSettingsModified = false;
    boolean lafSettingsModified = false;
    //if (uiSettings.ANTIALIASING_IN_EDITOR != myAntialiasingInEditorCheckBox.isSelected()) {
    //  uiSettings.ANTIALIASING_IN_EDITOR = myAntialiasingInEditorCheckBox.isSelected();
    //  uiSettingsModified = true;
    //}

    //if (uiSettings.USE_LCD_RENDERING_IN_EDITOR != myUseLCDRendering.isSelected()) {
    //  uiSettings.USE_LCD_RENDERING_IN_EDITOR = myUseLCDRendering.isSelected();
    //  uiSettingsModified = true;
    //}

    if (uiSettings.getShowEditorToolTip() != myShowCodeLensInEditorCheckBox.isSelected()) {
      uiSettings.setShowEditorToolTip(myShowCodeLensInEditorCheckBox.isSelected());
      uiSettingsModified = true;
      lafSettingsModified = true;
    }

    if (lafSettingsModified) {
      LafManager.getInstance().repaintUI();
    }
    if (uiSettingsModified) {
      uiSettings.fireUISettingsChanged();
    }
    EditorOptionsPanel.restartDaemons();
    ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorOptionsListener.APPEARANCE_CONFIGURABLE_TOPIC).changesApplied();

    super.apply();
  }

  @Override
  public boolean isModified() {
    if (super.isModified()) return true;
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    boolean isModified = isModified(myCbBlinkCaret, editorSettings.isBlinkCaret());
    isModified |= isModified(myBlinkIntervalField, editorSettings.getBlinkPeriod(), EditorSettingsExternalizable.BLINKING_RANGE);

    isModified |= isModified(myCbBlockCursor, editorSettings.isBlockCursor());

    isModified |= isModified(myCbRightMargin, editorSettings.isRightMarginShown());

    isModified |= isModified(myCbShowLineNumbers, editorSettings.isLineNumbersShown());
    isModified |= isModified(myCbShowWhitespaces, editorSettings.isWhitespacesShown());
    isModified |= isModified(myLeadingWhitespacesCheckBox, editorSettings.isLeadingWhitespacesShown());
    isModified |= isModified(myInnerWhitespacesCheckBox, editorSettings.isInnerWhitespacesShown());
    isModified |= isModified(myTrailingWhitespacesCheckBox, editorSettings.isTrailingWhitespacesShown());
    isModified |= isModified(myShowVerticalIndentGuidesCheckBox, editorSettings.isIndentGuidesShown());
    isModified |= isModified(myFocusModeCheckBox, editorSettings.isFocusMode());
    isModified |= isModified(myCbShowIntentionBulbCheckBox, editorSettings.isShowIntentionBulb());
    isModified |= isModified(myCbShowMethodSeparators, DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS);
    //isModified |= myAntialiasingInEditorCheckBox.isSelected() != UISettings.getInstance().ANTIALIASING_IN_EDITOR;
    //isModified |= myUseLCDRendering.isSelected() != UISettings.getInstance().USE_LCD_RENDERING_IN_EDITOR;
    isModified |= myShowCodeLensInEditorCheckBox.isSelected() != UISettings.getInstance().getShowEditorToolTip();
    isModified |= myShowParameterNameHints.isSelected() != editorSettings.isShowParameterNameHints();

    return isModified;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("tab.editor.settings.appearance");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.appearance";
  }

  @Override
  public JComponent createComponent() {
    for (UnnamedConfigurable provider : getConfigurables()) {
      JComponent component = provider.createComponent();
      if (component != null) {
        myAddonPanel.add(component, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                                                           GridBagConstraints.NONE, new Insets(0, 0, 15, 0), 0, 0));
      }
    }
    return myRootPanel;
  }

  @Override
  public void disposeUIResources() {
    myAddonPanel.removeAll();
    super.disposeUIResources();
  }

  @NotNull
  @Override
  protected List<UnnamedConfigurable> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(EP_NAME);
  }

  @Override
  @NotNull
  public String getId() {
    return "editor.preferences.appearance";
  }
}
