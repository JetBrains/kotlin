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
package com.intellij.codeInsight.folding.impl.actions;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.folding.impl.FoldingUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class BaseExpandToLevelAction extends EditorAction {
  protected BaseExpandToLevelAction(final int level, final boolean expandAll) {
    super(new BaseFoldingHandler() {
      @Override
      protected void doExecute(@NotNull final Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert editor.getProject() != null;
        CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(editor.getProject());
        foldingManager.updateFoldRegions(editor);

        if (caret == null) {
          caret = editor.getCaretModel().getPrimaryCaret();
        }
        int offset = caret.getOffset();
        FoldRegion rootRegion = null;
        if (!expandAll) {
          rootRegion = FoldingUtil.findFoldRegionStartingAtLine(editor, editor.getDocument().getLineNumber(offset));
          if (rootRegion == null) {
            FoldRegion[] regions = FoldingUtil.getFoldRegionsAtOffset(editor, offset);
            if (regions.length > 0) {
              rootRegion = regions[0];
            }
          }
          if (rootRegion == null) {
            return;
          }
        }
        final FoldRegion root = rootRegion;
        final int[] rootLevel = new int[] {root == null ? 1 : -1};

        editor.getFoldingModel().runBatchFoldingOperation(() -> {
          Iterator<FoldRegion> regionTreeIterator = FoldingUtil.createFoldTreeIterator(editor);
          Deque<FoldRegion> currentStack = new LinkedList<>();
          while (regionTreeIterator.hasNext()) {
            FoldRegion region = regionTreeIterator.next();
            while (!currentStack.isEmpty() && !isChild(currentStack.peek(), region)) {
              if (currentStack.remove() == root) {
                rootLevel[0] = -1;
              }
            }
            currentStack.push(region);
            int currentLevel = currentStack.size();

            if (region == root) {
              rootLevel[0] = currentLevel;
            }
            if (rootLevel[0] >= 0) {
              int relativeLevel = currentLevel - rootLevel[0];

              if (relativeLevel < level) {
                region.setExpanded(true);
              }
              else if (relativeLevel == level) {
                region.setExpanded(false);
              }
            }
          }
        });
      }
    });
  }

  private static boolean isChild(@NotNull FoldRegion parent, @NotNull FoldRegion child) {
    return child.getStartOffset() >= parent.getStartOffset() && child.getEndOffset() <= parent.getEndOffset();
  }
}
