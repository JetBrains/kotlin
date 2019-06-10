// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.application.options.editor;

import com.intellij.application.options.OptionId;
import com.intellij.application.options.OptionsApplicabilityFilter;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPass;
import com.intellij.codeInsight.documentation.QuickDocOnMouseOverManager;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actions.CaretStopBoundary;
import com.intellij.openapi.editor.actions.CaretStopOptions;
import com.intellij.openapi.editor.actions.CaretStopOptionsTransposed;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.options.CompositeConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.panel.ComponentPanelBuilder;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsApplicationSettings;
import com.intellij.openapi.vcs.impl.LineStatusTrackerSettingListener;
import com.intellij.profile.codeInspection.ui.ErrorOptionsProvider;
import com.intellij.profile.codeInspection.ui.ErrorOptionsProviderEP;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBEmptyBorder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

public class EditorOptionsPanel extends CompositeConfigurable<ErrorOptionsProvider> implements SearchableConfigurable {
  private static final String ID = "preferences.editor";

  private JPanel    myBehaviourPanel;
  private JCheckBox myCbHighlightBraces;
  private static final String STRIP_CHANGED = ApplicationBundle.message("combobox.strip.modified.lines");

  private static final String STRIP_ALL  = ApplicationBundle.message("combobox.strip.all");
  private static final String STRIP_NONE = ApplicationBundle.message("combobox.strip.none");
  private JComboBox myStripTrailingSpacesCombo;

  private JCheckBox myCbVirtualSpace;
  private JCheckBox myCbCaretInsideTabs;

  private JTextField myRecentFilesLimitField;

  private JCheckBox myCbHighlightScope;

  private JCheckBox  myCbSmoothScrolling;
  private JCheckBox  myCbVirtualPageAtBottom;
  private JCheckBox  myCbEnableDnD;
  private JCheckBox  myCbEnableWheelFontChange;
  private JCheckBox  myCbHonorCamelHumpsWhenSelectingByClicking;

  private JPanel       myHighlightSettingsPanel;
  private JRadioButton myRbPreferScrolling;
  private JRadioButton myRbPreferMovingCaret;
  private JCheckBox    myCbRenameLocalVariablesInplace;
  private JCheckBox    myCbHighlightIdentifierUnderCaret;
  private JCheckBox    myCbEnsureBlankLineBeforeCheckBox;
  private JCheckBox    myShowNotificationAfterReformatCodeCheckBox;
  private JCheckBox    myShowNotificationAfterOptimizeImportsCheckBox;
  private JCheckBox    myCbUseSoftWrapsAtEditor;
  private JCheckBox    myCbUseCustomSoftWrapIndent;
  private JTextField   myCustomSoftWrapIndent;
  private JLabel       myCustomSoftWrapIndentLabel;
  private JCheckBox    myCbShowSoftWrapsOnlyOnCaretLine;
  private JCheckBox    myPreselectCheckBox;
  private JBCheckBox   myCbShowQuickDocOnMouseMove;
  private JBLabel      myQuickDocDelayLabel;
  private JTextField   myQuickDocDelayTextField;
  private JComboBox<String> myRichCopyColorSchemeComboBox;
  private JCheckBox    myShowInlineDialogForCheckBox;
  private JCheckBox    myCbEnableRichCopyByDefault;
  private JCheckBox    myShowLSTInGutterCheckBox;
  private JCheckBox    myShowWhitespacesModificationsInLSTGutterCheckBox;
  private JCheckBox    myCbKeepTrailingSpacesOnCaretLine;
  private JTextField   myRecentLocationsLimitField;
  private JBTextField  mySoftWrapFileMasks;
  private JLabel       mySoftWrapFileMasksHint;

  private JComboBox<EditorCaretStopPolicyItem.WordBoundary> myWordBoundaryCaretStopComboBox;
  private JComboBox<EditorCaretStopPolicyItem.LineBoundary> myLineBoundaryCaretStopComboBox;

