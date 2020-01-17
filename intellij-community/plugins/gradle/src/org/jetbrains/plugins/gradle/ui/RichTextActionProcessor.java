// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.ui.ClickListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author Denis Zhdanov
 */
public class RichTextActionProcessor implements RichTextControlBuilder.RichTextProcessor {

  @Override
  public JComponent process(@NotNull String s) {
    final ActionManager actionManager = ActionManager.getInstance();
    final AnAction action = actionManager.getAction(s);
    if (action == null) {
      return null;
    }
    final Presentation presentation = action.getTemplatePresentation();

    if (presentation.getIcon() != null) {
      return new ActionButton(action, presentation.clone(), GradleConstants.TOOL_WINDOW_TOOLBAR_PLACE, JBUI.emptySize()) {
        @Override
        protected void paintButtonLook(Graphics g) {
          // Don't draw border at the inline button.
          ActionButtonLook look = getButtonLook();
          look.paintBackground(g, this);
          look.paintIcon(g, this, getIcon());
        }
      };
    }

    final String text = action.getTemplatePresentation().getText();
    JLabel result = new JLabel(text) {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        final int y = g.getClipBounds().height - getFontMetrics(getFont()).getDescent() + 2;
        final int width = getFontMetrics(getFont()).stringWidth(getText());
        g.drawLine(0, y, width, y);
      }
    };
    Color color = StartupUiUtil.isUnderDarcula() ? Color.ORANGE : Color.BLUE;
    result.setForeground(color);
    result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        final DataContext context = DataManager.getInstance().getDataContextFromFocus().getResult();
        if (context == null) {
          return false;
        }
        final Presentation presentation = new PresentationFactory().getPresentation(action);
        action.actionPerformed(new AnActionEvent(
          e, context, GradleConstants.TOOL_WINDOW_TOOLBAR_PLACE, presentation, ActionManager.getInstance(), e.getModifiers()
        ));
        return true;
      }
    }.installOn(result);
    return result;
  }

  @NotNull
  @Override
  public String getKey() {
    return "action";
  }
}
