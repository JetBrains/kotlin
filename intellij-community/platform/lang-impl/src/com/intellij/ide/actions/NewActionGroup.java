/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 */
public class NewActionGroup extends ActionGroup {
  private static final String PROJECT_OR_MODULE_GROUP_ID = "NewProjectOrModuleGroup";

  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    AnAction[] actions = ((ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_WEIGHING_NEW)).getChildren(e);
    if (e == null || ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
      AnAction newGroup = ActionManager.getInstance().getAction(PROJECT_OR_MODULE_GROUP_ID);
      if (newGroup != null) {
        AnAction[] newProjectActions = ((ActionGroup)newGroup).getChildren(e);
        if (newProjectActions.length > 0) {
          List<AnAction> mergedActions = new ArrayList<>(newProjectActions.length + 1 + actions.length);
          Collections.addAll(mergedActions, newProjectActions);
          mergedActions.add(Separator.getInstance());
          Collections.addAll(mergedActions, actions);
          return mergedActions.toArray(AnAction.EMPTY_ARRAY);
        }
      }
    }
    return actions;
  }

  public static boolean isActionInNewPopupMenu(@NotNull AnAction action) {
    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup fileGroup = (ActionGroup)actionManager.getAction(IdeActions.GROUP_FILE);
    if (!ActionUtil.anyActionFromGroupMatches(fileGroup, false, child -> child instanceof NewActionGroup)) return false;

    AnAction newProjectOrModuleGroup = ActionManager.getInstance().getAction(PROJECT_OR_MODULE_GROUP_ID);
    if (newProjectOrModuleGroup instanceof ActionGroup
        && ActionUtil.anyActionFromGroupMatches((ActionGroup)newProjectOrModuleGroup, false,Predicate.isEqual(action))) {
      return true;
    }

    ActionGroup newGroup = (ActionGroup)actionManager.getAction(IdeActions.GROUP_NEW);
    return ActionUtil.anyActionFromGroupMatches(newGroup, false, Predicate.isEqual(action));
  }
}
