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
package com.intellij.refactoring.changeSignature.inplace;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;

public class EscapeHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public EscapeHandler(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  public void execute(@NotNull Editor editor, DataContext dataContext) {
    InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(editor);
    if (currentRefactoring != null) {
      currentRefactoring.cancel();
      return;
    }

    if (myOriginalHandler.isEnabled(editor, dataContext)) {
      myOriginalHandler.execute(editor, dataContext);
    }
  }

  @Override
  public boolean isEnabled(Editor editor, DataContext dataContext) {
    InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(editor);
    if (currentRefactoring != null) {
      return true;
    }
    return myOriginalHandler.isEnabled(editor, dataContext);
  }
}
