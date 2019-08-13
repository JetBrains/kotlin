/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.actions.BaseViewAction;
import com.intellij.execution.ui.layout.ViewContext;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

public abstract class CloseViewsActionBase extends BaseViewAction {
  @Override
  protected void update(AnActionEvent e, ViewContext context, Content[] content) {
    e.getPresentation().setEnabledAndVisible(isEnabled(context, content, e.getPlace()));
  }

  @Override
  protected void actionPerformed(AnActionEvent e, ViewContext context, Content[] content) {
    ContentManager manager = context.getContentManager();
    for (Content c : manager.getContents()) {
      if (c.isCloseable() && isAccepted(c, content)) {
        manager.removeContent(c, context.isToDisposeRemovedContent());
      }
    }
  }

  public boolean isEnabled(ViewContext context, Content[] selectedContents, String place) {
    for (Content c : context.getContentManager().getContents()) {
      if (c.isCloseable() && isAccepted(c, selectedContents)) return true;
    }
    return false;
  }

  protected abstract boolean isAccepted(@NotNull Content c, @NotNull Content[] selectedContents);
}