// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.statusbar;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CodeStyleStatusBarPanel extends JPanel {
  private final TextPanel myLabel;
  private final JLabel myIconLabel;

  public CodeStyleStatusBarPanel() {
    super();
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setAlignmentY(Component.CENTER_ALIGNMENT);
    myLabel = new TextPanel() {};
    myLabel.setFont(SystemInfo.isMac ? JBUI.Fonts.label(11) : JBFont.label());
    add(myLabel);
    myIconLabel = new JLabel("");
    myIconLabel.setBorder(JBUI.Borders.empty(2,2,2,0));
    add(myIconLabel);
    setBorder(JBUI.Borders.empty(0));
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        setBackground(null);
      }
    });
  }

  public void setText(@NotNull String text) {
    myLabel.setText(text);
  }

  @Nullable
  public String getText() {
    return myLabel.getText();
  }

  public void setIcon(@Nullable Icon icon) {
    myIconLabel.setIcon(icon);
    myIconLabel.setVisible(icon != null);
  }
}
