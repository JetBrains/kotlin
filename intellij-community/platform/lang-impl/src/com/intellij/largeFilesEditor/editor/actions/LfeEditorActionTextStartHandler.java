// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor.actions;

import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class LfeEditorActionTextStartHandler extends LfeEditorActionTextStartEndHandler {

  public LfeEditorActionTextStartHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected boolean isStart() {
    return true;
  }
}
