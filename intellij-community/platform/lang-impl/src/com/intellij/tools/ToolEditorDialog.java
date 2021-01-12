// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.execution.filters.RegexpFilter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.macro.MacroManager;
import com.intellij.ide.macro.MacrosDialog;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.MacKeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.RefreshablePanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AbstractTitledSeparatorWithIcon;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.List;

public class ToolEditorDialog extends DialogWrapper {
  private static final String ADVANCED_OPTIONS_EXPANDED_KEY = "ExternalToolDialog.advanced.expanded";
  private static final boolean ADVANCED_OPTIONS_EXPANDED_DEFAULT = false;

  private static final Function<String, List<String>> OUTPUT_FILTERS_SPLITTER = s -> StringUtil.split(s, MacKeymapUtil.RETURN);
  private static final Function<List<String>, String> OUTPUT_FILTERS_JOINER = strings -> StringUtil.join(strings, MacKeymapUtil.RETURN);

  private final Project myProject;
  private boolean myEnabled;

  private JPanel myMainPanel;
  private JTextField myNameField;
  private ComboBox<String> myGroupCombo;
  private JTextField myDescriptionField;
  private TextFieldWithBrowseButton myProgramField;
  private RawCommandLineEditor myArgumentsField;
  private TextFieldWithBrowseButton myWorkingDirField;
  private JPanel myAdditionalOptionsPanel;
  private AbstractTitledSeparatorWithIcon myAdvancedOptionsSeparator;
  private JPanel myAdvancedOptionsPanel;
  private JBCheckBox mySynchronizedAfterRunCheckbox;
  private JBCheckBox myUseConsoleCheckbox;
  private JBCheckBox myShowConsoleOnStdOutCheckbox;
  private JBCheckBox myShowConsoleOnStdErrCheckbox;
  private RawCommandLineEditor myOutputFilterField;

  protected ToolEditorDialog(JComponent parent, String title) {
    super(parent, true);

    myArgumentsField.setDialogCaption("Program Arguments");

    DataContext dataContext = DataManager.getInstance().getDataContext(parent);
    myProject = CommonDataKeys.PROJECT.getData(dataContext);
    MacroManager.getInstance().cacheMacrosPreview(dataContext);
    setTitle(title);
    myAdvancedOptionsPanel.setBorder(JBUI.Borders.emptyLeft(IdeBorderFactory.TITLED_BORDER_INDENT));

    boolean on = PropertiesComponent.getInstance().getBoolean(ADVANCED_OPTIONS_EXPANDED_KEY, ADVANCED_OPTIONS_EXPANDED_DEFAULT);
    if (on) {
      myAdvancedOptionsSeparator.on();
    }
    else {
      myAdvancedOptionsSeparator.off();
    }

    init();
    addListeners();
  }

  @Override
  protected String getHelpId() {
    return "preferences.externalToolsEdit";
  }

  @Nullable
  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  protected JPanel createCenterPanel() {
    fillAdditionalOptionsPanel(myAdditionalOptionsPanel);
    return myMainPanel;
  }

  protected void fillAdditionalOptionsPanel(@NotNull final JPanel panel) {}

  protected void addWorkingDirectoryBrowseAction(@NotNull final TextFieldWithBrowseButton workingDirField) {
    workingDirField.addBrowseFolderListener(null, null, myProject, FileChooserDescriptorFactory.createSingleFolderDescriptor());
  }

