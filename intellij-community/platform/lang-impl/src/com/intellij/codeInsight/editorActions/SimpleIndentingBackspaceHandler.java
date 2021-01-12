/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.psi.PsiFile;

public class SimpleIndentingBackspaceHandler extends AbstractIndentingBackspaceHandler {
  private LogicalPosition myTargetPosition;

  public SimpleIndentingBackspaceHandler() {
    super(SmartBackspaceMode.INDENT);
  }

  @Override
  protected void doBeforeCharDeleted(char c, PsiFile file, Editor editor) {
    myTargetPosition = BackspaceHandler.getBackspaceUnindentPosition(file, editor);
  }

  @Override
  protected boolean doCharDeleted(char c, PsiFile file, Editor editor) {
    if (myTargetPosition != null) {
      BackspaceHandler.deleteToTargetPosition(editor, myTargetPosition);
      return true;
    }
    return false;
  }
}
