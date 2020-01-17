// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.changes.issueLinks.LinkMouseListenerBase;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Link mouse listener which stores a tag pointed by a cursor into installed component,
 * changes cursor, and repaint the component if active tag is changed.
 * Tag could be retrieved from the component by {@link RepaintLinkMouseListenerBase#ACTIVE_TAG} key.
 */
@ApiStatus.Experimental
public abstract class RepaintLinkMouseListenerBase<T> extends LinkMouseListenerBase<T> {
  public static final Key<Object> ACTIVE_TAG = Key.create("RepaintLinkMouseListenerActiveTag");

  @Override
  public void mouseMoved(MouseEvent e) {
    if (!isEnabled()) return;

    JComponent component = (JComponent)e.getSource();
    Object tag = getTagAt(e);
    UIUtil.setCursor(component, tag != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    if (!Comparing.equal(tag, ComponentUtil.getClientProperty(component, ACTIVE_TAG))) {
      ComponentUtil.putClientProperty(component, ACTIVE_TAG, tag);
      repaintComponent(e);
    }
  }

  @Override
  public void installOn(@NotNull Component component) {
    if (!(component instanceof JComponent)) {
      throw new IllegalArgumentException("JComponent expected");
    }
    super.installOn(component);

    component.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        JComponent component = (JComponent)e.getSource();
        Object tag = ComponentUtil.getClientProperty(component, ACTIVE_TAG);
        if (tag != null) {
          ComponentUtil.putClientProperty(component, ACTIVE_TAG, null);
          repaintComponent(e);
        }
      }
    });
  }

  protected abstract void repaintComponent(MouseEvent e);

  /**
   * Override this method if link listener should be temporary disabled for a component.
   * For example, if tree is empty and empty text link listener should manage the cursor.
   */
  protected boolean isEnabled() {
    return true;
  }
}
