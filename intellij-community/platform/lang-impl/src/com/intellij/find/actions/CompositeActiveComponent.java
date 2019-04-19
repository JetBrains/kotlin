/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.find.actions;

import com.intellij.ui.ActiveComponent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class CompositeActiveComponent implements ActiveComponent {
  private final ActiveComponent[] myComponents;
  private final JPanel myComponent;

  public CompositeActiveComponent(@NotNull ActiveComponent... components) {
    myComponents = components;

    myComponent = new JPanel(new FlowLayout(FlowLayout.CENTER, JBUI.scale(2), JBUI.scale(2)));
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

  @Override
  public JComponent getComponent() {
    return myComponent;
  }
}
