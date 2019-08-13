/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class ExcludingActionGroup extends ActionGroup {
  private final ActionGroup myDelegate;
  private final Set<AnAction> myExcludes;

  public ExcludingActionGroup(ActionGroup delegate, Set<AnAction> excludes) {
    super(delegate.getTemplatePresentation().getText(), delegate.isPopup());
    myDelegate = delegate;
    myExcludes = excludes;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    myDelegate.update(e);
  }

  @Override
  public boolean isDumbAware() {
    return myDelegate.isDumbAware();
  }

  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    List<AnAction> result = new ArrayList<>();
    for (AnAction action : myDelegate.getChildren(e)) {
      if (myExcludes.contains(action)) {
        continue;
      }
      if (action instanceof ActionGroup) {
        result.add(new ExcludingActionGroup((ActionGroup) action, myExcludes));
      } else {
        result.add(action);
      }
    }
    return result.toArray(AnAction.EMPTY_ARRAY);
  }
}
