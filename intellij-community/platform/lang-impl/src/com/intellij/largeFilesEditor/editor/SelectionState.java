// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

public class SelectionState {

  boolean isExists = false;
  final AbsoluteSymbolPosition start = new AbsoluteSymbolPosition(0, 0);
  final AbsoluteSymbolPosition end = new AbsoluteSymbolPosition(0, 0);


  void set(AbsoluteSymbolPosition startPosition, AbsoluteSymbolPosition endPosition) {
    setStart(startPosition);
    setEnd(endPosition);
  }

  void set(long startPageNumber, int startSymbolOffset, long endPageNumber, int endSymbolOffset) {
    setStart(startPageNumber, startSymbolOffset);
    setEnd(endPageNumber, endSymbolOffset);
  }

  void setStart(AbsoluteSymbolPosition startPosition) {
    start.set(startPosition);
  }

  void setStart(long startPageNumber, int startSymbolOffset) {
    start.set(startPageNumber, startSymbolOffset);
  }

  void setEnd(AbsoluteSymbolPosition endPosition) {
    end.set(endPosition);
  }

  void setEnd(long endPageNumber, int endSymbolOffset) {
    end.set(endPageNumber, endSymbolOffset);
  }
}
