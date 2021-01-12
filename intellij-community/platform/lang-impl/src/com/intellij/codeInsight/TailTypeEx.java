// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NonNls;

/**
 * @author yole
 */
public final class TailTypeEx {
  public static final TailType SMART_LPARENTH = new TailType() {
    @Override
    public int processTail(final Editor editor, int tailOffset) {
      tailOffset = insertChar(editor, tailOffset, '(');
      return moveCaret(editor, insertChar(editor, tailOffset, ')'), -1);
    }

    @NonNls
    public String toString() {
      return "SMART_LPARENTH";
    }
  };

  private TailTypeEx() {
  }
}
