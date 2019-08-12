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

package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PromptMacro extends PromptingMacro implements SecondQueueExpandMacro {
  @NotNull
  @Override
  public String getName() {
    return "Prompt";
  }

  @NotNull
  @Override
  public String getDescription() {
    return IdeBundle.message("macro.prompt");
  }

  @Override
  @Nullable
  protected String promptUser(DataContext dataContext) {
    return promptUser();
  }

  public String promptUser() {
    return Messages.showInputDialog(IdeBundle.message("prompt.enter.parameters"), IdeBundle.message("title.input"), Messages.getQuestionIcon());
  }

  @Override
  public void cachePreview(@NotNull DataContext dataContext) {
    myCachedPreview = IdeBundle.message("macro.prompt.preview");
  }
}
