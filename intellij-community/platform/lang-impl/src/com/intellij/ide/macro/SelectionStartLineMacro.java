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
package com.intellij.ide.macro;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;

/**
 * @author yole
 */
public class SelectionStartLineMacro extends EditorMacro {
  public SelectionStartLineMacro() {
    super("SelectionStartLine", "Selected text start line number");
  }

  @Override
  protected String expand(Editor editor) {
    VisualPosition selectionStartPosition = editor.getSelectionModel().getSelectionStartPosition();
    if (selectionStartPosition == null) {
      return null;
    }
    return String.valueOf(editor.visualToLogicalPosition(selectionStartPosition).line + 1);
  }
}
