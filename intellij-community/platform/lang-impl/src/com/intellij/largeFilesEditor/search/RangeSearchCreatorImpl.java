// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.largeFilesEditor.editor.EditorManagerAccessor;
import com.intellij.largeFilesEditor.editor.EditorManagerAccessorImpl;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearch;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageViewContentManager;
import org.jetbrains.annotations.NotNull;

public class RangeSearchCreatorImpl implements RangeSearchCreator {

  @NotNull
  @Override
  public RangeSearch createContent(Project project,
                                   VirtualFile virtualFile,
                                   String titleName,
                                   FileDataProviderForSearch fileDataProviderForSearch) {
    EditorManagerAccessor editorManagerAccessor = new EditorManagerAccessorImpl();
    RangeSearch rangeSearch = new RangeSearch(
      virtualFile, project, editorManagerAccessor, fileDataProviderForSearch);
    Content content = UsageViewContentManager.getInstance(project).addContent(
      titleName, true, rangeSearch.getComponent(), false, true);
    rangeSearch.setContent(content);

    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND).activate(null, true);

    content.setDisposer(new Disposable() {
      @Override
      public void dispose() {
        rangeSearch.dispose();
      }
    });

    return rangeSearch;
  }

}
