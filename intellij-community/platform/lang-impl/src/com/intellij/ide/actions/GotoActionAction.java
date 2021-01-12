// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.searcheverywhere.ActionSearchEverywhereContributor;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.IdeFocusManager;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class GotoActionAction extends SearchEverywhereBaseAction implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    showInSearchEverywherePopup(ActionSearchEverywhereContributor.class.getSimpleName(), e, false, true);
  }

  public static void openOptionOrPerformAction(@NotNull Object element, String enteredText, @Nullable Project project, @Nullable Component component) {
    openOptionOrPerformAction(element, enteredText, project, component, 0);
  }

  private static void openOptionOrPerformAction(Object element,
                                                String enteredText,
                                                @Nullable Project project,
                                                @Nullable Component component,
                                                @JdkConstants.InputEventMask int modifiers) {
    // invoke later to let the Goto Action popup close completely before the action is performed
    // and avoid focus issues if the action shows complicated popups itself
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project != null && project.isDisposed()) return;

      if (element instanceof OptionDescription) {
        OptionDescription optionDescription = (OptionDescription)element;
        if (optionDescription.hasExternalEditor()) {
          optionDescription.invokeInternalEditor();
        } else {
          ShowSettingsUtilImpl.showSettingsDialog(project, optionDescription.getConfigurableId(), enteredText);
        }
      }
      else {
        IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(
          () -> performAction(element, component, null, modifiers, null));
      }
    });
  }

  public static void performAction(@NotNull Object element, @Nullable Component component, @Nullable AnActionEvent e) {
    performAction(element, component, e, 0, null);
  }

  private static void performAction(Object element,
                                    @Nullable Component component,
                                    @Nullable AnActionEvent e,
                                    @JdkConstants.InputEventMask int modifiers,
                                    @Nullable Runnable callback) {
    // element could be AnAction (SearchEverywhere)
    if (component == null) return;
    AnAction action = element instanceof AnAction ? (AnAction)element : ((GotoActionModel.ActionWrapper)element).getAction();
    ApplicationManager.getApplication().invokeLater(() -> {
        DataManager instance = DataManager.getInstance();
        DataContext context = instance != null ? instance.getDataContext(component) : DataContext.EMPTY_CONTEXT;
        InputEvent inputEvent = e != null ? e.getInputEvent() : null;
        AnActionEvent event = AnActionEvent.createFromAnAction(action, inputEvent, ActionPlaces.ACTION_SEARCH, context);
        if (inputEvent == null && modifiers != 0) {
          event = new AnActionEvent(null, event.getDataContext(), event.getPlace(), event.getPresentation(), event.getActionManager(), modifiers);
        }

        if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
          if (action instanceof ActionGroup && !((ActionGroup)action).canBePerformed(context)) {
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
              event.getPresentation().getText(), (ActionGroup)action, context, false, callback, -1);
            Window window = SwingUtilities.getWindowAncestor(component);
            if (window != null) {
              popup.showInCenterOf(window);
            }
            else {
              popup.showInFocusCenter();
            }
          }
          else {
            ActionManagerEx manager = ActionManagerEx.getInstanceEx();
            manager.fireBeforeActionPerformed(action, context, event);
            ActionUtil.performActionDumbAware(action, event);
            if (callback != null) callback.run();
            manager.fireAfterActionPerformed(action, context, event);
          }
        }
    });
  }

  @Override
  protected boolean requiresProject() {
    return false;
  }
}