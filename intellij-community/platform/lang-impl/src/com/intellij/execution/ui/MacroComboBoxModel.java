// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.ui;

import com.intellij.openapi.application.PathMacros;
import com.intellij.util.SmartList;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import javax.swing.*;
import java.util.List;

final class MacroComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {
  private boolean withModuleDir;
  private List<String> macros;
  private Object selected;

  public void useModuleDir(boolean withModuleDir) {
    if (this.withModuleDir == withModuleDir) return;
    this.withModuleDir = withModuleDir;
    if (macros == null) return;
    macros = createMacros();
    fireContentsChanged(this, -1, -1);
  }

  @Override
  public Object getSelectedItem() {
    return selected;
  }

  @Override
  public void setSelectedItem(Object item) {
    if (item == null ? selected == null : item.equals(selected)) return;
    selected = item;
    fireContentsChanged(this, -1, -1);
  }

  @Override
  public int getSize() {
    List<String> list = getMacros();
    return list.size();
  }

  @Override
  public String getElementAt(int index) {
    List<String> list = getMacros();
    return 0 <= index && index < list.size() ? list.get(index) : null;
  }

  public List<String> getMacros() {
    if (macros == null) macros = createMacros();
    return macros;
  }

  private List<String> createMacros() {
    List<String> list = new SmartList<>();
    for (String name : PathMacros.getInstance().getUserMacroNames()) {
      list.add("$" + name + "$");
    }
    if (withModuleDir) {
      list.add(PathMacroUtil.MODULE_WORKING_DIR);
    }
    return list;
  }
}
