// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs;

import com.intellij.notification.impl.ui.StickyButton;
import com.intellij.notification.impl.ui.StickyButtonUI;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class ColorButtonBase extends StickyButton {
  @Nullable protected Color myColor;

  protected ColorButtonBase(@NotNull final String text, @NotNull final Color color) {
    super(FileColorManagerImpl.getAlias(text));
    myColor = color;
    setUI(createUI());
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doPerformAction(e);
      }
    });

    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
  }

  @Override
  public void setUI(ButtonUI ui) {
    if (myColor == null) return; // we call setUI manually after parent constructor invocation
    super.setUI(ui);
  }

  protected abstract void doPerformAction(ActionEvent e);

  @NotNull
  Color getColor() {
    assert myColor != null;
    return myColor;
  }

  public void setColor(@NotNull Color color) {
    myColor = color;
  }

  @Override
  public Color getForeground() {
    if (getModel().isSelected()) {
      return JBColor.foreground();
    }
    else if (getModel().isRollover()) {
      return JBColor.GRAY;
    }
    else {
      return getColor();
    }
  }

  @Override
  protected ButtonUI createUI() {
    return new ColorButtonUI();
  }


  protected static class ColorButtonUI extends StickyButtonUI<ColorButtonBase> {

    @Override
    protected Color getBackgroundColor(@NotNull final ColorButtonBase button) {
      return button.getColor();
    }

    @Override
    protected Color getFocusColor(@NotNull ColorButtonBase button) {
      return UIUtil.isUnderDarcula() ? button.getColor().brighter() : button.getColor().darker();
    }

    @Override
    protected Color getSelectionColor(@NotNull ColorButtonBase button) {
      return button.getColor();
    }

    @Override
    protected Color getRolloverColor(@NotNull ColorButtonBase button) {
      return button.getColor();
    }

    @Override
    protected int getArcSize() {
      return JBUIScale.scale(15);
    }
  }
}
