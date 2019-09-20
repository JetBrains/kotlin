// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.intellij.execution.services.RepaintLinkMouseListenerBase.ACTIVE_TAG;

abstract class ServiceViewTreeCellRendererBase extends NodeRenderer {
  private boolean myAppendingTag;

  protected abstract Object getTag(String fragment);

  @Override
  public void append(@Nls @NotNull String fragment, @NotNull SimpleTextAttributes attributes, boolean isMainText) {
    Object tag = myAppendingTag ? null : getTag(fragment);
    if (tag == null) {
      super.append(fragment, attributes, isMainText);
      return;
    }

    boolean isActive = mySelected || tag.equals(UIUtil.getClientProperty(myTree, ACTIVE_TAG));
    int linkStyle = getLinkStyle(attributes, isActive);
    Color linkColor = getLinkColor(isActive);
    myAppendingTag = true;
    try {
      append(fragment, new SimpleTextAttributes(linkStyle, linkColor), tag);
    }
    finally {
      myAppendingTag = false;
    }
  }

  private Color getLinkColor(boolean isActive) {
    return mySelected && isFocused()
           ? UIUtil.getTreeSelectionForeground(true)
           : isActive ? JBUI.CurrentTheme.Link.linkHoverColor() : JBUI.CurrentTheme.Link.linkColor();
  }

  @SimpleTextAttributes.StyleAttributeConstant
  private static int getLinkStyle(@NotNull SimpleTextAttributes attributes, boolean isActive) {
    int linkStyle = attributes.getStyle() & ~SimpleTextAttributes.STYLE_WAVED & ~SimpleTextAttributes.STYLE_BOLD_DOTTED_LINE;
    if (isActive) {
      linkStyle |= SimpleTextAttributes.STYLE_UNDERLINE;
    }
    else {
      linkStyle &= ~SimpleTextAttributes.STYLE_UNDERLINE;
    }
    return linkStyle;
  }
}
