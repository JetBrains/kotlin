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

package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.layout.impl.RunnerContentUi;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class RestoreViewAction extends DumbAwareToggleAction {

  private final RunnerContentUi myUi;
  private final Content myContent;

  public RestoreViewAction(@NotNull RunnerContentUi ui, @NotNull Content content) {
    myUi = ui;
    myContent = content;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return myContent.isValid() && myContent.getManager().getIndexOfContent(myContent) != -1;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    if (state) {
      myUi.restore(myContent);
    } else {
      myUi.minimize(myContent, null);
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(myContent.getDisplayName());
  }

  public Content getContent() {
    return myContent;
  }
}
