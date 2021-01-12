// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.ui.DefaultExternalSystemUiAware;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;

/**
 * @author Denis Zhdanov
 */
public final class ExternalSystemUiUtil {

  public static final int INSETS = 5;
  private static final int BALLOON_FADEOUT_TIME = 5000;

  private ExternalSystemUiUtil() {
  }

  /**
   * Asks to show balloon that contains information related to the given component.
   *
   * @param component    component for which we want to show information
   * @param messageType  balloon message type
   * @param message      message to show
   */
  public static void showBalloon(@NotNull JComponent component, @NotNull MessageType messageType, @NotNull String message) {
    final BalloonBuilder builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, messageType, null)
      .setDisposable(ApplicationManager.getApplication())
      .setFadeoutTime(BALLOON_FADEOUT_TIME);
    Balloon balloon = builder.createBalloon();
    Dimension size = component.getSize();
    Balloon.Position position;
    int x;
    int y;
    if (size == null) {
      x = y = 0;
      position = Balloon.Position.above;
    }
    else {
      x = Math.min(10, size.width / 2);
      y = size.height;
      position = Balloon.Position.below;
    }
    balloon.show(new RelativePoint(component, new Point(x, y)), position);
  }

  @NotNull
  public static GridBag getLabelConstraints(int indentLevel) {
    Insets insets = JBUI.insets(INSETS, INSETS + INSETS * indentLevel, 0, INSETS);
    return new GridBag().anchor(GridBagConstraints.WEST).weightx(0).insets(insets);
  }

  @NotNull
  public static GridBag getFillLineConstraints(int indentLevel) {
    Insets insets = JBUI.insets(INSETS, INSETS + INSETS * indentLevel, 0, INSETS);
    return new GridBag().weightx(1).coverLine().fillCellHorizontally().anchor(GridBagConstraints.WEST).insets(insets);
  }

  public static void fillBottom(@NotNull JComponent component) {
    component.add(Box.createVerticalGlue(), new GridBag().weightx(1).weighty(1).fillCell().coverLine());
  }

  public static void showUi(@NotNull Object o, boolean show) {
    for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        field.setAccessible(true);
        try {
          Object v = field.get(o);
          if (v instanceof JComponent) {
            ((JComponent)v).setVisible(show);
          }
        }
        catch (IllegalAccessException e) {
          // Ignore
        }
      }
    }
  }

  public static void disposeUi(@NotNull Object o) {
    for (Class<?> clazz = o.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        field.setAccessible(true);
        try {
          Object v = field.get(o);
          if (v instanceof JComponent) {
            field.set(o, null);
          }
        }
        catch (IllegalAccessException e) {
          // Ignore
        }
      }
    }
  }

  @NotNull
  public static ExternalSystemUiAware getUiAware(@NotNull ProjectSystemId externalSystemId) {
    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    return manager instanceof ExternalSystemUiAware ? (ExternalSystemUiAware)manager : DefaultExternalSystemUiAware.INSTANCE;
  }

  public static void executeAction(@NotNull final String actionId, @NotNull final InputEvent e) {
    final ActionManager actionManager = ActionManager.getInstance();
    final AnAction action = actionManager.getAction(actionId);
    if (action == null) {
      return;
    }
    final Presentation presentation = new Presentation();
    DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
    final AnActionEvent event = new AnActionEvent(e, context, "", presentation, actionManager, 0);
    action.update(event);
    if (presentation.isEnabled()) {
      action.actionPerformed(event);
    }
  }
}
