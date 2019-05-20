// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConsoleHistoryModel extends ConsoleHistoryBaseModel {
  @Nullable
  Entry getHistoryNext();

  @Nullable
  Entry getHistoryPrev();

  boolean hasHistory();

  int getCurrentIndex();

  void setContent(@NotNull String userContent);

  /* if true then overrides the navigation behavior such that the down key on last line always navigates to prev instead of only when there
     are no more characters in from of the caret
   */
  default boolean prevOnLastLine() {
    return false;
  }

  final class Entry {
    private final CharSequence text;
    private final int offset;

    public Entry(CharSequence text, int offset) {
      this.text = text;
      this.offset = offset;
    }

    public CharSequence getText() {
      return text;
    }

    public int getOffset() {
      return offset;
    }
  }
}
