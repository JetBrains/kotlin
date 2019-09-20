/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInsight.daemon.impl.actions;

/**
 * Action moves caret to the next highlighted element under caret.
 *
 * Please note, it works only if option "Highlight usages of element at caret" turned on.
 * @see com.intellij.codeInsight.CodeInsightSettings#HIGHLIGHT_IDENTIFIER_UNDER_CARET highlight usages
 */
public class GotoNextElementUnderCaretUsageAction extends GotoElementUnderCaretUsageBase {
  public GotoNextElementUnderCaretUsageAction() {
    super(Direction.FORWARD);
  }
}
