package com.intellij.application.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
* @author yole
*/
public class ModuleListCellRenderer extends SimpleListCellRenderer<Module> {
  private final String myEmptySelectionText;

  public ModuleListCellRenderer() {
    this("[none]");
  }

  public ModuleListCellRenderer(@NotNull String emptySelectionText) {
    myEmptySelectionText = emptySelectionText;
  }

  @Override
  public void customize(@NotNull JList<? extends Module> list, Module value, int index, boolean selected, boolean hasFocus) {
    if (value == null) {
      setText(myEmptySelectionText);
    }
    else {
      setIcon(ModuleType.get(value).getIcon());
      setText(value.getName());
    }
  }
}
