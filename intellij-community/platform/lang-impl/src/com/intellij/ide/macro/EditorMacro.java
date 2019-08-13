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
package com.intellij.ide.macro;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public abstract class EditorMacro extends Macro {
  private final String myName;
  private final String myDescription;

  public EditorMacro(String name, String description) {
    myName = name;
    myDescription = description;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public final String expand(DataContext dataContext) {
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor != null){
      return expand(editor);
    }
    return null;
  }

  /**
   * @return 1-based column index where tabs are treated as single characters. External tools don't know about IDEA's tab size.
   */
  protected static String getColumnNumber(Editor editor, LogicalPosition pos) {
    if (EditorUtil.inVirtualSpace(editor, pos)) {
      return String.valueOf(pos.column + 1);
    }

    int offset = editor.logicalPositionToOffset(pos);
    int lineStart = editor.getDocument().getLineStartOffset(editor.getDocument().getLineNumber(offset));
    return String.valueOf(offset - lineStart + 1);
  }

  @Nullable
  protected abstract String expand(Editor editor);
}
