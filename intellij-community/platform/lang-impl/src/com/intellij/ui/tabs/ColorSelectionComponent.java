// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs;

import com.intellij.ui.ColorChooser;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.FileColorManager;
import com.intellij.ui.JBColor;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author gregsh
 */
public class ColorSelectionComponent extends JPanel {
  private static final String CUSTOM_COLOR_NAME = "Custom";
  private final Map<String, ColorButton> myColorToButtonMap = new LinkedHashMap<>();
  private final ButtonGroup myButtonGroup = new ButtonGroup();
  private ChangeListener myChangeListener;

  public ColorSelectionComponent() {
    super(new GridLayout(1, 0, 5, 5));
    setOpaque(false);
  }

  public void setChangeListener(ChangeListener changeListener) {
    myChangeListener = changeListener;
  }

  public void setSelectedColor(String colorName) {
    AbstractButton button = myColorToButtonMap.get(colorName);
    if (button != null) {
      button.setSelected(true);
    }
  }

  @NotNull
  public Collection<String> getColorNames() {
    return myColorToButtonMap.keySet();
  }

  @Nullable
  public String getColorName(@Nullable Color color) {
    if (color == null) return null;
    for (String name : myColorToButtonMap.keySet()) {
      if (color.getRGB() == myColorToButtonMap.get(name).getColor().getRGB()) {
        return name;
      }
    }
    return null;
  }

  public void addCustomColorButton() {
    CustomColorButton customButton = new CustomColorButton();
    myButtonGroup.add(customButton);
    add(customButton);
    myColorToButtonMap.put(customButton.getText(), customButton);
  }

  public void addColorButton(@NotNull String name, @NotNull Color color) {
    ColorButton colorButton = new ColorButton(name, color);
    myButtonGroup.add(colorButton);
    add(colorButton);
    myColorToButtonMap.put(name, colorButton);
  }

  public void setCustomButtonColor(@NotNull Color color) {
    CustomColorButton button = (CustomColorButton)myColorToButtonMap.get(CUSTOM_COLOR_NAME);
    button.setColor(color);
    button.setSelected(true);
    button.repaint();
  }

  @Nullable
  public String getSelectedColorName() {
    for (String name : myColorToButtonMap.keySet()) {
      ColorButton button = myColorToButtonMap.get(name);
      if (!button.isSelected()) continue;
      if (button instanceof CustomColorButton) {
        final String color = ColorUtil.toHex(button.getColor());
        String colorName  = FileColorManagerImpl.getColorName(button.getColor());
        return colorName == null ? color : colorName;
      }
      return name;
    }
    return null;
  }

  public void initDefault(@NotNull FileColorManager manager, @Nullable String selectedColorName) {
    for (String name : manager.getColorNames()) {
      addColorButton(name, ObjectUtils.assertNotNull(manager.getColor(name)));
    }
    addCustomColorButton();
    setSelectedColor(selectedColorName);
  }

  private class ColorButton extends ColorButtonBase {
    protected ColorButton(String text, Color color) {
      super(text, color);
    }

    @Override
    protected void doPerformAction(ActionEvent e) {
      stateChanged();
    }
  }

  public void stateChanged() {
    if (myChangeListener != null) {
      myChangeListener.stateChanged(new ChangeEvent(this));
    }
  }

  private class CustomColorButton extends ColorButton {
    private CustomColorButton() {
      super(CUSTOM_COLOR_NAME, JBColor.WHITE);
      myColor = null;
    }

    @Override
    protected void doPerformAction(ActionEvent e) {
      final Color color = ColorChooser.chooseColor(this, "Choose Color", myColor);
      if (color != null) {
        myColor = color;
      }
      setSelected(myColor != null);
      stateChanged();
    }

    @Override
    protected ButtonUI createUI() {
      return new ColorButtonUI() {
        @Nullable
        @Override
        protected Color getUnfocusedBorderColor(@NotNull ColorButtonBase button) {
          if (StartupUiUtil.isUnderDarcula()) return JBColor.GRAY;
          return super.getUnfocusedBorderColor(button);
        }
      };
    }

    @Override
    public Color getForeground() {
      return getModel().isSelected() ? JBColor.BLACK : JBColor.GRAY;
    }

    @NotNull
    @Override
    Color getColor() {
      return myColor == null ? JBColor.WHITE : myColor;
    }
  }
}
