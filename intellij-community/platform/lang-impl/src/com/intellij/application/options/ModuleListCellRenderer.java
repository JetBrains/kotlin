package com.intellij.application.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
* @author yole
*/
public class ModuleListCellRenderer extends ListCellRendererWrapper<Module> {
  private final String myEmptySelectionText;

  public ModuleListCellRenderer() {
    this("[none]");
  }

  public ModuleListCellRenderer(@NotNull String emptySelectionText) {
    myEmptySelectionText = emptySelectionText;
  }

  @Override
  public void customize(JList list, Module module, int index, boolean selected, boolean hasFocus) {
    if (module == null) {
      setText(myEmptySelectionText);
    }
    else {
      setIcon(ModuleType.get(module).getIcon());
      setText(module.getName());
    }
  }
}
