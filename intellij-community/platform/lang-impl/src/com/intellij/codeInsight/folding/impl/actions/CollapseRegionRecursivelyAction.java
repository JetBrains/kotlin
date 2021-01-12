// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class CollapseRegionRecursivelyAction extends EditorAction {
  public CollapseRegionRecursivelyAction() {
    super(new BaseFoldingHandler() {
      @Override
      public void doExecute(@NotNull final Editor editor, @Nullable Caret caret, DataContext dataContext) {
        final List<FoldRegion> regions = getFoldRegionsForCaret(editor, caret, true);
        editor.getFoldingModel().runBatchFoldingOperation(() -> {
          for (FoldRegion region : regions) {
            region.setExpanded(false);
          }
        });
      }
    });
  }
}

