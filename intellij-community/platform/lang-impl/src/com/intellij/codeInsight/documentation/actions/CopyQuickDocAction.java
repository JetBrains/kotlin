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
package com.intellij.codeInsight.documentation.actions;

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;

/**
 * @author Denis Zhdanov
 */
public class CopyQuickDocAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

  public CopyQuickDocAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    String selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT);
    if (selected == null || selected.isEmpty()) {
      return;
    }

    CopyPasteManager.getInstance().setContents(new StringSelection(selected));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    String selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT);
    e.getPresentation().setEnabled(selected != null && !selected.isEmpty());
  }
}
