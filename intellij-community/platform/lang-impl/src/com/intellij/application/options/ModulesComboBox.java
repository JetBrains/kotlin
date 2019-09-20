// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SortedComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Combobox which allows to select a loaded modules from a project.
 * If you need to show unloaded modules as well, use {@link ModuleDescriptionsComboBox} instead.
 *
 * @see ModuleDescriptionsComboBox
 *
 * @author nik
 */
public class ModulesComboBox extends ComboBox<Module> {
  private final SortedComboBoxModel<Module> myModel;
  private boolean myAllowEmptySelection;

  public ModulesComboBox() {
    this(new SortedComboBoxModel<>(ModulesAlphaComparator.INSTANCE));
  }

  private ModulesComboBox(final SortedComboBoxModel<Module> model) {
    super(model);
    myModel = model;
    setRenderer(new ModuleListCellRenderer());
    setSwingPopup(false);
  }

  public void allowEmptySelection(@NotNull String emptySelectionText) {
    myAllowEmptySelection = true;
    myModel.add(null);
    setRenderer(new ModuleListCellRenderer(emptySelectionText));
  }

  public void setModules(@NotNull Collection<? extends Module> modules) {
    myModel.setAll(modules);
    if (myAllowEmptySelection) {
      myModel.add(null);
    }
  }

  public void fillModules(@NotNull Project project) {
    fillModules(project, null);
  }

  public void fillModules(@NotNull Project project, final @Nullable ModuleType moduleType) {
    Module[] allModules = ModuleManager.getInstance(project).getModules();
    if (moduleType == null) {
      setModules(Arrays.asList(allModules));
    }
    else {
      List<Module> modules = new ArrayList<>();
      for (Module module : allModules) {
        if (moduleType.equals(ModuleType.get(module))) {
          modules.add(module);
        }
      }
      setModules(modules);
    }
  }

  public void setSelectedModule(@Nullable Module module) {
    myModel.setSelectedItem(module);
  }

  @Nullable
  public Module getSelectedModule() {
    return myModel.getSelectedItem();
  }
}
