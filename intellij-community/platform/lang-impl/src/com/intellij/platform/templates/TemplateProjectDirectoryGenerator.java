/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.platform.templates;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardInputField;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGeneratorBase;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TemplateProjectDirectoryGenerator<T> extends DirectoryProjectGeneratorBase<T> {
  private final LocalArchivedTemplate myTemplate;
  private final ModuleBuilder myModuleBuilder;

  public TemplateProjectDirectoryGenerator(@NotNull LocalArchivedTemplate template) {
    myTemplate = template;
    myModuleBuilder = myTemplate.createModuleBuilder();
  }

  @Nls
  @NotNull
  @Override
  public String getName() {
    return myTemplate.getName();
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return myTemplate.getIcon();
  }

  @Override
  public void generateProject(@NotNull Project newProject,
                              @NotNull VirtualFile baseDir,
                              @NotNull T settings,
                              @NotNull Module module) {
    throw new IllegalStateException("Usn't supposed to be invoked, use generateProject(String, String) instead.");
  }

  public void generateProject(String name, String path){
    try {
      myModuleBuilder.createProject(name, path);
    }
    finally {
      myModuleBuilder.cleanup();
    }
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    String message = "Invalid settings";
    for (WizardInputField field : myTemplate.getInputFields()) {
      try {
        if (field.validate()) {
          continue;
        }
      }
      catch (ConfigurationException e) {
        message = e.getMessage();
      }
      return new ValidationResult(message);
    }

    ValidationResult result = myTemplate.validate(baseDirPath);
    if(result != null){
      return result;
    }

    return ValidationResult.OK;
  }

  public void buildUI(@NotNull SettingsStep settingsStep){
    for (WizardInputField field : myTemplate.getInputFields()) {
      field.addToSettings(settingsStep);
    }

    if(myTemplate.getInputFields().isEmpty()){
      settingsStep.addSettingsComponent(new JLabel());
    }
  }
}