  protected void addProgramBrowseAction(@NotNull final TextFieldWithBrowseButton programField) {
    programField.addBrowseFolderListener(
      new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(), myProject) {
        @Override
        protected void onFileChosen(@NotNull VirtualFile file) {
          super.onFileChosen(file);

          String workingDirectory = myWorkingDirField.getText();
          if (workingDirectory.isEmpty()) {
            VirtualFile parent = file.getParent();
            if (parent != null && parent.isDirectory()) {
              myWorkingDirField.setText(parent.getPresentableUrl());
            }
          }
        }
      });
  }

  private void createUIComponents() {
    myOutputFilterField = new RawCommandLineEditor(OUTPUT_FILTERS_SPLITTER, OUTPUT_FILTERS_JOINER);

    myAdvancedOptionsSeparator = new AbstractTitledSeparatorWithIcon(AllIcons.General.ArrowRight,
                                                                     AllIcons.General.ArrowDown,
                                                                     "Advanced Options") {
      @Override
      protected RefreshablePanel createPanel() {
        return new RefreshablePanel() {
          @Override
          public void refresh() {
          }

          @Override
          public JPanel getPanel() {
            return new JPanel();
          }
        };
      }

      @Override
      protected void initOnImpl() {
      }

      @Override
      protected void onImpl() {
        myAdvancedOptionsPanel.setVisible(true);
        PropertiesComponent.getInstance().setValue(ADVANCED_OPTIONS_EXPANDED_KEY, true, ADVANCED_OPTIONS_EXPANDED_DEFAULT);
      }

      @Override
      protected void offImpl() {
        myAdvancedOptionsPanel.setVisible(false);
        PropertiesComponent.getInstance().setValue(ADVANCED_OPTIONS_EXPANDED_KEY, false, ADVANCED_OPTIONS_EXPANDED_DEFAULT);

        final int extraHeight = myAdvancedOptionsPanel.getHeight();
        ApplicationManager.getApplication().invokeLater(() -> {
          final Dimension size = ToolEditorDialog.this.getSize();
          ToolEditorDialog.this.setSize(size.width, size.height - extraHeight);
          ToolEditorDialog.this.repaint();
        }, ModalityState.current(), o -> !ToolEditorDialog.this.isShowing());
      }
    };
  }

  private void addListeners() {
    addProgramBrowseAction(myProgramField);
    addWorkingDirectoryBrowseAction(myWorkingDirField);

    MacrosDialog.addTextFieldExtension((ExtendableTextField)myProgramField.getTextField());
    MacrosDialog.addTextFieldExtension((ExtendableTextField)myArgumentsField.getTextField());
    MacrosDialog.addTextFieldExtension((ExtendableTextField)myWorkingDirField.getTextField());

    myUseConsoleCheckbox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        myShowConsoleOnStdOutCheckbox.setEnabled(myUseConsoleCheckbox.isSelected());
        myShowConsoleOnStdErrCheckbox.setEnabled(myUseConsoleCheckbox.isSelected());
      }
    });
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (myNameField.getText().trim().isEmpty()) {
      return new ValidationInfo(ToolsBundle.message("dialog.message.specify.the.tool.name"), myNameField);
    }

    for (String s : OUTPUT_FILTERS_SPLITTER.fun(myOutputFilterField.getText())) {
      if (!s.contains(RegexpFilter.FILE_PATH_MACROS)) {
        return new ValidationInfo(
          ToolsBundle.message("dialog.message.each.output.filter.must.contain.0.macro", RegexpFilter.FILE_PATH_MACROS), myOutputFilterField);
      }
    }

    return null;
  }

  public Tool getData() {
    Tool tool = createTool();

    tool.setName(convertString(myNameField.getText()));
    tool.setDescription(convertString(myDescriptionField.getText()));
    Object selectedItem = myGroupCombo.getSelectedItem();
    tool.setGroup(StringUtil.notNullize(selectedItem != null ? convertString(selectedItem.toString()) : ""));
    tool.setUseConsole(myUseConsoleCheckbox.isSelected());
    tool.setShowConsoleOnStdOut(myShowConsoleOnStdOutCheckbox.isSelected());
    tool.setShowConsoleOnStdErr(myShowConsoleOnStdErrCheckbox.isSelected());
    tool.setFilesSynchronizedAfterRun(mySynchronizedAfterRunCheckbox.isSelected());
    tool.setEnabled(myEnabled);

    tool.setWorkingDirectory(StringUtil.nullize(FileUtil.toSystemIndependentName(myWorkingDirField.getText())));
    tool.setProgram(convertString(myProgramField.getText()));
    tool.setParameters(convertString(myArgumentsField.getText()));

    final List<String> filterStrings = OUTPUT_FILTERS_SPLITTER.fun(myOutputFilterField.getText().trim());
    final FilterInfo[] filters = ContainerUtil.map2Array(filterStrings, FilterInfo.class, s -> new FilterInfo(s, "", ""));
    tool.setOutputFilters(filters);

    return tool;
  }

  protected Tool createTool() {
    return new Tool();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#com.intellij.tools.ToolEditorDialog";
  }

  /**
   * Initialize controls
   */
  protected void setData(Tool tool, String[] existingGroups) {
    myNameField.setText(tool.getName());
    myDescriptionField.setText(tool.getDescription());
    if (myGroupCombo.getItemCount() > 0) {
      myGroupCombo.removeAllItems();
    }
    for (String existingGroup : existingGroups) {
      if (existingGroup != null) {
        myGroupCombo.addItem(existingGroup);
      }
    }
    myGroupCombo.setSelectedItem(tool.getGroup());
    myUseConsoleCheckbox.setSelected(tool.isUseConsole());
    myShowConsoleOnStdOutCheckbox.setEnabled(myUseConsoleCheckbox.isSelected());
    myShowConsoleOnStdOutCheckbox.setSelected(tool.isShowConsoleOnStdOut());
    myShowConsoleOnStdErrCheckbox.setEnabled(myUseConsoleCheckbox.isSelected());
    myShowConsoleOnStdErrCheckbox.setSelected(tool.isShowConsoleOnStdErr());
    mySynchronizedAfterRunCheckbox.setSelected(tool.synchronizeAfterExecution());
    myEnabled = tool.isEnabled();
    myWorkingDirField.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(tool.getWorkingDirectory())));
    myProgramField.setText(tool.getProgram());
    myArgumentsField.setText(tool.getParameters());
    myOutputFilterField.setText(OUTPUT_FILTERS_JOINER.fun(ContainerUtil.map(tool.getOutputFilters(), info -> info.getRegExp())));
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  private static String convertString(String s) {
    if (s != null && s.trim().isEmpty()) return null;
    return s;
  }
}