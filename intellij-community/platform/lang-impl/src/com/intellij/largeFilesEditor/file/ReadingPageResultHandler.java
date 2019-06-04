// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.largeFilesEditor.editor.Page;

public interface ReadingPageResultHandler {
  void run(Page readPage);
}
