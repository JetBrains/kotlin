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
package com.intellij.application.options;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.*;
import com.intellij.openapi.module.impl.LoadedModuleDescriptionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.SortedComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Combobox which may show not only regular loaded modules but also unloaded modules.
 * Use it instead of {@link ModulesComboBox} for configuration elements which may refer to unloaded modules.
 *
 * @see ModulesComboBox
 *
 * @author nik
 */
public final class ModuleDescriptionsComboBox extends ComboBox<ModuleDescription> {
  private final SortedComboBoxModel<ModuleDescription> myModel;
  private boolean myAllowEmptySelection;

  public ModuleDescriptionsComboBox() {
    myModel = new SortedComboBoxModel<>(Comparator.comparing(description -> description != null ? description.getName() : "",
                                                             String.CASE_INSENSITIVE_ORDER));
    setModel(myModel);
    setSwingPopup(false);
    setRenderer(new ModuleDescriptionListCellRenderer());
  }

  public void allowEmptySelection(@NotNull String emptySelectionText) {
    myAllowEmptySelection = true;
    myModel.add(null);
    setRenderer(new ModuleDescriptionListCellRenderer(emptySelectionText));
  }

  public void setModules(@NotNull Collection<? extends Module> modules) {
    myModel.clear();
    for (Module module : modules) {
      myModel.add(new LoadedModuleDescriptionImpl(module));
    }
    if (myAllowEmptySelection) {
      myModel.add(null);
    }
  }

  public void setAllModulesFromProject(@NotNull Project project) {
    setModules(Arrays.asList(ModuleManager.getInstance(project).getModules()));
  }

  public void setSelectedModule(@Nullable Module module) {
    myModel.setSelectedItem(module != null ? new LoadedModuleDescriptionImpl(module) : null);
  }

  public void setSelectedModule(@NotNull Project project, @NotNull String moduleName) {
    Module module = ModuleManager.getInstance(project).findModuleByName(moduleName);
    if (module != null) {
      setSelectedModule(module);
    }
    else {
      UnloadedModuleDescription description = ModuleManager.getInstance(project).getUnloadedModuleDescription(moduleName);
      if (description != null) {
        if (myModel.indexOf(description) < 0) {
          myModel.add(description);
        }
        myModel.setSelectedItem(description);
      }
      else {
        myModel.setSelectedItem(null);
      }
    }
  }

  @Nullable
  public Module getSelectedModule() {
    ModuleDescription selected = myModel.getSelectedItem();
    if (selected instanceof LoadedModuleDescription) {
      return ((LoadedModuleDescription)selected).getModule();
    }
    return null;
  }

  @Nullable
  public String getSelectedModuleName() {
    ModuleDescription selected = myModel.getSelectedItem();
    return selected != null ? selected.getName() : null;
  }

  private static class ModuleDescriptionListCellRenderer extends SimpleListCellRenderer<ModuleDescription> {
    private final String myEmptySelectionText;

    ModuleDescriptionListCellRenderer() {
      this("[none]");
    }

    ModuleDescriptionListCellRenderer(@NotNull String emptySelectionText) {
      myEmptySelectionText = emptySelectionText;
    }

    @Override
    public void customize(@NotNull JList<? extends ModuleDescription> list, ModuleDescription value, int index, boolean selected, boolean hasFocus) {
      setText(value == null ? myEmptySelectionText : value.getName());
      setIcon(value instanceof LoadedModuleDescription
              ? ModuleType.get(((LoadedModuleDescription)value).getModule()).getIcon()
              : value != null
                ? AllIcons.Modules.UnloadedModule : null);
    }
  }
}
