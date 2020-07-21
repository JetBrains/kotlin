// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.actions;

import com.intellij.openapi.actionSystem.IdeActions;

public class CompileFileAction extends CompileAction {
  private final static String RECOMPILE_FILES_ID_MOD = IdeActions.ACTION_COMPILE + "File";

  public CompileFileAction() {
    super(true, RECOMPILE_FILES_ID_MOD);
  }
}
