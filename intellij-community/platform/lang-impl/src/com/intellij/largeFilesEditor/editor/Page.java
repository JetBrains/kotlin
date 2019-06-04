// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

public class Page {
  private final String text;
  private final long pageNumber;


  public Page(String text, long pageNumber) {
    this.text = text;
    this.pageNumber = pageNumber;
  }

  public Page(long pageNumber) {
    this("", pageNumber);
  }

  public String getText() {
    return text;
  }

  public long getPageNumber() {
    return pageNumber;
  }
}
