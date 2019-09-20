// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearch;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface RangeSearchCreator {

  @NotNull
  RangeSearch createContent(Project project,
                            VirtualFile virtualFile,
                            String titleName);
}
