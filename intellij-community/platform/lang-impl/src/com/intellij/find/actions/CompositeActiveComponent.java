// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.ui.ActiveComponent;
import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CompositeActiveComponent implements ActiveComponent {
  private final ActiveComponent[] myComponents;
  private final JPanel myComponent;

  public CompositeActiveComponent(@NotNull ActiveComponent... components) {
    myComponents = components;

    myComponent = new JPanel(new FlowLayout(FlowLayout.CENTER, JBUIScale.scale(2), JBUIScale.scale(2)));
    myComponent.setBorder(null);
    myComponent.setOpaque(false);
    for (ActiveComponent component : components) {
      myComponent.add(component.getComponent());
    }
  }

  @Override
  public void setActive(boolean active) {
    for (ActiveComponent component : myComponents) {
      component.setActive(active);
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }
}
