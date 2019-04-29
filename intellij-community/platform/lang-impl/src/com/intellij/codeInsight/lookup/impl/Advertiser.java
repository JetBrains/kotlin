/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.codeInsight.lookup.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.ClickListener;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 */
public class Advertiser {
  private final List<Pair<String, Icon>> myTexts = ContainerUtil.createLockFreeCopyOnWriteList();
  private final JPanel myComponent = new JPanel(new AdvertiserLayout());

  private final AtomicInteger myCurrentItem = new AtomicInteger(0);
  private final JLabel myTextPanel = createLabel();
  private final JLabel myNextLabel;

  public Advertiser() {
    myNextLabel = new JLabel("Next Tip");
    myNextLabel.setFont(adFont());
    myNextLabel.setForeground(JBUI.CurrentTheme.Link.linkColor());
    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        myCurrentItem.incrementAndGet();
        updateAdvertisements();
        return true;
      }
    }.installOn(myNextLabel);

    myNextLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    myComponent.add(myTextPanel);
    myComponent.add(myNextLabel);
    myComponent.setOpaque(true);
    myComponent.setBackground(JBUI.CurrentTheme.Advertiser.background());
    myComponent.setBorder(JBUI.CurrentTheme.Advertiser.border());
  }

  private void updateAdvertisements() {
    myNextLabel.setVisible(myTexts.size() > 1);
    if (!myTexts.isEmpty()) {
      Pair<String, Icon> pair = myTexts.get(myCurrentItem.get() % myTexts.size());
      String text = pair.first;
      myTextPanel.setText(prepareText(text));
      myTextPanel.setIcon(pair.second);
    }
    else {
      myTextPanel.setText("");
      myTextPanel.setIcon(null);
    }
    myComponent.revalidate();
    myComponent.repaint();
  }

  private static JLabel createLabel() {
    JLabel label = new JLabel();
    label.setFont(adFont());
    label.setForeground(JBUI.CurrentTheme.Advertiser.foreground());
    return label;
  }

  private static String prepareText(String text) {
    return text + "  ";
  }

  public void showRandomText() {
    int count = myTexts.size();
    myCurrentItem.set(count > 0 ? new Random().nextInt(count) : 0);
    updateAdvertisements();
  }

  public void clearAdvertisements() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myTexts.clear();
    myCurrentItem.set(0);
    updateAdvertisements();
  }

  private static Font adFont() {
    Font font = UIUtil.getLabelFont();
    return font.deriveFont((float)(font.getSize() - JBUI.scale(2)));
  }

  public void addAdvertisement(@NotNull String text, @Nullable Icon icon) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myTexts.add(Pair.create(text, icon));
    updateAdvertisements();
  }

  public void setBackground(@Nullable Color background) {
    myComponent.setBackground(background != null ? background : JBUI.CurrentTheme.Advertiser.background());
  }

  public JComponent getAdComponent() {
    return myComponent;
  }

  public List<String> getAdvertisements() {
    return ContainerUtil.map(myTexts, pair -> pair.first);
  }

  // ------------------------------------------------------
  // Custom layout
  private class AdvertiserLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      Insets i = parent.getInsets();
      Dimension size = new Dimension();
      Dimension nextButtonSize = myNextLabel.getPreferredSize();

      FontMetrics fm = myTextPanel.getFontMetrics(myTextPanel.getFont());

      for (Pair<String, Icon> label : myTexts) {
        int width = SwingUtilities.computeStringWidth(fm, prepareText(label.first));

        if (label.second != null) {
          width += myTextPanel.getIconTextGap() + label.second.getIconWidth();
        }

        width += nextButtonSize.width + i.left + i.right;

        int height = Math.max(fm.getHeight(), label.second != null ? label.second.getIconHeight() : 0) + i.top + i.bottom;

        size.width = Math.max(size.width, width);
        size.height = Math.max(size.height, Math.max(height, nextButtonSize.height));
      }

      return size;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension minSize = myNextLabel.getPreferredSize();
      JBInsets.addTo(minSize, parent.getInsets());
      return minSize;
    }

    @Override
    public void layoutContainer(Container parent) {
      Insets i = parent.getInsets();
      Dimension size = parent.getSize();
      Dimension textPrefSize = myTextPanel.getPreferredSize();
      Dimension nextPrefSize = myNextLabel.getPreferredSize();

      int textWidth = (i.left + i.right + textPrefSize.width + nextPrefSize.width <= size.width) ?
                      textPrefSize.width : size.width - nextPrefSize.width - i.left - i.right;

      myTextPanel.setBounds(i.left, i.top, textWidth, textPrefSize.height);
      myNextLabel.setBounds(i.left + textWidth, i.top, nextPrefSize.width, nextPrefSize.height);
    }
  }
}
