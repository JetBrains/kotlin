/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.openapi.editor.richcopy;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class CopyAsRichTextAction extends DumbAwareAction {
  public CopyAsRichTextAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation p = e.getPresentation();
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    p.setVisible(!RichCopySettings.getInstance().isEnabled() &&
                 (e.isFromActionToolbar() || (editor != null && editor.getSelectionModel().hasSelection(true))) &&
                 (editor == null || isRichCopyPossible(editor)));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    RichCopySettings settings = RichCopySettings.getInstance();
    boolean savedValue = settings.isEnabled();
    try {
      settings.setEnabled(true);
      ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_COPY).actionPerformed(e);
    }
    finally {
      settings.setEnabled(savedValue);
    }
  }

  static boolean isRichCopyPossible(@NotNull Editor editor) {
    // ideally, we'd also want to check for the presence of PsiFile (CopyHandler won't work without it), but it might be more expensive
    return FileDocumentManager.getInstance().getFile(editor.getDocument()) != null;
  }
}
