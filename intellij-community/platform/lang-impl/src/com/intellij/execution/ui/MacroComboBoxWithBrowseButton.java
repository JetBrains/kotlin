/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.execution.ui;

import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.BrowseFolderRunnable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.ui.TextAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class MacroComboBoxWithBrowseButton extends ComboBox<String> implements TextAccessor {
  private Module module;
  private boolean always;

  public MacroComboBoxWithBrowseButton(FileChooserDescriptor descriptor, Project project) {
    super(new MacroComboBoxModel());
    descriptor.withShowHiddenFiles(true);

    Runnable action = new BrowseFolderRunnable<ComboBox<String>>(null, null, project, descriptor, this, accessor) {
      private Module getModule() {
        if (module == null) module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE_CONTEXT);
        if (module == null) module = myFileChooserDescriptor.getUserData(LangDataKeys.MODULE);
        return module;
      }

      @Nullable
      @Override
      protected Project getProject() {
        Project project = super.getProject();
        if (project != null) return project;
        Module module = getModule();
        return module == null ? null : module.getProject();
      }

      @NotNull
      @Override
      protected String expandPath(@NotNull String path) {
        Project project = getProject();
        if (project != null) path = PathMacroManager.getInstance(project).expandPath(path);

        Module module = getModule();
        if (module != null) path = PathMacroManager.getInstance(module).expandPath(path);

        return super.expandPath(path);
      }
    };

    initBrowsableEditor(action, project);

    Component component = editor.getEditorComponent();
    if (component instanceof JTextField) {
      FileChooserFactory.getInstance().installFileCompletion((JTextField)component, descriptor, true, null);
    }
  }

  @Override
  public String getText() {
    return accessor.getText(this);
  }

  @Override
  public void setText(String text) {
    accessor.setText(this, text != null ? text : "");
  }

  public void setModule(Module module) {
    this.module = module;
    configure();
  }

  public void showModuleMacroAlways() {
    always = true;
    configure();
  }

  private void configure() {
    MacroComboBoxModel model = (MacroComboBoxModel)getModel();
    if (model != null) model.useModuleDir(always || module != null);
  }

  private final TextComponentAccessor<ComboBox<String>> accessor = new TextComponentAccessor<ComboBox<String>>() {
    @Override
    public String getText(ComboBox<String> component) {
      Object item = component == null ? null : component.getSelectedItem();
      return item == null ? "" : item.toString();
    }

    @Override
    public void setText(ComboBox<String> component, @NotNull String text) {
      if (component != null) component.setSelectedItem(text);
    }
  };
}
