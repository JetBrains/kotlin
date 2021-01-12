// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import org.jetbrains.annotations.NotNull;

public class AbsoluteEditorPosition {

  long pageNumber;
  int verticalScrollOffset;

  public AbsoluteEditorPosition(long pageNumber, int verticalScrollOffset) {
    this.pageNumber = pageNumber;
    this.verticalScrollOffset = verticalScrollOffset;
  }

  void set(long newPageNumber, int newVerticalScrollBarOffset) {
    this.pageNumber = newPageNumber;
    this.verticalScrollOffset = newVerticalScrollBarOffset;
  }

  void copyFrom(@NotNull AbsoluteEditorPosition from) {
    pageNumber = from.pageNumber;
    verticalScrollOffset = from.verticalScrollOffset;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof AbsoluteEditorPosition
           && pageNumber == ((AbsoluteEditorPosition)obj).pageNumber
           && verticalScrollOffset == ((AbsoluteEditorPosition)obj).verticalScrollOffset;
  }
}
