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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actions.CopyAction;

public class CopyAsPlainTextAction extends EditorAction {
  public CopyAsPlainTextAction() {
    super(new CopyAction.Handler());
  }

  @Override
  public void update(Editor editor, Presentation presentation, DataContext dataContext) {
    super.update(editor, presentation, dataContext);
    presentation.setVisible(editor.getSelectionModel().hasSelection(true) && CopyAsRichTextAction.isRichCopyPossible(editor));
  }
}
