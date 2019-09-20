// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class NewElementAction extends DumbAwareAction implements PopupAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    createPopup(e.getDataContext())
      .showInBestPositionFor(e.getDataContext());
  }

  @NotNull
  protected ListPopup createPopup(@NotNull DataContext dataContext) {
    return JBPopupFactory.getInstance().createActionGroupPopup(
      getPopupTitle(),
      getGroup(dataContext),
      dataContext,
      getActionSelectionAid(),
      isShowDisabledActions(),
      getDisposeCallback(),
      getMaxRowCount(),
      getPreselectActionCondition(dataContext),
      getPlace());
  }

  @Nullable
  protected JBPopupFactory.ActionSelectionAid getActionSelectionAid() {
    return null;
  }

  protected int getMaxRowCount() {
    return -1;
  }

  @Nullable
  protected Condition<AnAction> getPreselectActionCondition(DataContext dataContext) {
    return LangDataKeys.PRESELECT_NEW_ACTION_CONDITION.getData(dataContext);
  }

  @Nullable
  protected Runnable getDisposeCallback() {
    return null;
  }

  protected boolean isShowDisabledActions() {
    return false;
  }

  protected String getPopupTitle() {
    return IdeBundle.message("title.popup.new.element");
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    if (!isEnabled(e)) {
      presentation.setEnabled(false);
      return;
    }

    presentation.setEnabled(!ActionGroupUtil.isGroupEmpty(getGroup(e.getDataContext()), e, isEnabledInModalContext()));
  }

  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (Boolean.TRUE.equals(e.getData(LangDataKeys.NO_NEW_ACTION))) {
      return false;
    }
    return true;
  }

  protected ActionGroup getGroup(DataContext dataContext) {
    return (ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_WEIGHING_NEW);
  }

  @NotNull
  protected String getPlace() {
    return ActionPlaces.getActionGroupPopupPlace(IdeActions.GROUP_WEIGHING_NEW);
  }
}
