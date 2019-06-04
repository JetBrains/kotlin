// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchTask;

import com.intellij.largeFilesEditor.editor.Page;

import java.io.IOException;

public interface FileDataProviderForSearch {

  long getPagesAmount() throws IOException;

  Page getPage_wait(long pageNumber) throws IOException;

  String getName();
}
