package com.intellij.execution.console;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class GutterContentProvider {
  protected static final int MAX_LINE_END_GUTTER_WIDTH_IN_CHAR = 2;

  public void beforeUiComponentUpdate(@NotNull Editor editor) {
  }

  public void documentCleared(@NotNull Editor editor) {
  }

  public void beforeEvaluate(@NotNull Editor editor) {
  }

  public abstract boolean hasText();

  @Nullable
  public abstract String getText(int line, @NotNull Editor editor);

  @Nullable
  public abstract String getToolTip(int line, @NotNull Editor editor);

  public abstract void doAction(int line, @NotNull Editor editor);

  public abstract boolean drawIcon(int line, @NotNull Graphics g, int y, @NotNull Editor editor);

  public boolean isShowSeparatorLine(int line, @NotNull Editor editor) {
    return true;
  }

  public int getLineStartGutterOverlap(@NotNull Editor editor) {
    return 0;
  }
}