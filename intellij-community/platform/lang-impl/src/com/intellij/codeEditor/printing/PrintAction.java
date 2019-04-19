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

package com.intellij.codeEditor.printing;

import com.intellij.ide.actions.PrintActionHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class PrintAction extends DumbAwareAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PrintActionHandler handler = PrintActionHandler.getHandler(e.getDataContext());
    if (handler == null) return;
    handler.print(e.getDataContext());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    PrintActionHandler handler = PrintActionHandler.getHandler(e.getDataContext());
    e.getPresentation().setEnabled(handler != null);
  }
}