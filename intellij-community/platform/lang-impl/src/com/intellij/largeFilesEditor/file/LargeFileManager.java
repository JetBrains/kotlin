// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.Disposable;

import java.io.IOException;
import java.nio.charset.Charset;

public interface LargeFileManager extends Disposable{

  void reset(Charset charset);

  String getCharsetName();

  long getPagesAmount() throws IOException;

  int getPageSize();

  Page getPage_wait(long pageNumber) throws IOException;

  boolean hasBOM();

  String getFileName();

  FileDataProviderForSearch getFileDataProviderForSearch();

  void requestReadPage(long pageNumber, ReadingPageResultHandler readingPageResultHandler);

  void addFileChangeListener(FileChangeListener listener);
}
