/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.externalSystem.model.settings.LocationSettingType;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.gradle.initialization.BuildLayoutParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class IdeaGradleSystemSettingsControlBuilder implements GradleSystemSettingsControlBuilder {

  @NotNull
  private final GradleSettings myInitialSettings;

  // Used by reflection at showUi() and disposeUiResources()
  @SuppressWarnings("FieldCanBeLocal")
  @Nullable
  private JBLabel myServiceDirectoryLabel;
  @Nullable
  private TextFieldWithBrowseButton myServiceDirectoryPathField;
  private boolean myServiceDirectoryPathModifiedByUser;
  private boolean dropServiceDirectory;

  // Used by reflection at showUi() and disposeUiResources()
  @SuppressWarnings("FieldCanBeLocal")
  @Nullable
  private JBLabel myGradleVmOptionsLabel;
  @Nullable
  private JBTextField myGradleVmOptionsField;
  private boolean dropVmOptions;

  @Nullable
  private JBCheckBox myOfflineModeBox;
  private boolean dropOfflineModeBox;

  public IdeaGradleSystemSettingsControlBuilder(@NotNull GradleSettings initialSettings) {
    myInitialSettings = initialSettings;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    if (!dropOfflineModeBox) {
      myOfflineModeBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.offline_work"));
      canvas.add(myOfflineModeBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }
    addServiceDirectoryControl(canvas, indentLevel);

    if (!dropVmOptions) {
      myGradleVmOptionsLabel = new JBLabel(GradleBundle.message("gradle.settings.text.vm.options"));
      canvas.add(myGradleVmOptionsLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      myGradleVmOptionsField = new JBTextField();
      canvas.add(myGradleVmOptionsField, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  @Override
  public void reset() {
    if (myServiceDirectoryPathField != null) {
      myServiceDirectoryPathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      myServiceDirectoryPathField.setText("");
      String path = myInitialSettings.getServiceDirectoryPath();
      if (StringUtil.isEmpty(path)) {
        deduceServiceDirectory(myServiceDirectoryPathField);
        myServiceDirectoryPathModifiedByUser = false;
      }
      else {
        myServiceDirectoryPathField.setText(path);
      }
    }

    if (myGradleVmOptionsField != null) {
      myGradleVmOptionsField.setText(trimIfPossible(myInitialSettings.getGradleVmOptions()));
    }
    if (myOfflineModeBox != null) {
      myOfflineModeBox.setSelected(myInitialSettings.isOfflineWork());
    }
  }

  @Override
  public boolean isModified() {
    if (myServiceDirectoryPathModifiedByUser && myServiceDirectoryPathField != null &&
        !Comparing.equal(ExternalSystemApiUtil.normalizePath(myServiceDirectoryPathField.getText()),
                         ExternalSystemApiUtil.normalizePath(myInitialSettings.getServiceDirectoryPath()))) {
      return true;
    }

    if (myGradleVmOptionsField != null && !Comparing.equal(trimIfPossible(myGradleVmOptionsField.getText()), trimIfPossible(
      myInitialSettings.getGradleVmOptions()))) {
      return true;
    }

    if (myOfflineModeBox != null && myOfflineModeBox.isSelected() != myInitialSettings.isOfflineWork()) {
      return true;
    }

    return false;
  }

  @Override
  public void apply(@NotNull GradleSettings settings) {
    if (myServiceDirectoryPathField != null && myServiceDirectoryPathModifiedByUser) {
      settings.setServiceDirectoryPath(ExternalSystemApiUtil.normalizePath(myServiceDirectoryPathField.getText()));
    }
    if (myGradleVmOptionsField != null) {
      settings.setGradleVmOptions(trimIfPossible(myGradleVmOptionsField.getText()));
    }
    if (myOfflineModeBox != null) {
      settings.setOfflineWork(myOfflineModeBox.isSelected());
    }
  }

  @Override
  public boolean validate(@NotNull GradleSettings settings) {
    return true;
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @NotNull
  @Override
  public GradleSettings getInitialSettings() {
    return myInitialSettings;
  }

  public IdeaGradleSystemSettingsControlBuilder dropServiceDirectory() {
    dropServiceDirectory = true;
    return this;
  }

  public IdeaGradleSystemSettingsControlBuilder dropVmOptions() {
    dropVmOptions = true;
    return this;
  }

  public IdeaGradleSystemSettingsControlBuilder dropOfflineModeBox() {
    dropOfflineModeBox = true;
    return this;
  }


  private void addServiceDirectoryControl(PaintAwarePanel canvas, int indentLevel) {
    if (dropServiceDirectory) return;

    myServiceDirectoryLabel = new JBLabel(GradleBundle.message("gradle.settings.text.service.dir.path"));
    myServiceDirectoryPathField = new TextFieldWithBrowseButton();
    myServiceDirectoryPathField.addBrowseFolderListener("", GradleBundle.message("gradle.settings.title.service.dir.path"), null,
                                                        new FileChooserDescriptor(false, true, false, false, false, false),
                                                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    myServiceDirectoryPathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        myServiceDirectoryPathModifiedByUser = true;
        myServiceDirectoryPathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        myServiceDirectoryPathModifiedByUser = true;
        myServiceDirectoryPathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
      }
    });

    canvas.add(myServiceDirectoryLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    canvas.add(myServiceDirectoryPathField, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
  }

  private static void deduceServiceDirectory(@NotNull TextFieldWithBrowseButton serviceDirectoryPathField) {
    File gradleUserHomeDir = new BuildLayoutParameters().getGradleUserHomeDir();
    serviceDirectoryPathField.setText(FileUtil.toSystemIndependentName(gradleUserHomeDir.getPath()));
    serviceDirectoryPathField.getTextField().setForeground(LocationSettingType.DEDUCED.getColor());
  }

  @Nullable
  private static String trimIfPossible(@Nullable String s) {
    return StringUtil.nullize(StringUtil.trim(s));
  }
}
