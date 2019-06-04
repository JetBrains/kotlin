// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.navigationToolbar.NavBarItem;
import com.intellij.ide.navigationToolbar.NavBarPanel;
import com.intellij.ide.ui.UISettings;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.ui.RelativeFont.SMALL;

/**
 * @author Konstantin Bulenkov
 */
public abstract class AbstractNavBarUI implements NavBarUI {

  private final static Map<NavBarItem, Map<ImageType, BufferedImage>> myCache = new THashMap<>();

  private enum ImageType {
    INACTIVE, NEXT_ACTIVE, ACTIVE, INACTIVE_FLOATING, NEXT_ACTIVE_FLOATING, ACTIVE_FLOATING,
    INACTIVE_NO_TOOLBAR, NEXT_ACTIVE_NO_TOOLBAR, ACTIVE_NO_TOOLBAR
  }

  @Override
  public Insets getElementIpad(boolean isPopupElement) {
    return isPopupElement ? JBInsets.create(1, 2) : JBUI.emptyInsets();
  }

  @Override
  public JBInsets getElementPadding() {
    return JBUI.insets(3);
  }

  @Override
  public Font getElementFont(NavBarItem navBarItem) {
    Font font = UIUtil.getLabelFont();
    return UISettings.getInstance().getUseSmallLabelsOnTabs() ? SMALL.derive(font) : font;
  }

  @Override
  public Color getBackground(boolean selected, boolean focused) {
    return selected && focused ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground();
  }

  @Nullable
  @Override
  public Color getForeground(boolean selected, boolean focused, boolean inactive) {
    return (selected && focused) ? UIUtil.getListSelectionForeground()
                                 : inactive ? UIUtil.getInactiveTextColor() : null;
  }

  @Override
  public short getSelectionAlpha() {
    return 150;
  }

  @Override
  public void doPaintNavBarItem(Graphics2D g, NavBarItem item, NavBarPanel navbar) {
    final boolean floating = navbar.isInFloatingMode();
    boolean toolbarVisible = UISettings.getInstance().getShowMainToolbar();
    final boolean selected = item.isSelected() && item.isFocused();
    boolean nextSelected = item.isNextSelected() && navbar.isFocused();


    ImageType type;
    if (floating) {
      type = selected ? ImageType.ACTIVE_FLOATING : nextSelected ? ImageType.NEXT_ACTIVE_FLOATING : ImageType.INACTIVE_FLOATING;
    } else {
      if (toolbarVisible) {
        type = selected ? ImageType.ACTIVE : nextSelected ? ImageType.NEXT_ACTIVE : ImageType.INACTIVE;
      } else {
        type = selected ? ImageType.ACTIVE_NO_TOOLBAR : nextSelected ? ImageType.NEXT_ACTIVE_NO_TOOLBAR : ImageType.INACTIVE_NO_TOOLBAR;
      }
    }

    Map<ImageType, BufferedImage> cached = myCache.computeIfAbsent(item, k -> new HashMap<>());

    BufferedImage image = cached.computeIfAbsent(type, k -> drawToBuffer(item, floating, toolbarVisible, selected, navbar));

    UIUtil.drawImage(g, image, 0, 0, null);

    Icon icon = item.getIcon();
    final int offset = item.isFirstElement() ? getFirstElementLeftOffset() : 0;
    final int iconOffset = getElementPadding().left + offset;
    icon.paintIcon(item, g, iconOffset, (item.getHeight() - icon.getIconHeight()) / 2);
    final int textOffset = icon.getIconWidth() + getElementPadding().width() + offset;
    item.doPaintText(g, textOffset);
  }

  private static BufferedImage drawToBuffer(NavBarItem item, boolean floating, boolean toolbarVisible, boolean selected, NavBarPanel navbar) {
    int w = item.getWidth();
    int h = item.getHeight();
    int offset = (w - getDecorationOffset());
    int h2 = h / 2;

    BufferedImage result = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);

    Color defaultBg = UIUtil.isUnderDarcula() ? Gray._100 : JBColor.WHITE;
    final Paint bg = floating ? defaultBg : null;
    final Color selection = UIUtil.getListSelectionBackground();

    Graphics2D g2 = result.createGraphics();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


    Path2D.Double shape = new Path2D.Double();
    shape.moveTo(0, 0);

    shape.lineTo(offset, 0);
    shape.lineTo(w, h2);
    shape.lineTo(offset, h);
    shape.lineTo(0, h);
    shape.closePath();

    Path2D.Double endShape = new Path2D.Double();
    endShape.moveTo(offset, 0);
    endShape.lineTo(w, 0);
    endShape.lineTo(w, h);
    endShape.lineTo(offset, h);
    endShape.lineTo(w, h2);
    endShape.closePath();

    if (bg != null && toolbarVisible) {
      g2.setPaint(bg);
      g2.fill(shape);
      g2.fill(endShape);
    }

    if (selected) {
      Path2D.Double focusShape = new Path2D.Double();
      if (toolbarVisible || floating) {
        focusShape.moveTo(offset, 0);
      } else {
        focusShape.moveTo(0, 0);
        focusShape.lineTo(offset, 0);
      }
      focusShape.lineTo(w - 1, h2);
      focusShape.lineTo(offset, h - 1);
      if (!toolbarVisible && !floating) {
        focusShape.lineTo(0, h - 1);

      }

      g2.setColor(selection);
      if (floating && item.isLastElement()) {
        g2.fillRect(0, 0, w, h);
      } else {
        g2.fill(shape);
      }
    }

    if (item.isNextSelected() && navbar.isFocused()) {
      g2.setColor(selection);
      g2.fill(endShape);
    }

    if (!item.isLastElement()) {
      if (!selected && (!navbar.isFocused() | !item.isNextSelected())) {
        Icon icon = AllIcons.Ide.NavBarSeparator;
        icon.paintIcon(item, g2, w - icon.getIconWidth() - JBUIScale.scale(1), h2 - icon.getIconHeight() / 2);
      }
    }

    g2.dispose();
    return result;
  }

  private static int getDecorationOffset() {
    return JBUIScale.scale(8);
  }

   private static int getFirstElementLeftOffset() {
     return JBUIScale.scale(6);
   }

  @Override
  public Dimension getOffsets(NavBarItem item) {
    final Dimension size = new Dimension();
    if (! item.isPopupElement()) {
      size.width += getDecorationOffset() + getElementPadding().width() + (item.isFirstElement() ? getFirstElementLeftOffset() : 0);
      size.height += getElementPadding().height();
    }
    return size;
  }

  @Override
  public Insets getWrapperPanelInsets(Insets insets) {
    final JBInsets result = JBUI.insets(insets);
    if (shouldPaintWrapperPanel()) {
      result.top += JBUIScale.scale(1);
    }
    return result;
  }

  private static boolean shouldPaintWrapperPanel() {
    return false; //return !UISettings.getInstance().SHOW_MAIN_TOOLBAR && NavBarRootPaneExtension.runToolbarExists();
  }

  protected Color getBackgroundColor() {
    return ColorUtil.darker(UIUtil.getPanelBackground(), 1);
  }

  @Override
  public void doPaintNavBarPanel(Graphics2D g, Rectangle r, boolean mainToolbarVisible, boolean undocked) {
  }

  @Override
  public void clearItems() {
    myCache.clear();
  }

  @Override
  public int getPopupOffset(@NotNull NavBarItem item) {
    return item.isFirstElement() ? 0 : JBUIScale.scale(5);
  }
}
