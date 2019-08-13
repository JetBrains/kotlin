// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

public class Position {
  public long pageNumber;
  public int symbolOffsetInPage;

  public Position() {
    this.pageNumber = 0;
    this.symbolOffsetInPage = 0;
  }

  public Position(long pageNumber, int symbolOffsetInPage) {
    this.pageNumber = pageNumber;
    this.symbolOffsetInPage = symbolOffsetInPage;
  }

  public void reset(long pageNumber, int symbolNumber) {
    this.pageNumber = pageNumber;
    this.symbolOffsetInPage = symbolNumber;
  }

  @Override
  public boolean equals(Object target) {
    if (this == target) {
      return true;
    }

    if (target instanceof Position) {
      if (pageNumber == ((Position)target).pageNumber
          && symbolOffsetInPage == ((Position)target).symbolOffsetInPage) {
        return true;
      }
    }
    return false;
  }
}