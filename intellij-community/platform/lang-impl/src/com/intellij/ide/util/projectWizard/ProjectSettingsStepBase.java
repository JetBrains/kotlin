// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.BundleBase;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.ProjectGeneratorPeer;
import com.intellij.platform.templates.TemplateProjectDirectoryGenerator;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame.BOTTOM_PANEL;

@SuppressWarnings("ComponentNotRegistered")
public class ProjectSettingsStepBase<T> extends AbstractActionWithPanel implements DumbAware, Disposable {
  protected DirectoryProjectGenerator<T> myProjectGenerator;
  protected AbstractNewProjectStep.AbstractCallback<T> myCallback;
  protected TextFieldWithBrowseButton myLocationField;
  protected File myProjectDirectory;
  protected JButton myCreateButton;
  protected JLabel myErrorLabel;
  protected NotNullLazyValue<ProjectGeneratorPeer<T>> myLazyGeneratorPeer;

  public ProjectSettingsStepBase(DirectoryProjectGenerator<T> projectGenerator,
                                 AbstractNewProjectStep.AbstractCallback<T> callback) {
    super();
    getTemplatePresentation().setIcon(projectGenerator.getLogo());
    getTemplatePresentation().setText(projectGenerator.getName());
    myProjectGenerator = projectGenerator;
    myCallback = callback;
    myProjectDirectory = findSequentNonExistingUntitled();
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
  }

  @Override
  public void onPanelSelected() {
    checkWebProjectValid();
  }

  @NotNull
  @Override
  public JButton getActionButton() {
    return myCreateButton;
  }

  @NotNull
  protected NotNullLazyValue<ProjectGeneratorPeer<T>> createLazyPeer() {
    return myProjectGenerator.createLazyPeer();
  }

