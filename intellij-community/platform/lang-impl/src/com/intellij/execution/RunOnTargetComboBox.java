// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.remote.RemoteTargetConfiguration;
import com.intellij.execution.remote.RemoteTargetConfigurationKt;
import com.intellij.execution.remote.RemoteTargetType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SeparatorWithText;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RunOnTargetComboBox extends ComboBox<RunOnTargetComboBox.Item> {
  public RunOnTargetComboBox() {
    super(new MyModel());
    setRenderer(new MyRenderer());
  }

  public void initModel() {
    MyModel model = (MyModel)getModel();
    model.removeAllElements();
    model.addElement(null);
    model.addElement(new Item("New Targets", null, true, false));
    for (RemoteTargetType<?> type : RemoteTargetType.Companion.getEXTENSION_NAME().getExtensionList()) {
      model.addElement(new Item(type.getDisplayName(), type.getIcon(), false, true));
    }
  }

  public void addTarget(@NotNull RemoteTargetConfiguration config, int index) {
    Icon icon = RemoteTargetConfigurationKt.getTargetType(config).getIcon();
    ((MyModel)getModel()).insertElementAt(new Item(config.getDisplayName(), icon, false, false), index);
  }

  @Nullable
  public String getSelectedTargetName() {
    return ObjectUtils.doIfCast(getSelectedItem(), Item.class, i -> i.getTargetName());
  }

  public void addTargets(List<RemoteTargetConfiguration> configs) {
    int index = 1;
    for (RemoteTargetConfiguration config : configs) {
      addTarget(config, index);
      index++;
    }
  }

  public void selectTarget(String configName) {
    if (configName == null) {
      setSelectedItem(null);
      return;
    }
    for (int i = 0; i < getModel().getSize(); i++) {
      Item at = getModel().getElementAt(i);
      if (at != null && !at.separator && !at.type && configName.equals(at.getTargetName())) {
        setSelectedItem(at);
      }
    }
    //todo[remoteServers]: add invalid value
  }

  public static class Item {
    private final String displayName;
    private final Icon icon;
    private final boolean separator;
    private final boolean type;

    private Item(String displayName, Icon icon, boolean separator, boolean type) {
      this.displayName = displayName;
      this.icon = icon;
      this.separator = separator;
      this.type = type;
    }

    public String getTargetName() {
      return separator || type ? null : displayName;
    }
  }

  private static class MyModel extends DefaultComboBoxModel<RunOnTargetComboBox.Item> {
    @Override
    public void setSelectedItem(Object anObject) {
      if (anObject instanceof Item) {
        if (((Item)anObject).separator) {
          return;
        }
        if (((Item)anObject).type) {
          //todo[remoteServers]: invoke wizard
          return;
        }
      }
      super.setSelectedItem(anObject);
    }
  }

  private static class MyRenderer extends ColoredListCellRenderer<RunOnTargetComboBox.Item> {
    @Override
    public Component getListCellRendererComponent(JList<? extends Item> list, Item value, int index, boolean selected, boolean hasFocus) {
      if (value != null && value.separator) {
        SeparatorWithText separator = new SeparatorWithText();
        separator.setCaption(value.displayName);
        separator.setCaptionCentered(false);
        setFont(getFont().deriveFont(Font.PLAIN));
        return separator;
      }
      return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends Item> list, Item value, int index, boolean selected, boolean hasFocus) {
      if (value == null) {
        append("Local machine");
        setIcon(AllIcons.Nodes.HomeFolder);
      }
      else {
        if (!value.separator) {
          append(value.displayName);
          setIcon(value.icon);
        }
      }
    }
  }
}

