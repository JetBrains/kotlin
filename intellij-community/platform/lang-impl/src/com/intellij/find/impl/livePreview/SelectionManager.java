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
package com.intellij.find.impl.livePreview;

import com.intellij.find.FindResult;
import com.intellij.find.FindUtil;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectionManager {
  @NotNull private final SearchResults mySearchResults;
  private final boolean myHadSelectionInitially;
  private final List<FoldRegion> myRegionsToRestore = new ArrayList<>();

  public SelectionManager(@NotNull SearchResults results) {
    mySearchResults = results;
    myHadSelectionInitially = results.getEditor().getSelectionModel().hasSelection();
  }

  public void updateSelection(boolean removePreviousSelection, boolean removeAllPreviousSelections) {
    Editor editor = mySearchResults.getEditor();
    if (removeAllPreviousSelections) {
      editor.getCaretModel().removeSecondaryCarets();
    }
    final FindResult cursor = mySearchResults.getCursor();
    if (cursor == null) {
      if (removePreviousSelection && !myHadSelectionInitially) editor.getSelectionModel().removeSelection();
      return;
    }
    if (mySearchResults.getFindModel().isGlobal()) {
      if (removePreviousSelection || removeAllPreviousSelections) {
        FoldingModel foldingModel = editor.getFoldingModel();
        final FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();

        foldingModel.runBatchFoldingOperation(() -> {
          for (FoldRegion region : myRegionsToRestore) {
            if (region.isValid()) region.setExpanded(false);
          }
          myRegionsToRestore.clear();
          for (FoldRegion region : allRegions) {
            if (!region.isValid()) continue;
            if (cursor.intersects(TextRange.create(region))) {
              if (!region.isExpanded()) {
                region.setExpanded(true);
                myRegionsToRestore.add(region);
              }
            }
          }
        });
        editor.getSelectionModel().setSelection(cursor.getStartOffset(), cursor.getEndOffset());
        editor.getCaretModel().moveToOffset(cursor.getEndOffset());
      }
      else {
        FindUtil.selectSearchResultInEditor(editor, cursor, -1);
      }
      editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    } else {
      if (!SearchResults.insideVisibleArea(editor, cursor)) {
        LogicalPosition pos = editor.offsetToLogicalPosition(cursor.getStartOffset());
        editor.getScrollingModel().scrollTo(pos, ScrollType.CENTER);
      }
    }
  }

  public boolean removeCurrentSelection() {
    Editor editor = mySearchResults.getEditor();
    CaretModel caretModel = editor.getCaretModel();
    Caret primaryCaret = caretModel.getPrimaryCaret();
    if (caretModel.getCaretCount() > 1) {
      caretModel.removeCaret(primaryCaret);
      return true;
    }
    else {
      primaryCaret.moveToOffset(primaryCaret.getSelectionStart());
      primaryCaret.removeSelection();
      return false;
    }
  }

  public boolean isSelected(@NotNull FindResult result) {
    Editor editor = mySearchResults.getEditor();
    int endOffset = result.getEndOffset();
    for (Caret caret : editor.getCaretModel().getAllCarets()) {
      if (caret.getOffset() == endOffset) return true;
    }
    return false;
  }
}
