// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class RunAnythingIconHandler implements PropertyChangeListener {
  private static final String FOREGROUND_PROPERTY = "foreground";
  protected static final String MATCHED_PROVIDER_PROPERTY = "JTextField.match";

  private final Consumer<? super ExtendableTextComponent.Extension> myConsumer;
  private final JTextComponent myComponent;

  RunAnythingIconHandler(@NotNull Consumer<? super ExtendableTextComponent.Extension> consumer, @NotNull JTextComponent component) {
    myConsumer = consumer;
    myComponent = component;

    setConfigurationIcon(component.getClientProperty(MATCHED_PROVIDER_PROPERTY));
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (myComponent == null) return;

    if (MATCHED_PROVIDER_PROPERTY.equals(event.getPropertyName())) {
      setConfigurationIcon(event.getNewValue());
    }
    else if (FOREGROUND_PROPERTY.equals(event.getPropertyName())) {
      myComponent.setForeground(UIUtil.getTextFieldForeground());
    }

    myComponent.repaint();
  }

  private void setConfigurationIcon(Object variant) {
    if (!(variant instanceof Icon)) return;

    myConsumer.consume(new RunConfigurationTypeExtension((Icon)variant));
  }

  private static class RunConfigurationTypeExtension implements ExtendableTextComponent.Extension {
    private final Icon myVariant;

    RunConfigurationTypeExtension(Icon variant) {
      myVariant = variant;
    }

    @Override
    public Icon getIcon(boolean hovered) {
      return myVariant;
    }

    @Override
    public boolean isIconBeforeText() {
      return true;
    }

    @Override
    public String toString() {
      return MATCHED_PROVIDER_PROPERTY;
    }
  }
}


