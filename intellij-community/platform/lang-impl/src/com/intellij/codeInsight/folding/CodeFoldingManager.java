// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CodeFoldingManager {
  public static CodeFoldingManager getInstance(Project project) {
    return project.getService(CodeFoldingManager.class);
  }

  public abstract void updateFoldRegions(@NotNull Editor editor);

  @Nullable
  public abstract Runnable updateFoldRegionsAsync(@NotNull Editor editor, boolean firstTime);

  @Nullable
  public abstract FoldRegion findFoldRegion(@NotNull Editor editor, int startOffset, int endOffset);

  public abstract FoldRegion[] getFoldRegionsAtOffset(@NotNull Editor editor, int offset);

  public abstract CodeFoldingState saveFoldingState(@NotNull Editor editor);

  public abstract void restoreFoldingState(@NotNull Editor editor, @NotNull CodeFoldingState state);

  public abstract void writeFoldingState(@NotNull CodeFoldingState state, @NotNull Element element);

  public abstract CodeFoldingState readFoldingState(@NotNull Element element, @NotNull Document document);

  public abstract void releaseFoldings(@NotNull Editor editor);

  public abstract void buildInitialFoldings(@NotNull Editor editor);

  @Nullable
  public abstract CodeFoldingState buildInitialFoldings(@NotNull Document document);

  /**
   * For auto-generated regions (created by {@link com.intellij.lang.folding.FoldingBuilder}s), returns their 'collapsed by default'
   * status, for other regions returns {@code null}.
   */
  @Nullable
  public abstract Boolean isCollapsedByDefault(@NotNull FoldRegion region);

  /**
   * Schedules recalculation of foldings in editor ({@link com.intellij.codeInsight.daemon.impl.CodeFoldingPass CodeFoldingPass}), which
   * will happen even if document (and other dependencies declared by {@link com.intellij.lang.folding.FoldingBuilder FoldingBuilder})
   * haven't changed.
   */
  public abstract void scheduleAsyncFoldingUpdate(@NotNull Editor editor);
}
