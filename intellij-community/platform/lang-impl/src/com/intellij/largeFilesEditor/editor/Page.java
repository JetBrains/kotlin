// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

public class Page {
  private final String text;
  private final long pageNumber;
  private final boolean isLastInFile;

  public Page(String text, long pageNumber, boolean isLastInFile) {
    this.text = text;
    this.pageNumber = pageNumber;
    this.isLastInFile = isLastInFile;
  }

  public String getText() {
    return text;
  }

  public long getPageNumber() {
    return pageNumber;
  }

  public boolean isLastInFile() {
    return isLastInFile;
  }
}
