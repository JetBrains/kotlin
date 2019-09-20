// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.TextAccessor;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommonProgramParametersPanel extends JPanel implements PanelWithAnchor {
  protected LabeledComponent<RawCommandLineEditor> myProgramParametersComponent;
  protected LabeledComponent<JComponent> myWorkingDirectoryComponent;
  @Deprecated
  protected TextFieldWithBrowseButton myWorkingDirectoryField;
  protected MacroComboBoxWithBrowseButton myWorkingDirectoryComboBox;
  protected EnvironmentVariablesComponent myEnvVariablesComponent;
  protected JComponent myAnchor;

  private Module myModuleContext = null;

  public CommonProgramParametersPanel() {
    this(true);
  }

  public CommonProgramParametersPanel(boolean init) {
    super();

    setLayout(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false));

    if (init) {
      init();
    }
  }

  protected void init() {
    initComponents();
    updateUI();
    setupAnchor();
  }

  protected void setupAnchor() {
    myAnchor = UIUtil.mergeComponentsWithAnchor(myProgramParametersComponent, myWorkingDirectoryComponent, myEnvVariablesComponent);
  }

  @Nullable
  protected Project getProject() {
    return myModuleContext != null ? myModuleContext.getProject() : null;
  }

  protected void initComponents() {
    myProgramParametersComponent = LabeledComponent.create(new RawCommandLineEditor(),
                                                           ExecutionBundle.message("run.configuration.program.parameters"));
    FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    //noinspection DialogTitleCapitalization
    fileChooserDescriptor.setTitle(ExecutionBundle.message("select.working.directory.message"));
    myWorkingDirectoryComboBox = new MacroComboBoxWithBrowseButton(fileChooserDescriptor, getProject());

    // for backward compatibility: com.microsoft.tooling.msservices.intellij.azure:3.0.11
    myWorkingDirectoryField = new TextFieldWithBrowseButton();
    addWorkingDirectoryListener(myWorkingDirectoryField::setText);

    myWorkingDirectoryComponent = LabeledComponent.create(myWorkingDirectoryComboBox, ExecutionBundle.message("run.configuration.working.directory.label"));
    myEnvVariablesComponent = new EnvironmentVariablesComponent();

    myEnvVariablesComponent.setLabelLocation(BorderLayout.WEST);
    myProgramParametersComponent.setLabelLocation(BorderLayout.WEST);
    myWorkingDirectoryComponent.setLabelLocation(BorderLayout.WEST);

    addComponents();

    setPreferredSize(new Dimension(10, 10));

    copyDialogCaption(myProgramParametersComponent);
  }

  /**
   * @deprecated use {@link MacroComboBoxWithBrowseButton}
   */
  @Deprecated
  protected JComponent createComponentWithMacroBrowse(@NotNull final TextFieldWithBrowseButton textAccessor) {
    final FixedSizeButton button = new FixedSizeButton(textAccessor);
    button.setIcon(AllIcons.Actions.ListFiles);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        List<String> macros = new ArrayList<>();
        ComboBoxModel<String> model = myWorkingDirectoryComboBox.getModel();
        for (int i = 0; i < model.getSize(); ++i) {
          macros.add(model.getElementAt(i));
        }
        JBPopupFactory.getInstance()
          .createPopupChooserBuilder(macros)
          .setItemChosenCallback((value) -> textAccessor.setText(value))
          .setMovable(false)
          .setResizable(false)
          .createPopup()
          .showUnderneathOf(button);
      }
    });

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(textAccessor, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);
    return panel;
  }

  protected void addComponents() {
    add(myProgramParametersComponent);
    add(myWorkingDirectoryComponent);
    add(myEnvVariablesComponent);
  }

  protected void copyDialogCaption(final LabeledComponent<RawCommandLineEditor> component) {
    final RawCommandLineEditor rawCommandLineEditor = component.getComponent();
    rawCommandLineEditor.setDialogCaption(component.getRawText());
    component.getLabel().setLabelFor(rawCommandLineEditor.getTextField());
  }

  public void setProgramParametersLabel(String textWithMnemonic) {
    myProgramParametersComponent.setText(textWithMnemonic);
    copyDialogCaption(myProgramParametersComponent);
  }

  public void setProgramParameters(String params) {
    myProgramParametersComponent.getComponent().setText(params);
  }

  public TextAccessor getWorkingDirectoryAccessor() {
    return myWorkingDirectoryComboBox;
  }

  public void addWorkingDirectoryListener(Consumer<? super String> onTextChange) {
    myWorkingDirectoryComboBox.addActionListener(event -> onTextChange.accept(myWorkingDirectoryComboBox.getText()));
  }

  public void setWorkingDirectory(String dir) {
    myWorkingDirectoryComboBox.setText(dir);
  }

  public void setModuleContext(Module moduleContext) {
    myModuleContext = moduleContext;
    myWorkingDirectoryComboBox.setModule(moduleContext);
  }

  public void setHasModuleMacro() {
    myWorkingDirectoryComboBox.showModuleMacroAlways();
  }

  public LabeledComponent<RawCommandLineEditor> getProgramParametersComponent() {
    return myProgramParametersComponent;
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  @Override
  public void setAnchor(JComponent anchor) {
    myAnchor = anchor;
    myProgramParametersComponent.setAnchor(anchor);
    myWorkingDirectoryComponent.setAnchor(anchor);
    myEnvVariablesComponent.setAnchor(anchor);
  }

  public void applyTo(@NotNull CommonProgramRunConfigurationParameters configuration) {
    configuration.setProgramParameters(fromTextField(myProgramParametersComponent.getComponent(), configuration));
    configuration.setWorkingDirectory(fromTextField(myWorkingDirectoryComboBox, configuration));

    configuration.setEnvs(myEnvVariablesComponent.getEnvs());
    configuration.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
  }

  @Nullable
  protected String fromTextField(@NotNull TextAccessor textAccessor, @NotNull CommonProgramRunConfigurationParameters configuration) {
    return textAccessor.getText();
  }

  public void reset(@NotNull CommonProgramRunConfigurationParameters configuration) {
    setProgramParameters(configuration.getProgramParameters());
    setWorkingDirectory(PathUtil.toSystemDependentName(configuration.getWorkingDirectory()));

    myEnvVariablesComponent.setEnvs(configuration.getEnvs());
    myEnvVariablesComponent.setPassParentEnvs(configuration.isPassParentEnvs());
  }
}
