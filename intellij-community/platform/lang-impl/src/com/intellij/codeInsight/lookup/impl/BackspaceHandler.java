// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;

public class BackspaceHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public BackspaceHandler(EditorActionHandler originalHandler){
    myOriginalHandler = originalHandler;
  }

  @Override
  public void doExecute(@NotNull final Editor editor, Caret caret, final DataContext dataContext){
    LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(editor);
    if (lookup == null){
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    int hideOffset = lookup.getLookupStart();
    int originalStart = lookup.getLookupOriginalStart();
    if (originalStart >= 0 && originalStart <= hideOffset) {
      hideOffset = originalStart - 1;
    }
    
    truncatePrefix(dataContext, lookup, myOriginalHandler, hideOffset, caret);
  }

  static void truncatePrefix(final DataContext dataContext,
                             LookupImpl lookup,
                             final EditorActionHandler handler,
                             final int hideOffset,
                             final Caret caret) {
    final Editor editor = lookup.getEditor();
    if (!lookup.performGuardedChange(() -> handler.execute(editor, caret, dataContext))) {
      return;
    }

    final CompletionProgressIndicator process = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    lookup.truncatePrefix(process == null || !process.isAutopopupCompletion(), hideOffset);
  }
}
