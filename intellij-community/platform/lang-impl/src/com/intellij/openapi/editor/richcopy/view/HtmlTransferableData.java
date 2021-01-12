// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.richcopy.view;

import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;

public class HtmlTransferableData extends HtmlSyntaxInfoReader implements RawTextWithMarkup {
  @NotNull public static final DataFlavor FLAVOR = new DataFlavor("text/html; class=java.io.Reader; charset=UTF-8", "HTML text");
  public static final int PRIORITY = 200;

  public HtmlTransferableData(@NotNull SyntaxInfo syntaxInfo, int tabSize) {
    super(syntaxInfo, tabSize);
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
  }

  @Override
  public int getOffsetCount() {
    return 0;
  }

  @Override
  public int getOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int setOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int getPriority() {
    return PRIORITY;
  }
}