  private static final String ACTIVE_COLOR_SCHEME = ApplicationBundle.message("combobox.richcopy.color.scheme.active");
  private static final UINumericRange RECENT_FILES_RANGE = new UINumericRange(50, 1, 500);
  private static final UINumericRange RECENT_LOCATIONS_RANGE = new UINumericRange(10, 1, 100);

  private final ErrorHighlightingPanel myErrorHighlightingPanel = new ErrorHighlightingPanel(getConfigurables());

  public EditorOptionsPanel() {
    if (SystemInfo.isMac) {
      myCbEnableWheelFontChange.setText(ApplicationBundle.message("checkbox.enable.ctrl.mousewheel.changes.font.size.macos"));
    }

    mySoftWrapFileMasks.getEmptyText().setText(ApplicationBundle.message("soft.wraps.file.masks.empty.text"));

    myStripTrailingSpacesCombo.addItem(STRIP_CHANGED);
    myStripTrailingSpacesCombo.addItem(STRIP_ALL);
    myStripTrailingSpacesCombo.addItem(STRIP_NONE);

    myStripTrailingSpacesCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCbKeepTrailingSpacesOnCaretLine.setEnabled(!STRIP_NONE.equals(myStripTrailingSpacesCombo.getSelectedItem()));
      }
    });


    myHighlightSettingsPanel.setLayout(new BorderLayout());
    myHighlightSettingsPanel.add(myErrorHighlightingPanel.getPanel(), BorderLayout.CENTER);


    myCbRenameLocalVariablesInplace.setVisible(OptionsApplicabilityFilter.isApplicable(OptionId.RENAME_IN_PLACE));

    myRichCopyColorSchemeComboBox.setRenderer(SimpleListCellRenderer.create("", value ->
      RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER.equals(value) ? ACTIVE_COLOR_SCHEME : value));

    initCaretStopComboBox(myWordBoundaryCaretStopComboBox, EditorCaretStopPolicyItem.WordBoundary.values());
    initCaretStopComboBox(myLineBoundaryCaretStopComboBox, EditorCaretStopPolicyItem.LineBoundary.values());

    initQuickDocProcessing();
    initSoftWrapsSettingsProcessing();
    initVcsSettingsProcessing();
  }

  private static <E extends EditorCaretStopPolicyItem> void initCaretStopComboBox(@NotNull JComboBox<E> comboBox, @NotNull E[] values) {
    final DefaultComboBoxModel<E> model = new EditorCaretStopPolicyItem.SeparatorAwareComboBoxModel<>();

    boolean lastWasOsDefault = false;
    for (E item : values) {
      final boolean isOsDefault = item.getOsDefault() != EditorCaretStopPolicyItem.OsDefault.NONE;
      if (lastWasOsDefault && !isOsDefault) model.addElement(null);
      lastWasOsDefault = isOsDefault;

      final int insertionIndex = item.getOsDefault().isIdeDefault() ? 0 : model.getSize();
      model.insertElementAt(item, insertionIndex);
    }

    comboBox.setModel(model);
    comboBox.setRenderer(new EditorCaretStopPolicyItem.SeparatorAwareListItemRenderer());
  }

  @Override
  public void reset() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    UISettings uiSettings = UISettings.getInstance();
    VcsApplicationSettings vcsSettings = VcsApplicationSettings.getInstance();

    // Display

    myCbSmoothScrolling.setSelected(editorSettings.isSmoothScrolling());

    // Caret Movement
    final CaretStopOptions caretStopOptions = editorSettings.getCaretStopOptions();
    myWordBoundaryCaretStopComboBox.setSelectedItem(EditorCaretStopPolicyItem.WordBoundary.itemForPolicy(caretStopOptions));
    myLineBoundaryCaretStopComboBox.setSelectedItem(EditorCaretStopPolicyItem.LineBoundary.itemForPolicy(caretStopOptions));

    // Brace highlighting

    myCbHighlightBraces.setSelected(codeInsightSettings.HIGHLIGHT_BRACES);
    myCbHighlightScope.setSelected(codeInsightSettings.HIGHLIGHT_SCOPE);
    myCbHighlightIdentifierUnderCaret.setSelected(codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET);

    // Virtual space

    myCbUseSoftWrapsAtEditor.setSelected(editorSettings.isUseSoftWraps());
    mySoftWrapFileMasks.setText(editorSettings.getSoftWrapFileMasks());
    myCbUseCustomSoftWrapIndent.setSelected(editorSettings.isUseCustomSoftWrapIndent());
    myCustomSoftWrapIndent.setText(Integer.toString(editorSettings.getCustomSoftWrapIndent()));
    myCbShowSoftWrapsOnlyOnCaretLine.setSelected(!editorSettings.isAllSoftWrapsShown());
    updateSoftWrapSettingsRepresentation();

    myCbVirtualSpace.setSelected(editorSettings.isVirtualSpace());
    myCbCaretInsideTabs.setSelected(editorSettings.isCaretInsideTabs());
    myCbVirtualPageAtBottom.setSelected(editorSettings.isAdditionalPageAtBottom());

    // Strip trailing spaces on save

    String stripTrailingSpaces = editorSettings.getStripTrailingSpaces();
    if(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE.equals(stripTrailingSpaces)) {
      myStripTrailingSpacesCombo.setSelectedItem(STRIP_NONE);
    }
    else if (EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED.equals(stripTrailingSpaces)) {
      myStripTrailingSpacesCombo.setSelectedItem(STRIP_CHANGED);
    }
    else if (EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE.equals(stripTrailingSpaces)) {
      myStripTrailingSpacesCombo.setSelectedItem(STRIP_ALL);
    }
    myCbKeepTrailingSpacesOnCaretLine.setSelected(editorSettings.isKeepTrailingSpacesOnCaretLine());

    myCbEnsureBlankLineBeforeCheckBox.setSelected(editorSettings.isEnsureNewLineAtEOF());
    myCbShowQuickDocOnMouseMove.setSelected(editorSettings.isShowQuickDocOnMouseOverElement());
    myQuickDocDelayTextField.setText(Long.toString(editorSettings.getQuickDocOnMouseOverElementDelayMillis()));
    myQuickDocDelayTextField.setEnabled(editorSettings.isShowQuickDocOnMouseOverElement());
    myQuickDocDelayLabel.setEnabled(editorSettings.isShowQuickDocOnMouseOverElement());

    // Advanced mouse
    myCbEnableDnD.setSelected(editorSettings.isDndEnabled());
    myCbEnableWheelFontChange.setSelected(editorSettings.isWheelFontChangeEnabled());
    myCbHonorCamelHumpsWhenSelectingByClicking.setSelected(editorSettings.isMouseClickSelectionHonorsCamelWords());

    myRbPreferMovingCaret.setSelected(editorSettings.isRefrainFromScrolling());
    myRbPreferScrolling.setSelected(!editorSettings.isRefrainFromScrolling());


    myRecentFilesLimitField.setText(Integer.toString(uiSettings.getRecentFilesLimit()));
    myRecentLocationsLimitField.setText(Integer.toString(uiSettings.getRecentLocationsLimit()));

    myCbRenameLocalVariablesInplace.setSelected(editorSettings.isVariableInplaceRenameEnabled());
    myPreselectCheckBox.setSelected(editorSettings.isPreselectRename());
    myShowInlineDialogForCheckBox.setSelected(editorSettings.isShowInlineLocalDialog());

    myShowNotificationAfterReformatCodeCheckBox.setSelected(editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION);
    myShowNotificationAfterOptimizeImportsCheckBox.setSelected(editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION);

    myShowLSTInGutterCheckBox.setSelected(vcsSettings.SHOW_LST_GUTTER_MARKERS);
    myShowWhitespacesModificationsInLSTGutterCheckBox.setSelected(vcsSettings.SHOW_WHITESPACES_IN_LST);
    myShowWhitespacesModificationsInLSTGutterCheckBox.setEnabled(myShowLSTInGutterCheckBox.isSelected());

    myErrorHighlightingPanel.reset();
    super.reset();

    RichCopySettings settings = RichCopySettings.getInstance();
    myCbEnableRichCopyByDefault.setSelected(settings.isEnabled());
    myRichCopyColorSchemeComboBox.removeAllItems();
    EditorColorsScheme[] schemes = EditorColorsManager.getInstance().getAllSchemes();
    myRichCopyColorSchemeComboBox.addItem(RichCopySettings.ACTIVE_GLOBAL_SCHEME_MARKER);
    for (EditorColorsScheme scheme : schemes) {
      myRichCopyColorSchemeComboBox.addItem(scheme.getName());
    }
    String toSelect = settings.getSchemeName();
    if (!StringUtil.isEmpty(toSelect)) {
      myRichCopyColorSchemeComboBox.setSelectedItem(toSelect);
    }
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    UISettings uiSettings=UISettings.getInstance();
    VcsApplicationSettings vcsSettings = VcsApplicationSettings.getInstance();

    // Display

    editorSettings.setSmoothScrolling(myCbSmoothScrolling.isSelected());

    // Caret Movement
    editorSettings.setCaretStopOptions(getCaretStopOptions());

    // Brace Highlighting

    codeInsightSettings.HIGHLIGHT_BRACES = myCbHighlightBraces.isSelected();
    codeInsightSettings.HIGHLIGHT_SCOPE = myCbHighlightScope.isSelected();
    codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET = myCbHighlightIdentifierUnderCaret.isSelected();
    clearAllIdentifierHighlighters();

    // Virtual space

    editorSettings.setUseSoftWraps(myCbUseSoftWrapsAtEditor.isSelected());
    editorSettings.setSoftWrapFileMasks(mySoftWrapFileMasks.getText());
    editorSettings.setUseCustomSoftWrapIndent(myCbUseCustomSoftWrapIndent.isSelected());
    editorSettings.setCustomSoftWrapIndent(getCustomSoftWrapIndent());
    editorSettings.setAllSoftwrapsShown(!myCbShowSoftWrapsOnlyOnCaretLine.isSelected());
    editorSettings.setVirtualSpace(myCbVirtualSpace.isSelected());
    editorSettings.setCaretInsideTabs(myCbCaretInsideTabs.isSelected());
    editorSettings.setAdditionalPageAtBottom(myCbVirtualPageAtBottom.isSelected());

    // Strip trailing spaces on save

    if(STRIP_NONE.equals(myStripTrailingSpacesCombo.getSelectedItem())) {
      editorSettings.setStripTrailingSpaces(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE);
    }
    else if(STRIP_CHANGED.equals(myStripTrailingSpacesCombo.getSelectedItem())){
      editorSettings.setStripTrailingSpaces(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED);
    }
    else {
      editorSettings.setStripTrailingSpaces(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE);
    }
    editorSettings.setKeepTrailingSpacesOnCaretLine(myCbKeepTrailingSpacesOnCaretLine.isSelected());

    editorSettings.setEnsureNewLineAtEOF(myCbEnsureBlankLineBeforeCheckBox.isSelected());

    if (myCbShowQuickDocOnMouseMove.isSelected() ^ editorSettings.isShowQuickDocOnMouseOverElement()) {
      boolean enabled = myCbShowQuickDocOnMouseMove.isSelected();
      editorSettings.setShowQuickDocOnMouseOverElement(enabled);
      ServiceManager.getService(QuickDocOnMouseOverManager.class).setEnabled(enabled);
    }

    int quickDocDelay = getQuickDocDelayFromGui();
    if (quickDocDelay != -1) {
      editorSettings.setQuickDocOnMouseOverElementDelayMillis(quickDocDelay);
    }

    editorSettings.setDndEnabled(myCbEnableDnD.isSelected());

    editorSettings.setWheelFontChangeEnabled(myCbEnableWheelFontChange.isSelected());
    editorSettings.setMouseClickSelectionHonorsCamelWords(myCbHonorCamelHumpsWhenSelectingByClicking.isSelected());
    editorSettings.setRefrainFromScrolling(myRbPreferMovingCaret.isSelected());

    editorSettings.setVariableInplaceRenameEnabled(myCbRenameLocalVariablesInplace.isSelected());
    editorSettings.setPreselectRename(myPreselectCheckBox.isSelected());
    editorSettings.setShowInlineLocalDialog(myShowInlineDialogForCheckBox.isSelected());

    editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION = myShowNotificationAfterReformatCodeCheckBox.isSelected();
    editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION = myShowNotificationAfterOptimizeImportsCheckBox.isSelected();

    boolean updateVcsSettings = false;
    if (vcsSettings.SHOW_WHITESPACES_IN_LST != myShowWhitespacesModificationsInLSTGutterCheckBox.isSelected()) {
      vcsSettings.SHOW_WHITESPACES_IN_LST = myShowWhitespacesModificationsInLSTGutterCheckBox.isSelected();
      updateVcsSettings = true;
    }
    if (vcsSettings.SHOW_LST_GUTTER_MARKERS != myShowLSTInGutterCheckBox.isSelected()) {
      vcsSettings.SHOW_LST_GUTTER_MARKERS = myShowLSTInGutterCheckBox.isSelected();
      updateVcsSettings = true;
    }
    if (updateVcsSettings) {
      ApplicationManager.getApplication().getMessageBus().syncPublisher(LineStatusTrackerSettingListener.TOPIC).settingsUpdated();
    }

    reinitAllEditors();

    boolean uiSettingsChanged = false;
    String temp=myRecentFilesLimitField.getText();
    if(temp.trim().length() > 0){
      try {
        int newRecentFilesLimit = Integer.parseInt(temp);
        if (newRecentFilesLimit > 0 && uiSettings.getRecentFilesLimit() != newRecentFilesLimit) {
          uiSettings.getState().setRecentFilesLimit(newRecentFilesLimit);
          uiSettingsChanged = true;
        }
      }catch (NumberFormatException ignored){}
    }
    if(uiSettingsChanged){
      uiSettings.fireUISettingsChanged();
    }

    uiSettingsChanged = setRecentLocationLimit(uiSettings, myRecentLocationsLimitField.getText()) || uiSettingsChanged;
    if (uiSettingsChanged) {
      uiSettings.fireUISettingsChanged();
    }

    myErrorHighlightingPanel.apply();
    super.apply();
    UISettings.getInstance().fireUISettingsChanged();

    RichCopySettings settings = RichCopySettings.getInstance();
    settings.setEnabled(myCbEnableRichCopyByDefault.isSelected());
    Object item = myRichCopyColorSchemeComboBox.getSelectedItem();
    if (item instanceof String) {
      settings.setSchemeName(item.toString());
    }

    restartDaemons();
    ApplicationManager.getApplication().getMessageBus().syncPublisher(EditorOptionsListener.OPTIONS_PANEL_TOPIC).changesApplied();
  }

  private void createUIComponents() {
    mySoftWrapFileMasks = new JBTextField();
    mySoftWrapFileMasksHint = ComponentPanelBuilder.createCommentComponent(ApplicationBundle.message("soft.wraps.file.masks.hint"), true);
    mySoftWrapFileMasksHint.setBorder(new JBEmptyBorder(ComponentPanelBuilder.computeCommentInsets(mySoftWrapFileMasks, true)));
  }

  private static boolean setRecentLocationLimit(@NotNull UISettings uiSettings, @NotNull String recentLocationsLimit) {
    try {
      int newRecentLocationsLimit = Integer.parseInt(recentLocationsLimit.trim());
      if (uiSettings.getRecentLocationsLimit() != newRecentLocationsLimit) {
        uiSettings.getState().setRecentLocationsLimit(newRecentLocationsLimit);
        return true;
      }
    }
    catch (NumberFormatException ignored) {
    }
    return false;
  }

  private int getQuickDocDelayFromGui() {
    try {
      return EditorSettingsExternalizable.QUICK_DOC_DELAY_RANGE.fit(Integer.parseInt(myQuickDocDelayTextField.getText().trim()));
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }

  public static void restartDaemons() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      DaemonCodeAnalyzer.getInstance(project).settingsChanged();
    }
  }

  private static void clearAllIdentifierHighlighters() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
        if (fileEditor instanceof TextEditor) {
          Document document = ((TextEditor)fileEditor).getEditor().getDocument();
          IdentifierHighlighterPass.clearMyHighlights(document, project);
        }
      }
    }
  }

  public static void reinitAllEditors() {
    EditorFactory.getInstance().refreshAllEditors();
  }

  @NotNull
  @Override
  protected List<ErrorOptionsProvider> createConfigurables() {
    return ConfigurableWrapper.createConfigurables(ErrorOptionsProviderEP.EP_NAME);
  }

  @Override
  public boolean isModified() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
    VcsApplicationSettings vcsSettings = VcsApplicationSettings.getInstance();

    // Display
    boolean isModified = isModified(myCbSmoothScrolling, editorSettings.isSmoothScrolling());

    // Caret Movement
    isModified |= !getCaretStopOptions().equals(editorSettings.getCaretStopOptions());

    // Brace highlighting
    isModified |= isModified(myCbHighlightBraces, codeInsightSettings.HIGHLIGHT_BRACES);
    isModified |= isModified(myCbHighlightScope, codeInsightSettings.HIGHLIGHT_SCOPE);
    isModified |= isModified(myCbHighlightIdentifierUnderCaret, codeInsightSettings.HIGHLIGHT_IDENTIFIER_UNDER_CARET);

    // Virtual space
    isModified |= isModified(myCbUseSoftWrapsAtEditor, editorSettings.isUseSoftWraps());
    isModified |= !mySoftWrapFileMasks.getText().equals(editorSettings.getSoftWrapFileMasks());
    isModified |= isModified(myCbUseCustomSoftWrapIndent, editorSettings.isUseCustomSoftWrapIndent());
    isModified |= editorSettings.getCustomSoftWrapIndent() != getCustomSoftWrapIndent();
    isModified |= isModified(myCbShowSoftWrapsOnlyOnCaretLine, !editorSettings.isAllSoftWrapsShown());
    isModified |= isModified(myCbVirtualSpace, editorSettings.isVirtualSpace());
    isModified |= isModified(myCbCaretInsideTabs, editorSettings.isCaretInsideTabs());
    isModified |= isModified(myCbVirtualPageAtBottom, editorSettings.isAdditionalPageAtBottom());

    // Paste

    // Strip trailing spaces, ensure EOL on EOF on save
    isModified |= !getStripTrailingSpacesValue().equals(editorSettings.getStripTrailingSpaces());
    isModified |= isModified(myCbKeepTrailingSpacesOnCaretLine, editorSettings.isKeepTrailingSpacesOnCaretLine());

    isModified |= isModified(myCbEnsureBlankLineBeforeCheckBox, editorSettings.isEnsureNewLineAtEOF());

    isModified |= isModified(myCbShowQuickDocOnMouseMove, editorSettings.isShowQuickDocOnMouseOverElement());
    isModified |= isModified(myQuickDocDelayTextField, editorSettings.getQuickDocOnMouseOverElementDelayMillis(), EditorSettingsExternalizable.QUICK_DOC_DELAY_RANGE);

    // advanced mouse
    isModified |= isModified(myCbEnableDnD, editorSettings.isDndEnabled());
    isModified |= isModified(myCbEnableWheelFontChange, editorSettings.isWheelFontChangeEnabled());
    isModified |= isModified(myCbHonorCamelHumpsWhenSelectingByClicking, editorSettings.isMouseClickSelectionHonorsCamelWords());

    isModified |= isModified(myRbPreferMovingCaret,  editorSettings.isRefrainFromScrolling());


    isModified |= isModified(myRecentFilesLimitField, UISettings.getInstance().getRecentFilesLimit(), RECENT_FILES_RANGE);
    isModified |= isModified(myRecentLocationsLimitField, UISettings.getInstance().getRecentLocationsLimit(), RECENT_LOCATIONS_RANGE);
    isModified |= isModified(myCbRenameLocalVariablesInplace, editorSettings.isVariableInplaceRenameEnabled());
    isModified |= isModified(myPreselectCheckBox, editorSettings.isPreselectRename());
    isModified |= isModified(myShowInlineDialogForCheckBox, editorSettings.isShowInlineLocalDialog());

    isModified |= isModified(myShowNotificationAfterReformatCodeCheckBox, editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION);
    isModified |= isModified(myShowNotificationAfterOptimizeImportsCheckBox, editorSettings.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION);

    isModified |= isModified(myShowLSTInGutterCheckBox, vcsSettings.SHOW_LST_GUTTER_MARKERS);
    isModified |= isModified(myShowWhitespacesModificationsInLSTGutterCheckBox, vcsSettings.SHOW_WHITESPACES_IN_LST);

    isModified |= myErrorHighlightingPanel.isModified();
    isModified |= super.isModified();

    RichCopySettings settings = RichCopySettings.getInstance();
    isModified |= isModified(myCbEnableRichCopyByDefault, settings.isEnabled());
    isModified |= !Comparing.equal(settings.getSchemeName(), myRichCopyColorSchemeComboBox.getSelectedItem());

    return isModified;
  }

  @NotNull
  protected CaretStopOptions getCaretStopOptions() {
    return new CaretStopOptionsTransposed(
      getCaretStopBoundary(myWordBoundaryCaretStopComboBox, CaretStopOptionsTransposed.DEFAULT.getWordBoundary()),
      getCaretStopBoundary(myLineBoundaryCaretStopComboBox, CaretStopOptionsTransposed.DEFAULT.getLineBoundary())).toCaretStopOptions();
  }

  @NotNull
  protected static CaretStopBoundary getCaretStopBoundary(@NotNull JComboBox<? extends EditorCaretStopPolicyItem> comboBox,
                                                          @NotNull CaretStopBoundary defaultValue) {
    final Object selectedItem = comboBox.getSelectedItem();
    if (!(selectedItem instanceof EditorCaretStopPolicyItem)) return defaultValue;
    return ((EditorCaretStopPolicyItem)selectedItem).getCaretStopBoundary();
  }

  @NotNull
  @EditorSettingsExternalizable.StripTrailingSpaces
  private String getStripTrailingSpacesValue() {
    Object selectedItem = myStripTrailingSpacesCombo.getSelectedItem();
    if(STRIP_NONE.equals(selectedItem)) {
      return EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE;
    }
    if(STRIP_CHANGED.equals(selectedItem)){
      return EditorSettingsExternalizable.STRIP_TRAILING_SPACES_CHANGED;
    }
    return EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE;
  }

  private int getCustomSoftWrapIndent() {
    String indentAsString = myCustomSoftWrapIndent.getText();
    int defaultIndent = 0;
    if (indentAsString == null) {
      return defaultIndent;
    }
    try {
      int indent = Integer.parseInt(indentAsString.trim());
      return indent >= 0 ? indent : defaultIndent;
    } catch (IllegalArgumentException e) {
      // Ignore
    }
    return defaultIndent;
  }

  private void initQuickDocProcessing() {
    myCbShowQuickDocOnMouseMove.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        myQuickDocDelayTextField.setEnabled(myCbShowQuickDocOnMouseMove.isSelected());
        myQuickDocDelayLabel.setEnabled(myCbShowQuickDocOnMouseMove.isSelected());
      }
    });
  }

  private void initSoftWrapsSettingsProcessing() {
    myCbUseCustomSoftWrapIndent.addItemListener(e -> updateSoftWrapSettingsRepresentation());
  }

  private void updateSoftWrapSettingsRepresentation() {
    myCustomSoftWrapIndent.setEnabled(myCbUseCustomSoftWrapIndent.isSelected());
    myCustomSoftWrapIndentLabel.setEnabled(myCbUseCustomSoftWrapIndent.isSelected());
  }

  private void initVcsSettingsProcessing() {
    myShowLSTInGutterCheckBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        myShowWhitespacesModificationsInLSTGutterCheckBox.setEnabled(myShowLSTInGutterCheckBox.isSelected());
      }
    });
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @Override
  public String getDisplayName() {
    return ApplicationBundle.message("title.editor");
  }

  @Override
  public String getHelpTopic() {
    return ID;
  }

  @Override
  public JComponent createComponent() {
    return myBehaviourPanel;
  }
}
