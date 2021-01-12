// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.impl.ConsoleBuffer;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsState;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingManagerImpl;
import com.intellij.openapi.vfs.encoding.EncodingReference;
import com.intellij.ui.*;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author peter
 */
public class ConsoleConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private static final Logger LOG = Logger.getInstance(ConsoleConfigurable.class);

  private JPanel myMainComponent;
  private JCheckBox myCbUseSoftWrapsAtConsole;
  private JTextField myCommandsHistoryLimitField;
  private JCheckBox myCbOverrideConsoleCycleBufferSize;
  private ConsoleEncodingComboBox myEncodingComboBox;
  private JTextField myConsoleCycleBufferSizeField;
  private JLabel myConsoleBufferSizeWarningLabel;

  private MyAddDeleteListPanel myPositivePanel;
  private MyAddDeleteListPanel myNegativePanel;
  private final ConsoleFoldingSettings mySettings = ConsoleFoldingSettings.getSettings();

  @Override
  public JComponent createComponent() {
    if (myMainComponent == null) {
      myMainComponent = new JPanel(new BorderLayout());
      myCbUseSoftWrapsAtConsole = new JCheckBox(ApplicationBundle.message("checkbox.use.soft.wraps.at.console"), false);
      myCommandsHistoryLimitField = new JTextField(4);
      myCbOverrideConsoleCycleBufferSize = new JCheckBox(ApplicationBundle.message("checkbox.override.console.cycle.buffer.size", String.valueOf(ConsoleBuffer.getLegacyCycleBufferSize() / 1024)), false);
      myCbOverrideConsoleCycleBufferSize.addChangeListener(e -> {
        myConsoleCycleBufferSizeField.setEnabled(myCbOverrideConsoleCycleBufferSize.isSelected());
        myConsoleBufferSizeWarningLabel.setVisible(myCbOverrideConsoleCycleBufferSize.isSelected());
      });
      myConsoleCycleBufferSizeField = new JTextField(4);
      myConsoleBufferSizeWarningLabel = new JLabel();
      myConsoleBufferSizeWarningLabel.setForeground(JBColor.red);
      myConsoleCycleBufferSizeField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          updateWarningLabel();
        }
      });
      myEncodingComboBox = new ConsoleEncodingComboBox();

      JPanel northPanel = new JPanel(new GridBagLayout());
      GridBag gridBag = new GridBag();
      gridBag.anchor(GridBagConstraints.WEST).setDefaultAnchor(GridBagConstraints.WEST);
      northPanel.add(myCbUseSoftWrapsAtConsole, gridBag.nextLine().next());
      northPanel.add(Box.createHorizontalGlue(), gridBag.next().coverLine());
      JLabel label = new JLabel(ApplicationBundle.message("editbox.console.history.limit"));
      label.setLabelFor(myCommandsHistoryLimitField);
      northPanel.add(label, gridBag.nextLine().next());
      northPanel.add(myCommandsHistoryLimitField, gridBag.next());
      if (ConsoleBuffer.useCycleBuffer()) {
        northPanel.add(myCbOverrideConsoleCycleBufferSize, gridBag.nextLine().next());
        northPanel.add(myConsoleCycleBufferSizeField, gridBag.next());
        northPanel.add(new JLabel(ExecutionBundle.message("settings.console.kb")), gridBag.next());
        northPanel.add(Box.createHorizontalStrut(JBUIScale.scale(20)), gridBag.next());
        northPanel.add(myConsoleBufferSizeWarningLabel, gridBag.next());
      }
      northPanel.add(new JLabel(ApplicationBundle.message("combobox.console.default.encoding.label")), gridBag.nextLine().next());
      northPanel.add(myEncodingComboBox,gridBag.next().coverLine());
      if (!editFoldingsOnly()) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(northPanel, BorderLayout.WEST);
        myMainComponent.add(wrapper, BorderLayout.NORTH);
      }
      Splitter splitter = new Splitter(true);
      myMainComponent.add(splitter, BorderLayout.CENTER);
      myPositivePanel =
        new MyAddDeleteListPanel(ApplicationBundle.message("console.fold.console.lines"),
                                 ApplicationBundle.message("console.enter.substring.folded"));
      myNegativePanel = new MyAddDeleteListPanel(ApplicationBundle.message("console.fold.exceptions"),
                                                 ApplicationBundle.message("console.enter.substring.dont.fold:"));
      splitter.setFirstComponent(myPositivePanel);
      splitter.setSecondComponent(myNegativePanel);

      myPositivePanel.getEmptyText().setText(ApplicationBundle.message("console.fold.nothing"));
      myNegativePanel.getEmptyText().setText(ApplicationBundle.message("console.no.exceptions"));
    }
    return myMainComponent;
  }

  private void updateWarningLabel() {
    try {
      int value = Integer.parseInt(myConsoleCycleBufferSizeField.getText().trim());
      if (value <= 0) {
        myConsoleBufferSizeWarningLabel.setText(ApplicationBundle.message("checkbox.override.console.cycle.buffer.size.warning.unlimited"));
        return;
      }
      if (value > FileUtilRt.LARGE_FOR_CONTENT_LOADING / 1024) {
        myConsoleBufferSizeWarningLabel.setText(ApplicationBundle.message("checkbox.override.console.cycle.buffer.size.warning.too.large"));
        return;
      }
    }
    catch (NumberFormatException ignored) {}
    myConsoleBufferSizeWarningLabel.setText("");
  }

  protected boolean editFoldingsOnly() {
    return false;
  }

  public void addRule(@NotNull String rule) {
    myPositivePanel.addRule(rule);
  }

  @Override
  public boolean isModified() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    boolean isModified = !ContainerUtil.newHashSet(myNegativePanel.getListItems()).equals(new HashSet<>(mySettings.getNegativePatterns()));
    isModified |= !ContainerUtil.newHashSet(myPositivePanel.getListItems()).equals(new HashSet<>(mySettings.getPositivePatterns()));
    isModified |= isModified(myCbUseSoftWrapsAtConsole, editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.CONSOLE));
    UISettings uiSettings = UISettings.getInstance();
    isModified |= isModified(myCommandsHistoryLimitField, uiSettings.getConsoleCommandHistoryLimit());
    if (ConsoleBuffer.useCycleBuffer()) {
      isModified |= isModified(myCbOverrideConsoleCycleBufferSize, uiSettings.getOverrideConsoleCycleBufferSize());
      isModified |= isModified(myConsoleCycleBufferSizeField, uiSettings.getConsoleCycleBufferSizeKb());
    }
    isModified |= isEncodingModified();

    return isModified;
  }

  private static boolean isModified(JTextField textField, int value) {
    try {
      int fieldValue = Integer.parseInt(textField.getText().trim());
      return fieldValue != value;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isEncodingModified() {
    EncodingManager encodingManager = EncodingManager.getInstance();

    EncodingReference defaultEncoding = EncodingReference.DEFAULT;
    if (encodingManager instanceof EncodingManagerImpl) {
      defaultEncoding = ((EncodingManagerImpl)encodingManager).getDefaultConsoleEncodingReference();
    }

    EncodingReference consoleEncoding = myEncodingComboBox.getSelectedEncodingReference();
    return !defaultEncoding.equals(consoleEncoding);
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    UISettings settingsManager = UISettings.getInstance();
    UISettingsState uiSettings = settingsManager.getState();
    EncodingManager encodingManager = EncodingManager.getInstance();

    editorSettings.setUseSoftWraps(myCbUseSoftWrapsAtConsole.isSelected(), SoftWrapAppliancePlaces.CONSOLE);
    boolean uiSettingsChanged = false;
    if (isModified(myCommandsHistoryLimitField, uiSettings.getConsoleCommandHistoryLimit())) {
      uiSettings.setConsoleCommandHistoryLimit(Math.max(0, Math.min(1000, Integer.parseInt(myCommandsHistoryLimitField.getText().trim()))));
      uiSettingsChanged = true;
    }
    if (ConsoleBuffer.useCycleBuffer()) {
      if (isModified(myCbOverrideConsoleCycleBufferSize, uiSettings.getOverrideConsoleCycleBufferSize())) {
        uiSettings.setOverrideConsoleCycleBufferSize(myCbOverrideConsoleCycleBufferSize.isSelected());
        uiSettingsChanged = true;
      }
      if (isModified(myConsoleCycleBufferSizeField, uiSettings.getConsoleCycleBufferSizeKb())) {
        uiSettings.setConsoleCycleBufferSizeKb(Math.max(0, Integer.parseInt(myConsoleCycleBufferSizeField.getText().trim())));
        uiSettingsChanged = true;
      }
    }
    if (uiSettingsChanged) {
      settingsManager.fireUISettingsChanged();
    }
    if (isEncodingModified()) {
      if (encodingManager instanceof EncodingManagerImpl) {
        ((EncodingManagerImpl)encodingManager).setDefaultConsoleEncodingReference(myEncodingComboBox.getSelectedEncodingReference());
      }
    }

    myNegativePanel.applyTo(mySettings.getNegativePatterns());
    myPositivePanel.applyTo(mySettings.getPositivePatterns());
  }

  @Override
  public void reset() {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    UISettingsState uiSettings = UISettings.getInstance().getState();
    EncodingManager encodingManager = EncodingManager.getInstance();

    myCbUseSoftWrapsAtConsole.setSelected(editorSettings.isUseSoftWraps(SoftWrapAppliancePlaces.CONSOLE));
    myCommandsHistoryLimitField.setText(Integer.toString(uiSettings.getConsoleCommandHistoryLimit()));

    myCbOverrideConsoleCycleBufferSize.setEnabled(ConsoleBuffer.useCycleBuffer());
    myCbOverrideConsoleCycleBufferSize.setSelected(uiSettings.getOverrideConsoleCycleBufferSize());
    myConsoleCycleBufferSizeField.setEnabled(ConsoleBuffer.useCycleBuffer() && uiSettings.getOverrideConsoleCycleBufferSize());
    myConsoleCycleBufferSizeField.setText(Integer.toString(uiSettings.getConsoleCycleBufferSizeKb()));

    EncodingReference encodingReference = EncodingReference.DEFAULT;
    if (encodingManager instanceof EncodingManagerImpl) {
      encodingReference = ((EncodingManagerImpl)encodingManager).getDefaultConsoleEncodingReference();
    } else {
      LOG.warn("Expected EncodingManagerImpl but got " + encodingManager.getClass().getName());
    }
    myEncodingComboBox.reset(encodingReference);

    myNegativePanel.resetFrom(mySettings.getNegativePatterns());
    myPositivePanel.resetFrom(mySettings.getPositivePatterns());
  }

  @Override
  public void disposeUIResources() {
    myMainComponent = null;
    myNegativePanel = null;
    myPositivePanel = null;
  }

  @Override
  @NotNull
  public String getId() {
    return getDisplayName();
  }

  @Override
  public String getDisplayName() {
    return ExecutionBundle.message("configurable.ConsoleConfigurable.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "reference.idesettings.console.folding";
  }

  private static class MyAddDeleteListPanel extends AddEditDeleteListPanel<String> {
    private final String myQuery;

    MyAddDeleteListPanel(String title, String query) {
      super(title, new ArrayList<>());
      myQuery = query;
      new ListSpeedSearch(myList);
    }

    @Override
    protected Border createTitledBorder(String title) {
      return IdeBorderFactory.createTitledBorder(title, false, JBUI.insetsTop(8)).setShowLine(false);
    }

    @Override
    @Nullable
    protected String findItemToAdd() {
      return showEditDialog("");
    }

    @Nullable
    private String showEditDialog(final String initialValue) {
      return Messages.showInputDialog(this, myQuery, ExecutionBundle.message("dialog.title.folding.pattern"), Messages.getQuestionIcon(), initialValue, new InputValidatorEx() {
        @Override
        public boolean checkInput(String inputString) {
          return !StringUtil.isEmpty(inputString);
        }

        @Override
        public boolean canClose(String inputString) {
          return !StringUtil.isEmpty(inputString);
        }

        @Nullable
        @Override
        public String getErrorText(String inputString) {
          if (!checkInput(inputString)) {
            return "Console folding rule string cannot be empty";
          }
          return null;
        }
      });
    }

    void resetFrom(List<String> patterns) {
      myListModel.clear();
      patterns.stream().sorted(String.CASE_INSENSITIVE_ORDER).forEach(myListModel::addElement);
    }

    void applyTo(List<? super String> patterns) {
      patterns.clear();
      for (Object o : getListItems()) {
        patterns.add((String)o);
      }
    }

    public void addRule(String rule) {
      addElement(rule);
    }

    @Override
    protected String editSelectedItem(String item) {
      return showEditDialog(item);
    }
  }
}
