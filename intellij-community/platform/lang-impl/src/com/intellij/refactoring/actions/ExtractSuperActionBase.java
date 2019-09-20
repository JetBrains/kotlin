/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public abstract class ExtractSuperActionBase extends BasePlatformRefactoringAction {

  @Override
  public boolean isAvailableInEditorOnly() {
    return false;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    removeFirstWordInMainMenu(this, e);
  }

  public static void removeFirstWordInMainMenu(AnAction action, @NotNull AnActionEvent e) {
    if (ActionPlaces.MAIN_MENU.equals(e.getPlace())) {
      String templateText = action.getTemplatePresentation().getText();
      if (templateText.startsWith("Extract") || templateText.startsWith("Introduce")) {
        e.getPresentation().setText(templateText.substring(templateText.indexOf(' ') + 1));
      }
    }
  }
}