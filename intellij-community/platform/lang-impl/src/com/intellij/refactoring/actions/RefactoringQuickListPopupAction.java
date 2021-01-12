/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.actions;

import com.intellij.ide.actions.CopyElementAction;
import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RefactoringQuickListPopupAction extends QuickSwitchSchemeAction {

  public RefactoringQuickListPopupAction() {
    setInjectedContext(true);
  }

  @Override
  protected void fillActions(@Nullable final Project project,
                             @NotNull final DefaultActionGroup group,
                             @NotNull final DataContext dataContext) {
    if (project == null) {
      return;
    }

    final ActionManagerImpl actionManager = (ActionManagerImpl) ActionManager.getInstance();
    final AnAction action = actionManager.getAction(IdeActions.GROUP_REFACTOR);
    collectEnabledChildren(action, group, dataContext, actionManager, false);
  }

  private static void collectEnabledChildren(AnAction action,
                                             @NotNull DefaultActionGroup destinationGroup,
                                             @NotNull DataContext dataContext,
                                             @NotNull ActionManagerImpl actionManager,
                                             boolean popup) {
    if (action instanceof DefaultActionGroup) {
      final AnAction[] children = ((DefaultActionGroup)action).getChildren(null);
      for (AnAction child : children) {
        if (child instanceof DefaultActionGroup) {
          final boolean isPopup = ((DefaultActionGroup)child).isPopup();
          if (isPopup) {
            destinationGroup.add(new Separator(child.getTemplatePresentation().getText()));
          }
          collectEnabledChildren(child, destinationGroup, dataContext, actionManager, isPopup || popup);
          if (isPopup) {
            destinationGroup.add(Separator.getInstance());
          }
        } else if (child instanceof Separator && !popup) {
          destinationGroup.add(child);
        }
        else {
          if (isRefactoringAction(child, dataContext, actionManager)) {
            final Presentation presentation = new Presentation();
            final AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.REFACTORING_QUICKLIST, presentation, actionManager, 0);
            event.setInjectedContext(child.isInInjectedContext());
            child.update(event);
            if (presentation.isEnabled() && presentation.isVisible()) {
              destinationGroup.add(child);
            }
          }
        }
      }
    }
  }

  private static boolean isRefactoringAction(AnAction child, DataContext dataContext, ActionManagerImpl actionManager) {
    if (child instanceof BaseRefactoringAction && ((BaseRefactoringAction)child).hasAvailableHandler(dataContext) ||
        child instanceof CopyElementAction) {
      return true;
    }

    return child instanceof OverridingAction &&
           (actionManager.getBaseAction((OverridingAction)child) instanceof BaseRefactoringAction ||
            actionManager.getBaseAction((OverridingAction)child) instanceof CopyElementAction);
  }


  @Override
  protected void showPopup(AnActionEvent e, ListPopup popup) {
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor != null) {
      popup.showInBestPositionFor(editor);
    } else {
      super.showPopup(e, popup);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setVisible(
      ActionPlaces.isMainMenuOrActionSearch(e.getPlace())
      || ActionPlaces.ACTION_PLACE_QUICK_LIST_POPUP_ACTION.equals(e.getPlace())
      || ActionPlaces.TOUCHBAR_GENERAL.equals(e.getPlace())
    );
  }

  @Override
  protected String getPopupTitle(@NotNull AnActionEvent e) {
    return RefactoringBundle.message("refactor.this.title");
  }
}
