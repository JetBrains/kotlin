// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.Executor;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.HelpTooltip;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandExecutionProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingRecentCommandProvider;
import com.intellij.ide.actions.runAnything.activity.RunAnythingRecentProjectProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.keymap.MacKeymapUtil;
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.FontUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class RunAnythingAction extends AnAction implements CustomComponentAction, DumbAware {
  public static final String RUN_ANYTHING_ACTION_ID = "RunAnything";
  public static final DataKey<Executor> EXECUTOR_KEY = DataKey.create("EXECUTOR_KEY");
  public static final AtomicBoolean SHIFT_IS_PRESSED = new AtomicBoolean(false);
  public static final AtomicBoolean ALT_IS_PRESSED = new AtomicBoolean(false);
  static final String RUN_ANYTHING = "RunAnything";

  private boolean myIsDoubleCtrlRegistered;

  private static final NotNullLazyValue<Boolean> IS_ACTION_ENABLED = new NotNullLazyValue<Boolean>() {
    @NotNull
    @Override
    protected Boolean compute() {
      return Arrays.stream(RunAnythingProvider.EP_NAME.getExtensions())
        .anyMatch(provider -> !(provider instanceof RunAnythingRunConfigurationProvider ||
                                provider instanceof RunAnythingRecentProjectProvider ||
                                provider instanceof RunAnythingRecentCommandProvider ||
                                provider instanceof RunAnythingCommandExecutionProvider));
    }
  };

  static {
    IdeEventQueue.getInstance().addPostprocessor(event -> {
      if (event instanceof KeyEvent) {
        final int keyCode = ((KeyEvent)event).getKeyCode();
        if (keyCode == KeyEvent.VK_SHIFT) {
          SHIFT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
        }
        else if (keyCode == KeyEvent.VK_ALT) {
          ALT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
        }
      }
      return false;
    }, null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (Registry.is("ide.suppress.double.click.handler") && e.getInputEvent() instanceof KeyEvent) {
      if (((KeyEvent)e.getInputEvent()).getKeyCode() == KeyEvent.VK_CONTROL) {
        return;
      }
    }

    if (e.getProject() != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_RUN_ANYTHING);

      RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(e.getProject());
      String text = GotoActionBase.getInitialTextForNavigation(e.getData(CommonDataKeys.EDITOR));
      runAnythingManager.show(text, e);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    if (getActiveKeymapShortcuts(RUN_ANYTHING_ACTION_ID).getShortcuts().length == 0) {
      if (!myIsDoubleCtrlRegistered) {
        ModifierKeyDoubleClickHandler.getInstance().registerAction(RUN_ANYTHING_ACTION_ID, KeyEvent.VK_CONTROL, -1, false);
        myIsDoubleCtrlRegistered = true;
      }
    }
    else {
      if (myIsDoubleCtrlRegistered) {
        ModifierKeyDoubleClickHandler.getInstance().unregisterAction(RUN_ANYTHING_ACTION_ID);
        myIsDoubleCtrlRegistered = false;
      }
    }

    boolean isEnabled = IS_ACTION_ENABLED.getValue();
    e.getPresentation().setEnabledAndVisible(isEnabled);
  }

  @NotNull
  @Override
  public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
    return new ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
      @Override
      protected void updateToolTipText() {
        if (Registry.is("ide.helptooltip.enabled")) {
          HelpTooltip.dispose(this);

          new HelpTooltip()
            .setTitle(myPresentation.getText())
            .setShortcut(getShortcut())
            .setDescription(getShortcutText())
            .setLocation(getTooltipLocation()).installOn(this);
        }
        else {
          setToolTipText(getShortcutText());
        }
      }
    };
  }

  @NotNull
  private String getShortcutText() {
    return IdeBundle.message("run.anything.action.tooltip.text") + getShortcut();
  }

  @NotNull
  private String getShortcut() {
    if (myIsDoubleCtrlRegistered) {
      return " " + IdeBundle
        .message("run.anything.double.ctrl.shortcut", SystemInfo.isMac ? FontUtil.thinSpace() + MacKeymapUtil.CONTROL : " Ctrl");
    }
    //keymap shortcut is added automatically
    return "";
  }
}