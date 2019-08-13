// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.util.ui.ItemRemovable;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ModuleOptionsTableModel extends AbstractTableModel implements ItemRemovable {
  private static class Item {
    private final Module module;
    private String option;

    Item(Module module) {
      this(module, "");
    }

    private Item(Module module, String option) {
      this.module = module;
      this.option = option;
    }
  }

  private final List<Item> myItems = new ArrayList<>();

  @NotNull
  public Map<String, String> getModuleOptions() {
    Map<String, String> map = new HashMap<>();
    for (Item item : myItems) {
      map.put(item.module.getName(), item.option);
    }
    return map;
  }

  public void setModuleOptions(@NotNull Project project, @NotNull Map<String, String> moduleOptions) {
    myItems.clear();
    if (!moduleOptions.isEmpty()) {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        String options = moduleOptions.get(module.getName());
        if (options != null) {
          myItems.add(new Item(module, options));
        }
      }
      sortItems();
    }
    fireTableDataChanged();
  }

  public int addModulesToModel(@NotNull Project project, @NotNull Component parent) {
    Set<Module> existing = new HashSet<>();
    for (Item item : myItems) {
      existing.add(item.module);
    }

    List<Module> candidates = new ArrayList<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (!existing.contains(module)) {
        candidates.add(module);
      }
    }

    if (!candidates.isEmpty()) {
      Collections.sort(candidates, Comparator.comparing(Module::getName));
      ChooseModulesDialog chooser = new ChooseModulesDialog(parent, candidates, "Choose module");
      chooser.show();
      List<Module> chosen = chooser.getChosenElements();

      if (!chosen.isEmpty()) {
        for (Module module : chosen) {
          myItems.add(new Item(module));
        }
        sortItems();
        fireTableDataChanged();

        for (int i = 0; i < myItems.size(); i++) {
          if (myItems.get(i).module.equals(chosen.get(0))) {
            return i;
          }
        }
      }
    }

    return -1;
  }

  private void sortItems() {
    Collections.sort(myItems, Comparator.comparing(o -> o.module.getName()));
  }

  @Override
  public int getRowCount() {
    return myItems.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex != 0;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Item item = myItems.get(rowIndex);
    return columnIndex == 0 ? item.module : item.option;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    Item item = myItems.get(rowIndex);
    item.option = ((String)aValue).trim();
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  @Override
  public void removeRow(int idx) {
    myItems.remove(idx);
    fireTableRowsDeleted(idx, idx);
  }
}