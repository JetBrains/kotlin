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
package com.intellij.codeInsight.folding.impl.actions;

import com.intellij.codeInsight.folding.impl.FoldingUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseFoldingHandler extends EditorActionHandler {
  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return editor.getProject() != null;
  }

  /**
   * Returns fold regions inside selection, or all regions in editor, if selection doesn't exist or doesn't contain fold regions.
   */
  protected List<FoldRegion> getFoldRegionsForSelection(@NotNull Editor editor, @Nullable Caret caret) {
    FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
    if (caret == null) {
      caret = editor.getCaretModel().getPrimaryCaret();
    }
    if (caret.hasSelection()) {
      List<FoldRegion> result = new ArrayList<>();
      for (FoldRegion region : allRegions) {
        if (region.getStartOffset() >= caret.getSelectionStart() && region.getEndOffset() <= caret.getSelectionEnd()) {
          result.add(region);
        }
      }
      if (!result.isEmpty()) {
        return result;
      }
    }
    return Arrays.asList(allRegions);
  }

  /**
   * Returns a region corresponding to current caret position, and all regions contained in it.
   */
  protected List<FoldRegion> getFoldRegionsForCaret(@NotNull Editor editor, @Nullable Caret caret, boolean toCollapse) {
    if (caret == null) {
      caret = editor.getCaretModel().getPrimaryCaret();
    }
    int offset = caret.getOffset();
    FoldRegion rootRegion = FoldingUtil.findFoldRegionStartingAtLine(editor, editor.getDocument().getLineNumber(offset));
    if (rootRegion == null || toCollapse && !rootRegion.isExpanded()) {
      rootRegion = null;
      FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, offset);
      for (FoldRegion region : regions) {
        if (region.isExpanded() == toCollapse) {
          rootRegion = region;
          break;
        }
      }
    }
    List<FoldRegion> result = new ArrayList<>();
    if (rootRegion != null) {
      FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
      for (FoldRegion region : allRegions) {
        if (region.getStartOffset() >= rootRegion.getStartOffset() && region.getEndOffset() <= rootRegion.getEndOffset()) {
          result.add(region);
        }
      }
    }
    return result;
  }
}
