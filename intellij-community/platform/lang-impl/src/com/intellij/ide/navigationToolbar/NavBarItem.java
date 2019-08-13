// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.ide.navigationToolbar.ui.NavBarUI;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarItem extends SimpleColoredComponent implements DataProvider, Disposable {
  private final String myText;
  private final SimpleTextAttributes myAttributes;
  private final int myIndex;
  private final Icon myIcon;
  private final NavBarPanel myPanel;
  private final Object myObject;
  private final boolean isPopupElement;
  private final NavBarUI myUI;

  public NavBarItem(NavBarPanel panel, Object object, int idx, Disposable parent) {
    myPanel = panel;
    myUI = panel.getNavBarUI();
    myObject = object;
    myIndex = idx;
    isPopupElement = idx == -1;

    if (object != null) {
      NavBarPresentation presentation = myPanel.getPresentation();
      myText = presentation.getPresentableText(object);
      Icon icon = presentation.getIcon(object);
      myIcon = icon != null ? icon : JBUI.scale(EmptyIcon.create(5));
      myAttributes = presentation.getTextAttributes(object, false);
    }
    else {
      myText = "Sample";
      myIcon = PlatformIcons.FOLDER_ICON;
      myAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    Disposer.register(parent == null ? panel : parent, this);

    setOpaque(false);
    setIpad(myUI.getElementIpad(isPopupElement));

    if (!isPopupElement) {
      setMyBorder(null);
      setBorder(null);
      setPaintFocusBorder(false);
      if (myPanel.allowNavItemsFocus()) {
        // Take ownership of Tab/Shift-Tab navigation (to move focus out of nav bar panel), as
        // navigation between items is handled by the Left/Right cursor keys. This is similar
        // to the behavior a JRadioButton contained inside a GroupBox.
        setFocusTraversalKeysEnabled(false);
        setFocusable(true);
        addKeyListener(new KeyHandler());
        addFocusListener(new FocusHandler());
      }
    }

    update();
  }

  public NavBarItem(NavBarPanel panel, Object object, Disposable parent) {
    this(panel, object, -1, parent);
  }

  public Object getObject() {
    return myObject;
  }

  public SimpleTextAttributes getAttributes() {
    return myAttributes;
  }

  @NotNull
  public String getText() {
    return myText;
  }

  @Override
  public Font getFont() {
    return myUI == null ? super.getFont() : myUI.getElementFont(this);
  }

  void update() {
    clear();

    setIcon(myIcon);

    final boolean focused = isFocusedOrPopupElement();
    final boolean selected = isSelected();

    setFocusBorderAroundIcon(false);
    setBackground(myUI.getBackground(selected, focused));

    Color fg = myUI.getForeground(selected, focused, isInactive());
    if (fg == null) fg = myAttributes.getFgColor();

    final Color bg = getBackground();
    append(myText, new SimpleTextAttributes(bg, fg, myAttributes.getWaveColor(), myAttributes.getStyle()));

    //repaint();
  }

  public boolean isInactive() {
    final NavBarModel model = myPanel.getModel();
    return model.getSelectedIndex() < myIndex && model.getSelectedIndex() != -1;
  }

  public boolean isPopupElement() {
    return isPopupElement;
  }

  @Override
  protected void doPaint(Graphics2D g) {
    if (isPopupElement) {
      super.doPaint(g);
    }
    else {
      myUI.doPaintNavBarItem(g, this, myPanel);
    }
  }

  public int doPaintText(Graphics2D g, int offset) {
    return super.doPaintText(g, offset, false);
  }

  public boolean isLastElement() {
    return myIndex == myPanel.getModel().size() - 1;
  }

  public boolean isFirstElement() {
    return myIndex == 0;
  }

  @Override
  public void setOpaque(boolean isOpaque) {
    super.setOpaque(false);
  }

  @NotNull
  @Override
  public Dimension getPreferredSize() {
    final Dimension size = super.getPreferredSize();
    final Dimension offsets = myUI.getOffsets(this);
    return new Dimension(size.width + offsets.width, size.height + offsets.height);
  }

  @NotNull
  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  private boolean isFocusedOrPopupElement() {
    return isFocused() || isPopupElement;
  }

  public boolean isFocused() {
    if (myPanel.allowNavItemsFocus()) {
      return UIUtil.isFocusAncestor(myPanel) && !myPanel.isNodePopupActive();
    } else {
      return myPanel.hasFocus() && !myPanel.isNodePopupActive();
    }
  }

  public boolean isSelected() {
    final NavBarModel model = myPanel.getModel();
    return isPopupElement ? myPanel.isSelectedInPopup(myObject) : model.getSelectedIndex() == myIndex;
  }

  @Override
  protected boolean shouldDrawBackground() {
    return isSelected() && isFocusedOrPopupElement();
  }

  @Override
  public boolean isIconOpaque() {
    return false;
  }

  @Override
  public void dispose() { }

  public boolean isNextSelected() {
    return myIndex == myPanel.getModel().getSelectedIndex() - 1;
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    return myPanel.getDataImpl(dataId, this, () -> JBIterable.of(myObject));
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleNavBarItem();
    }
    return accessibleContext;
  }

  protected class AccessibleNavBarItem extends AccessibleSimpleColoredComponent implements AccessibleAction {
    @Override
    public AccessibleRole getAccessibleRole() {
      if (!isPopupElement()) {
        return AccessibleRole.PUSH_BUTTON;
      }
      return super.getAccessibleRole();
    }

    @Override
    public AccessibleAction getAccessibleAction() {
      return this;
    }

    @Override
    public int getAccessibleActionCount() {
      return !isPopupElement() ? 1 : 0;
    }

    @Override
    public String getAccessibleActionDescription(int i) {
      if (i == 0 && !isPopupElement()) {
        return UIManager.getString("AbstractButton.clickText");
      }
      return null;
    }

    @Override
    public boolean doAccessibleAction(int i) {
      if (i == 0 && !isPopupElement()) {
        myPanel.getModel().setSelectedIndex(myIndex);
      }
      return false;
    }
  }

  private class KeyHandler extends KeyAdapter {
    // This listener checks if the key event is a KeyEvent.VK_TAB
    // or shift + KeyEvent.VK_TAB event, consume the event
    // if so and move the focus to next/previous component after/before
    // the containing NavBarPanel.
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_TAB) {
        // Check source is a nav bar item
        if (e.getSource() instanceof NavBarItem) {
          e.consume();
          jumpToNextComponent(!e.isShiftDown());
        }
      }
    }

    void jumpToNextComponent(boolean next) {
      // The base will be first or last NavBarItem in the NavBarPanel
      NavBarItem focusBase = null;
      List<NavBarItem> items = myPanel.getItems();
      if (items.size() > 0) {
        if (next) {
          focusBase = items.get(items.size() - 1);
        } else {
          focusBase = items.get(0);
        }
      }

      // Transfer focus
      if (focusBase != null){
        if (next) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(focusBase);
        } else {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(focusBase);
        }
      }
    }
  }

  private class FocusHandler implements FocusListener {
    @Override
    public void focusGained(FocusEvent e) {
      myPanel.fireNavBarItemFocusGained(e);
    }

    @Override
    public void focusLost(FocusEvent e) {
      myPanel.fireNavBarItemFocusLost(e);
    }
  }
}
