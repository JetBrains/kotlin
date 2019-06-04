// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.openapi.editor.actionSystem.EditorActionHandler;

public class LfeEditorActionSearchBackHandler extends LfeEditorActionSearchAgainHandler {

  public LfeEditorActionSearchBackHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected boolean isForwardDirection() {
    return false;
  }
}
