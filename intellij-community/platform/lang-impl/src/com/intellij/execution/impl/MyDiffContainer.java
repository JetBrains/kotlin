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
package com.intellij.execution.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.AnimatedIcon;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
* Created by IntelliJ IDEA.
* @author amakeev
* @author Irina.Chernushina
*/
class MyDiffContainer extends JBLayeredPane implements Disposable {
  private final AnimatedIcon myIcon = new AsyncProcessIcon(getClass().getName());

  private final JComponent myContent;
  private final JComponent myLoadingPanel;
  private final JLabel myJLabel;

  MyDiffContainer(@NotNull JComponent content, @NotNull String text) {
    setLayout(new MyOverlayLayout());
    myContent = content;
    myLoadingPanel = new JPanel(new MyPanelLayout());
    myLoadingPanel.setOpaque(false);
    myLoadingPanel.add(myIcon);
    Disposer.register(this, myIcon);
    myJLabel = new JLabel(text);
    myJLabel.setForeground(UIUtil.getInactiveTextColor());
    myLoadingPanel.add(myJLabel);

    add(myContent);
    add(myLoadingPanel, JLayeredPane.POPUP_LAYER);

    finishUpdating();
  }

  @Override
  public void dispose() {
  }

  void startUpdating() {
    myLoadingPanel.setVisible(true);
    myIcon.resume();
  }

  void finishUpdating() {
    myIcon.suspend();
    myLoadingPanel.setVisible(false);
  }

  private class MyOverlayLayout extends AbstractLayoutManager {
    @Override
    public void layoutContainer(Container parent) {
      /*
        Propogate bound to all children
       */
      for(int i = 0; i< getComponentCount(); i++) {
        getComponent(i).setBounds(0, 0, getWidth(), getHeight());
      }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return myContent.getPreferredSize();
    }
  }

  private class MyPanelLayout extends AbstractLayoutManager {
    @Override
    public void layoutContainer(Container parent) {
      Dimension size = myIcon.getPreferredSize();
      Dimension preferredSize = myJLabel.getPreferredSize();
      int width = getWidth();
      int offset = width - size.width - 15 - preferredSize.width;
      myIcon.setBounds(offset, 0, size.width, size.height);
      myJLabel.setBounds(offset + size.width + 3, 0, preferredSize.width, size.height);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return myContent.getPreferredSize();
    }
  }
}
