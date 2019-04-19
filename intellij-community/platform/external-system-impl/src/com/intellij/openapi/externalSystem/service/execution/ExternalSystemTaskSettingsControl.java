// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectPathField;
import com.intellij.openapi.externalSystem.util.*;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.GridBag;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.normalizePath;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTaskSettingsControl implements ExternalSystemSettingsControl<ExternalSystemTaskExecutionSettings> {

  @NotNull private final ProjectSystemId myExternalSystemId;
  @NotNull private final Project myProject;

  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myProjectPathLabel;
  private ExternalProjectPathField myProjectPathField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myTasksLabel;
  private EditorTextField myTasksTextField;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myVmOptionsLabel;
  private RawCommandLineEditor myVmOptionsEditor;
  @SuppressWarnings("FieldCanBeLocal") // Used via reflection at showUi() and disposeResources()
  private JBLabel myArgumentsLabel;
  private RawCommandLineEditor myArgumentsEditor;
  private EnvironmentVariablesComponent myEnvVariablesComponent;

  @Nullable private ExternalSystemTaskExecutionSettings myOriginalSettings;

  public ExternalSystemTaskSettingsControl(@NotNull Project project, @NotNull ProjectSystemId externalSystemId) {
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  public void setOriginalSettings(@Nullable ExternalSystemTaskExecutionSettings originalSettings) {
    myOriginalSettings = originalSettings;
  }

  @Override
  public void fillUi(@NotNull final PaintAwarePanel canvas, int indentLevel) {
    myProjectPathLabel = new JBLabel(ExternalSystemBundle.message(
      "run.configuration.settings.label.project", myExternalSystemId.getReadableName()
    ));
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    FileChooserDescriptor projectPathChooserDescriptor = null;
    if (manager instanceof ExternalSystemUiAware) {
      projectPathChooserDescriptor = ((ExternalSystemUiAware)manager).getExternalProjectConfigDescriptor();
    }
    if (projectPathChooserDescriptor == null) {
      projectPathChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
    }
    String title = ExternalSystemBundle.message("settings.label.select.project", myExternalSystemId.getReadableName());
    myProjectPathField = new ExternalProjectPathField(myProject, myExternalSystemId, projectPathChooserDescriptor, title) {
      @Override
      public Dimension getPreferredSize() {
        return myVmOptionsEditor == null ? super.getPreferredSize() : myVmOptionsEditor.getTextField().getPreferredSize();
      }
    };
    canvas.add(myProjectPathLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myProjectPathField, ExternalSystemUiUtil.getFillLineConstraints(0));

    myTasksLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.tasks"));
    myTasksTextField = new EditorTextField("", myProject, PlainTextFileType.INSTANCE);
    canvas.add(myTasksLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    GridBag c = ExternalSystemUiUtil.getFillLineConstraints(0);
    c.insets.right = myProjectPathField.getButton().getPreferredSize().width + 8 /* street magic, sorry */;
    canvas.add(myTasksTextField, c);

    new TaskCompletionProvider(myProject, myExternalSystemId, myProjectPathField).apply(myTasksTextField);

    myVmOptionsLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.vmoptions"));
    myVmOptionsEditor = new RawCommandLineEditor();
    myVmOptionsEditor.setDialogCaption(ExternalSystemBundle.message("run.configuration.settings.label.vmoptions"));
    canvas.add(myVmOptionsLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myVmOptionsEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
    myArgumentsLabel = new JBLabel(ExternalSystemBundle.message("run.configuration.settings.label.arguments"));
    myArgumentsEditor = new RawCommandLineEditor();
    myArgumentsEditor.setDialogCaption(ExternalSystemBundle.message("run.configuration.settings.label.arguments"));
    canvas.add(myArgumentsLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myArgumentsEditor, ExternalSystemUiUtil.getFillLineConstraints(0));
    myEnvVariablesComponent = new EnvironmentVariablesComponent();
    JBLabel myEnvVariablesComponentLabel = myEnvVariablesComponent.getLabel();
    myEnvVariablesComponentLabel.remove(myEnvVariablesComponentLabel);
    canvas.add(myEnvVariablesComponentLabel, ExternalSystemUiUtil.getLabelConstraints(0));
    canvas.add(myEnvVariablesComponent, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
  public void reset() {
    myProjectPathField.setText("");
    myTasksTextField.setText("");
    myVmOptionsEditor.setText("");
    myArgumentsEditor.setText("");
    myEnvVariablesComponent.setEnvData(EnvironmentVariablesData.DEFAULT);
    showUi(true);

    if (myOriginalSettings == null) {
      return;
    }

    String path = myOriginalSettings.getExternalProjectPath();
    if (StringUtil.isEmpty(path)) {
      path = "";
    }
    myProjectPathField.setText(path);
    myTasksTextField.setText(StringUtil.join(myOriginalSettings.getTaskNames(), " "));
    myVmOptionsEditor.setText(myOriginalSettings.getVmOptions());
    myArgumentsEditor.setText(myOriginalSettings.getScriptParameters());
    myEnvVariablesComponent.setEnvData(EnvironmentVariablesData.create(myOriginalSettings.getEnv(), myOriginalSettings.isPassParentEnvs()));
  }

  @Override
  public boolean isModified() {
    if (myOriginalSettings == null) {
      return false;
    }

    return !Comparing.equal(normalizePath(myProjectPathField.getText()),
                            normalizePath(myOriginalSettings.getExternalProjectPath()))
           || !Comparing.equal(normalizePath(myTasksTextField.getText()),
                               normalizePath(StringUtil.join(myOriginalSettings.getTaskNames(), " ")))
           || !Comparing.equal(normalizePath(myVmOptionsEditor.getText()),
                               normalizePath(myOriginalSettings.getVmOptions()))
           || !Comparing.equal(normalizePath(myArgumentsEditor.getText()),
                               normalizePath(myOriginalSettings.getScriptParameters()))
           || myEnvVariablesComponent.isPassParentEnvs() != myOriginalSettings.isPassParentEnvs()
           || !myEnvVariablesComponent.getEnvs().equals(myOriginalSettings.getEnv());

  }

  @Override
  public void apply(@NotNull ExternalSystemTaskExecutionSettings settings) {
    String projectPath = myProjectPathField.getText();
    settings.setExternalProjectPath(projectPath);
    settings.setTaskNames(StringUtil.split(myTasksTextField.getText(), " "));
    settings.setVmOptions(myVmOptionsEditor.getText());
    settings.setScriptParameters(myArgumentsEditor.getText());
    settings.setPassParentEnvs(myEnvVariablesComponent.isPassParentEnvs());
    settings.setEnv(myEnvVariablesComponent.getEnvs().isEmpty() ? Collections.emptyMap() : new THashMap<>(myEnvVariablesComponent.getEnvs()));
  }

  @Override
  public boolean validate(@NotNull ExternalSystemTaskExecutionSettings settings) throws ConfigurationException {
    String projectPath = myProjectPathField.getText();
    if (myOriginalSettings == null) {
      throw new ConfigurationException(String.format(
        "Can't store external task settings into run configuration. Reason: target run configuration is undefined. Tasks: '%s', " +
        "external project: '%s', vm options: '%s', arguments: '%s'",
        myTasksTextField.getText(), projectPath, myVmOptionsEditor.getText(), myArgumentsEditor.getText()
      ));
    }
    return true;
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }
}
