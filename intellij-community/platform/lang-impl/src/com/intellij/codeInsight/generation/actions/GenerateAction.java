// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GenerateAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    Project project = ObjectUtils.assertNotNull(getEventProject(e));
    final ListPopup popup =
      JBPopupFactory.getInstance().createActionGroupPopup(
          CodeInsightBundle.message("generate.list.popup.title"),
                                                          wrapGroup(getGroup(), dataContext, project),
                                                          dataContext,
                                                          JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                          false);

    popup.showInBestPositionFor(dataContext);
  }

  @Override
  public void update(@NotNull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setEnabledAndVisible(isEnabled(event));
    }
    else {
      presentation.setEnabled(isEnabled(event));
    }
  }

  private static boolean isEnabled(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) {
      return false;
    }

    Editor editor = event.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      return false;
    }

    boolean groupEmpty = ActionGroupUtil.isGroupEmpty(getGroup(), event, LaterInvocator.isInModalContext());
    return !groupEmpty;
  }

  private static DefaultActionGroup getGroup() {
    return (DefaultActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_GENERATE);
  }

  private static DefaultActionGroup wrapGroup(DefaultActionGroup actionGroup, DataContext dataContext, @NotNull Project project) {
    final DefaultActionGroup copy = new DefaultActionGroup();
    for (final AnAction action : actionGroup.getChildren(null)) {
      if (DumbService.isDumb(project) && !action.isDumbAware()) {
        continue;
      }
      
      if (action instanceof GenerateActionPopupTemplateInjector) {
        final AnAction editTemplateAction = ((GenerateActionPopupTemplateInjector)action).createEditTemplateAction(dataContext);
        if (editTemplateAction != null) {
          copy.add(new GenerateWrappingGroup(action, editTemplateAction));
          continue;
        }
      }
      if (action instanceof DefaultActionGroup) {
        copy.add(wrapGroup((DefaultActionGroup)action, dataContext, project));
      }
      else {
        copy.add(action);
      }
    }
    return copy;
  }

  private static class GenerateWrappingGroup extends ActionGroup {

    private final AnAction myAction;
    private final AnAction myEditTemplateAction;

    GenerateWrappingGroup(AnAction action, AnAction editTemplateAction) {
      myAction = action;
      myEditTemplateAction = editTemplateAction;
      copyFrom(action);
      setPopup(true);
    }

    @Override
    public boolean canBePerformed(@NotNull DataContext context) {
      return true;
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return new AnAction[] {myEditTemplateAction};
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      DumbService.getInstance(Objects.requireNonNull(getEventProject(e)))
        .withAlternativeResolveEnabled(() -> myAction.actionPerformed(e));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      myAction.update(e);
    }
  }
}