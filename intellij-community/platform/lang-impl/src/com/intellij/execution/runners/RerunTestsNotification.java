// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.runners;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.GotItMessage;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.*;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.concurrent.TimeUnit;

public class RerunTestsNotification {

  private static final String KEY = "rerun.tests.notification.shown";

  public static void showRerunNotification(@Nullable RunContentDescriptor contentToReuse,
                                           @NotNull ExecutionConsole executionConsole) {
    if (contentToReuse == null) {
      return;
    }
    String lastActionId = ActionManagerEx.getInstanceEx().getPrevPreformedActionId();
    boolean showNotification = !RerunTestsAction.ID.equals(lastActionId);
    if (showNotification && !PropertiesComponent.getInstance().isTrueValue(KEY)) {
      UiNotifyConnector.doWhenFirstShown(executionConsole.getComponent(), () -> doShow(executionConsole));
    }
  }

  private static void doShow(@NotNull ExecutionConsole executionConsole) {
    EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
      String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(
        ActionManager.getInstance().getAction(RerunTestsAction.ID)
      );
      if (shortcutText.isEmpty()) {
        return;
      }

      ConsoleView consoleView = UIUtil.findComponentOfType(executionConsole.getComponent(), ConsoleViewImpl.class);
      if (consoleView != null) {
        GotItMessage message = GotItMessage.createMessage("Rerun tests with " + shortcutText, "");
        Disposable disposable = Disposer.newDisposable();
        Disposer.register(executionConsole, disposable);
        message.setDisposable(disposable);
        message.setCallback(() -> PropertiesComponent.getInstance().setValue(KEY, true));
        message.setShowCallout(false);
        JComponent consoleComponent = consoleView.getComponent();
        message.show(
          new PositionTracker<Balloon>(consoleComponent) {
            @Override
            public RelativePoint recalculateLocation(@NotNull Balloon balloon) {
              RelativePoint point = RelativePoint.getSouthEastOf(consoleComponent);
              Insets shadowInsets = balloon instanceof BalloonImpl ? ((BalloonImpl)balloon).getShadowBorderInsets()
                                                                   : JBUI.emptyInsets();
              Dimension balloonContentSize = JBDimension.create(balloon.getPreferredSize(), true);
              JBInsets.removeFrom(balloonContentSize, shadowInsets);

              // compensate "-shift.top" from BalloonImpl.Below.getShiftedPoint(java.awt.Point, java.awt.Insets)
              point.getPoint().y += shadowInsets.top;

              int spacingFromEdges = JBUIScale.scale(12);
              point.getPoint().translate(-balloonContentSize.width / 2 - spacingFromEdges, -balloonContentSize.height / 2 - spacingFromEdges);
              return point;
            }
          },
          Balloon.Position.below
        );
        consoleComponent.addHierarchyListener(new HierarchyListener() {
          @Override
          public void hierarchyChanged(HierarchyEvent e) {
            if (!consoleComponent.isShowing()) {
              Disposer.dispose(disposable);
              consoleComponent.removeHierarchyListener(this);
            }
          }
        });
      }
    }, 1000, TimeUnit.MILLISECONDS);
  }

}