  @Override
  public JPanel createPanel() {
    myLazyGeneratorPeer = createLazyPeer();
    final JPanel mainPanel = new JPanel(new BorderLayout());

    final JLabel label = createErrorLabel();
    final JButton button = createActionButton();
    button.addActionListener(createCloseActionListener());
    Disposer.register(this, () -> UIUtil.dispose(button));
    final JPanel scrollPanel = createAndFillContentPanel();
    initGeneratorListeners();
    registerValidators();
    final JBScrollPane scrollPane = new JBScrollPane(scrollPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(null);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    final JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.setName(BOTTOM_PANEL);

    bottomPanel.add(label, BorderLayout.NORTH);
    bottomPanel.add(button, BorderLayout.EAST);
    mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    return mainPanel;
  }

  protected final JLabel createErrorLabel() {
    JLabel errorLabel = new JLabel("");
    errorLabel.setForeground(JBColor.RED);

    myErrorLabel = errorLabel;

    return errorLabel;
  }

  protected final JButton createActionButton() {
    JButton button = new JButton("Create");
    button.putClientProperty(DialogWrapper.DEFAULT_ACTION, Boolean.TRUE);

    myCreateButton = button;
    return button;
  }

  @NotNull
  protected final ActionListener createCloseActionListener() {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean isValid = checkValid();
        if (isValid && myCallback != null) {
          final DialogWrapper dialog = DialogWrapper.findInstance(myCreateButton);
          if (dialog != null) {
            dialog.close(DialogWrapper.OK_EXIT_CODE);
          }
          TransactionGuard.getInstance().submitTransactionAndWait(() -> myCallback.consume(ProjectSettingsStepBase.this, getPeer()));
        }
      }
    };
  }

  protected ProjectGeneratorPeer<T> getPeer() {
    return myLazyGeneratorPeer.getValue();
  }

  protected final JPanel createContentPanelWithAdvancedSettingsPanel() {
    final JPanel basePanel = createBasePanel();
    final JPanel scrollPanel = new JPanel(new BorderLayout());
    scrollPanel.add(basePanel, BorderLayout.NORTH);
    final JPanel advancedSettings = createAdvancedSettings();
    if (advancedSettings != null) {
      scrollPanel.add(advancedSettings, BorderLayout.CENTER);
    }
    return scrollPanel;
  }

  protected void initGeneratorListeners() {
    if (myProjectGenerator instanceof WebProjectTemplate) {
      getPeer().addSettingsListener(new ProjectGeneratorPeer.SettingsListener() {
        @Override
        public void stateChanged(boolean validSettings) {
          checkValid();
        }
      });
    }
  }

  protected final Icon getIcon() {
    return myProjectGenerator.getLogo();
  }

  protected JPanel createBasePanel() {
    final JPanel panel = new JPanel(new VerticalFlowLayout(0, 2));
    final LabeledComponent<TextFieldWithBrowseButton> component = createLocationComponent();
    panel.add(component);
    return panel;
  }

  protected void registerValidators() {
    final DocumentAdapter documentAdapter = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        checkValid();
      }
    };
    myLocationField.getTextField().getDocument().addDocumentListener(documentAdapter);
    Disposer.register(this, () -> myLocationField.getTextField().getDocument().removeDocumentListener(documentAdapter));
    checkWebProjectValid();
  }

  private void checkWebProjectValid() {
    if (myProjectGenerator instanceof WebProjectTemplate && !((WebProjectTemplate)myProjectGenerator).postponeValidation()) {
      checkValid();
    }
  }

  public boolean checkValid() {
    if (myLocationField == null) return true;
    final String projectName = myLocationField.getText();

    if (projectName.trim().isEmpty()) {
      setErrorText("Project name can't be empty");
      return false;
    }
    final String text = myLocationField.getText().trim();
    if (text.indexOf('$') >= 0) {
      setErrorText("Project directory name must not contain the $ character");
      return false;
    }
    try {
      Paths.get(text);
    } catch (InvalidPathException e) {
      setErrorText("Invalid project directory path");
      return false;
    }
    if (myProjectGenerator != null) {
      final String baseDirPath = myLocationField.getTextField().getText();
      ValidationResult validationResult = myProjectGenerator.validate(baseDirPath);
      final ValidationInfo peerValidationResult = getPeer().validate();
      if (!validationResult.isOk()) {
        setErrorText(validationResult.getErrorMessage());
        return false;
      } else if (peerValidationResult != null) {
        setErrorText(peerValidationResult.message);
        return false;
      }
      if (myProjectGenerator instanceof WebProjectTemplate) {
        final ProjectGeneratorPeer<T> peer = getPeer();
        final ValidationInfo validationInfo = peer.validate();
        if (validationInfo != null) {
          setErrorText(validationInfo.message);
          return false;
        }
      }
    }

    setErrorText(null);
    return true;
  }

  protected JPanel createAndFillContentPanel() {
    WebProjectSettingsStepWrapper settingsStep = new WebProjectSettingsStepWrapper();
    if (myProjectGenerator instanceof WebProjectTemplate) {
      getPeer().buildUI(settingsStep);
    }
    else if (myProjectGenerator instanceof TemplateProjectDirectoryGenerator) {
      ((TemplateProjectDirectoryGenerator)myProjectGenerator).buildUI(settingsStep);
    }
    else {
      return createContentPanelWithAdvancedSettingsPanel();
    }

    //back compatibility: some plugins can implement only GeneratorPeer#getComponent() method
    if (settingsStep.isEmpty()) return createContentPanelWithAdvancedSettingsPanel();

    final JPanel jPanel = new JPanel(new VerticalFlowLayout(0, 5));
    List<LabeledComponent> labeledComponentList = new ArrayList<>();
    labeledComponentList.add(createLocationComponent());
    labeledComponentList.addAll(settingsStep.getFields());

    final JPanel scrollPanel = new JPanel(new BorderLayout());
    scrollPanel.add(jPanel, BorderLayout.NORTH);

    for (LabeledComponent component : labeledComponentList) {
      component.setLabelLocation(BorderLayout.WEST);
      jPanel.add(component);
    }

    for (JComponent component : settingsStep.getComponents()) {
      jPanel.add(component);
    }

    UIUtil.mergeComponentsWithAnchor(labeledComponentList);

    return scrollPanel;
  }

  public void setErrorText(@Nullable String text) {
    myErrorLabel.setText(text);
    myErrorLabel.setForeground(MessageType.ERROR.getTitleForeground());
    myErrorLabel.setIcon(StringUtil.isEmpty(text) ? null : AllIcons.Actions.Lightning);
    myCreateButton.setEnabled(text == null);
  }

  public void setWarningText(@Nullable String text) {
    myErrorLabel.setText("<html>Note: " + text + "  </html>");
    myErrorLabel.setForeground(MessageType.WARNING.getTitleForeground());
  }

  @Nullable
  protected JPanel createAdvancedSettings() {
    final JPanel jPanel = new JPanel(new VerticalFlowLayout(0, 5));
    jPanel.add(getPeer().getComponent(myLocationField, () -> checkValid()));
    return jPanel;
  }

  public DirectoryProjectGenerator<T> getProjectGenerator() {
    return myProjectGenerator;
  }

  public final String getProjectLocation() {
    return FileUtil.expandUserHome(FileUtil.toSystemIndependentName(myLocationField.getText()));
  }

  public final void setLocation(@NotNull final String location) {
    myLocationField.setText(FileUtil.getLocationRelativeToUserHome(FileUtil.toSystemDependentName(location)));
  }

  protected final LabeledComponent<TextFieldWithBrowseButton> createLocationComponent() {
    myLocationField = new TextFieldWithBrowseButton();
    myProjectDirectory = findSequentNonExistingUntitled();
    final String projectLocation = myProjectDirectory.toString();
    myLocationField.setText(projectLocation);
    final int index = projectLocation.lastIndexOf(File.separator);
    if (index > 0) {
      JTextField textField = myLocationField.getTextField();
      textField.select(index + 1, projectLocation.length());
      textField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true);
    }

    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myLocationField.addBrowseFolderListener("Select Base Directory", "Select base directory for the project", null, descriptor);
    return LabeledComponent.create(myLocationField, BundleBase.replaceMnemonicAmpersand("&Location"), BorderLayout.WEST);
  }

  @NotNull
  protected File findSequentNonExistingUntitled() {
    return FileUtil.findSequentNonexistentFile(new File(ProjectUtil.getBaseDir()), "untitled", "");
  }

  @Override
  public void dispose() {}
}
