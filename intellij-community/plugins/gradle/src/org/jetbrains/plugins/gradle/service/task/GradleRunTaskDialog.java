// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.task;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectPathField;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.ui.EditorComboBoxRenderer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.StringComboboxEditor;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleArgumentsCompletionProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public class GradleRunTaskDialog extends DialogWrapper {

  private final Project myProject;
  @Nullable private final Collection<String> myHistory;

  private JPanel contentPane;

  private ExternalProjectPathField myProjectPathField;

  private JPanel commandLinePanel;
  private JLabel commandLineLabel;
  private JPanel projectPathFieldPanel;
  private ComboBox commandLineComboBox;
  private EditorTextField commandLineEditor;


  public GradleRunTaskDialog(@NotNull Project project) {
    this(project, null);
  }

  public GradleRunTaskDialog(@NotNull Project project, @Nullable Collection<String> history) {
    super(project, true);
    myProject = project;
    myHistory = history;

    setTitle("Run Gradle Task");
    setUpDialog();
    setModal(true);
    init();
  }

  private void setUpDialog() {
    JComponent commandLineComponent;
    if (myHistory == null) {
      commandLineEditor = new EditorTextField("", myProject, PlainTextFileType.INSTANCE);
      commandLineComponent = commandLineEditor;

      commandLineLabel.setLabelFor(commandLineEditor);
    }
    else {
      commandLineComboBox = new ComboBox(ArrayUtilRt.toStringArray(myHistory));
      commandLineComponent = commandLineComboBox;

      commandLineLabel.setLabelFor(commandLineComboBox);

      commandLineComboBox.setLightWeightPopupEnabled(false);

      EditorComboBoxEditor editor = new StringComboboxEditor(myProject, PlainTextFileType.INSTANCE, commandLineComboBox);
      commandLineComboBox.setRenderer(new EditorComboBoxRenderer(editor));

      commandLineComboBox.setEditable(true);
      commandLineComboBox.setEditor(editor);
      commandLineComboBox.setFocusable(true);

      commandLineEditor = editor.getEditorComponent();
    }

    commandLinePanel.add(commandLineComponent);

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
    FileChooserDescriptor projectPathChooserDescriptor = null;
    if (manager instanceof ExternalSystemUiAware) {
      projectPathChooserDescriptor = ((ExternalSystemUiAware)manager).getExternalProjectConfigDescriptor();
    }
    if (projectPathChooserDescriptor == null) {
      projectPathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
    }

    String title = ExternalSystemBundle.message("settings.label.select.project", GradleConstants.SYSTEM_ID.getReadableName());
    myProjectPathField = new ExternalProjectPathField(myProject, GradleConstants.SYSTEM_ID, projectPathChooserDescriptor, title) {
      @Override
      public Dimension getPreferredSize() {
        return commandLinePanel == null ? super.getPreferredSize() : commandLinePanel.getPreferredSize();
      }
    };

    projectPathFieldPanel.add(myProjectPathField);

    new GradleArgumentsCompletionProvider(myProject, myProjectPathField).apply(commandLineEditor);
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (myProjectPathField.getText().trim().isEmpty()) {
      return new ValidationInfo("Working directory is empty", myProjectPathField);
    }

    return null;
  }

  @NotNull
  public String getCommandLine() {
    if (commandLineComboBox != null) {
      return (String)commandLineComboBox.getEditor().getItem();
    }
    else {
      return commandLineEditor.getText();
    }
  }

  public void setCommandLine(@NotNull String fullCommandLine) {
    if (commandLineComboBox != null) {
      commandLineComboBox.setSelectedItem(fullCommandLine);
    }

    commandLineEditor.setText(fullCommandLine);
  }

  @NotNull
  public String getWorkDirectory() {
    return myProjectPathField.getText();
  }

  public void setWorkDirectory(@NotNull String path) {
    myProjectPathField.setText(path);
  }

  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return commandLineComboBox;
  }
}
