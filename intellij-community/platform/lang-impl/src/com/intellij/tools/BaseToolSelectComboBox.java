// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.SeparatorWithText;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public abstract class BaseToolSelectComboBox<T extends Tool> extends ComboboxWithBrowseButton {
  public static final Object NONE_TOOL = ObjectUtils.sentinel("NONE_TOOL");

  public BaseToolSelectComboBox() {
    final JComboBox comboBox = getComboBox();

    //noinspection unchecked
    comboBox.setModel(new CollectionComboBoxModel(getComboBoxElements(), null) {
      @Override
      public void setSelectedItem(@Nullable Object item) {
        if (item instanceof ToolsGroup) {
          return;
        }
        super.setSelectedItem(item);
      }
    });


    comboBox.setRenderer(new ColoredListCellRenderer<Object>() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof ToolsGroup) {
          SeparatorWithText separator = new SeparatorWithText();
          separator.setCaption(StringUtil.notNullize(((ToolsGroup)value).getName(), ToolsBundle.message("tools.unnamed.group")));
          separator.setCaptionCentered(false);
          return separator;
        } else {
          return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
        }
      }

      @Override
      protected void customizeCellRenderer(@NotNull JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof ToolsGroup) {
          // do nothing - see getListCellRendererComponent()
        }
        else if (value instanceof Tool) {
          append(StringUtil.notNullize(((Tool)value).getName()), ((Tool)value).isEnabled() ? REGULAR_ATTRIBUTES : GRAYED_ATTRIBUTES);
        }
        else {
          append(ToolsBundle.message("tools.list.item.none"));
        }
      }
    });

    getButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Object item = comboBox.getSelectedItem();
        String id = null;
        if (item instanceof Tool) {
          id = ((Tool)item).getActionId();
        }
        final ToolSelectDialog dialog = getToolSelectDialog(id);
        if (!dialog.showAndGet()) {
          return;
        }

        comboBox.setModel(new CollectionComboBoxModel(getComboBoxElements(), dialog.getSelectedTool()));
      }
    });
  }

  @NotNull
  protected abstract BaseToolManager<T> getToolManager();

  @NotNull
  protected abstract ToolSelectDialog getToolSelectDialog(@Nullable String toolIdToSelect);

  @NotNull
  protected List<Object> getComboBoxElements() {
    List<Object> result = new SmartList<>();
    BaseToolManager<T> manager = getToolManager();
    result.add(NONE_TOOL);//for empty selection
    for (ToolsGroup<T> group : manager.getGroups()) {
      result.add(group);
      result.addAll(manager.getTools(group.getName()));
    }

    return result;
  }

  public int getValuableItemCount() {
    final JComboBox comboBox = getComboBox();
    int itemCount = comboBox.getItemCount();
    if (itemCount == 0) {
      return 0;
    }

    int valuableCount = 0;
    for (int i = 0; i < itemCount; i++) {
      if (comboBox.getItemAt(i) != NONE_TOOL) {
        valuableCount++;
      }
    }
    return valuableCount;
  }

  @Nullable
  public Tool selectTool(@Nullable String toolId) {
    JComboBox comboBox = getComboBox();
    if (toolId == null) {
      comboBox.setSelectedIndex(-1);
      return null;
    }

    for (int i = 0; i < comboBox.getItemCount(); i++) {
      final Object itemAt = comboBox.getItemAt(i);
      if (itemAt instanceof Tool && toolId.equals(((Tool)itemAt).getActionId())) {
        comboBox.setSelectedIndex(i);
        return (Tool)itemAt;
      }
    }

    return null;
  }

  @Nullable
  public Tool getSelectedTool() {
    Object item = getComboBox().getSelectedItem();
    return item instanceof Tool ? (Tool)item : null;
  }
}
