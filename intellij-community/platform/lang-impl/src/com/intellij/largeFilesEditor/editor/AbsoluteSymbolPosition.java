// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import org.jetbrains.annotations.NotNull;

class AbsoluteSymbolPosition {

  long pageNumber;
  int symbolOffsetInPage;

  AbsoluteSymbolPosition(long pageNumber, int symbolOffsetInPage) {
    this.pageNumber = pageNumber;
    this.symbolOffsetInPage = symbolOffsetInPage;
  }

  void set(@NotNull AbsoluteSymbolPosition from) {
    set(from.pageNumber, from.symbolOffsetInPage);
  }

  void set(long pageNumber, int symbolOffsetInPage) {
    this.pageNumber = pageNumber;
    this.symbolOffsetInPage = symbolOffsetInPage;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AbsoluteSymbolPosition) {
      AbsoluteSymbolPosition other = (AbsoluteSymbolPosition)obj;
      if (pageNumber == other.pageNumber && symbolOffsetInPage == other.symbolOffsetInPage) {
        return true;
      }
    }
    return false;
  }

  boolean isLessOrEqualsThen(AbsoluteSymbolPosition other) {
    return !isMoreThen(other);
  }

  boolean isMoreOrEqualsThen(AbsoluteSymbolPosition other) {
    return !isLessThen(other);
  }

  boolean isLessThen(AbsoluteSymbolPosition other) {
    if (other == null) return false;

    return pageNumber < other.pageNumber
           || pageNumber == other.pageNumber
              && symbolOffsetInPage < other.symbolOffsetInPage;
  }

  boolean isMoreThen(AbsoluteSymbolPosition other) {
    if (other == null) return false;

    return pageNumber > other.pageNumber
           || pageNumber == other.pageNumber
              && symbolOffsetInPage > other.symbolOffsetInPage;
  }

  @Override
  public String toString() {
    return "("+pageNumber+","+symbolOffsetInPage+")";
  }
}
