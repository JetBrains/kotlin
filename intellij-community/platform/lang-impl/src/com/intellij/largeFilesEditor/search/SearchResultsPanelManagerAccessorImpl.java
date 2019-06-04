// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.icons.AllIcons;
import com.intellij.largeFilesEditor.editor.EditorManagerAccessor;
import com.intellij.largeFilesEditor.editor.EditorManagerAccessorImpl;
import com.intellij.largeFilesEditor.search.searchResultsPanel.SearchResultsToolWindow;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SearchResultsPanelManagerAccessorImpl implements SearchResultsPanelManagerAccessor {

  private static final Logger logger = Logger.getInstance(SearchResultsPanelManagerAccessorImpl.class);
  private static final String TOOLWINDOW_ID = "lfe.toolWindow.searchResults";
  private static final String TOOLWINDOW_STRIPE_TITLE = "Find in Large File";

  @Override
  public SearchResultsToolWindow getSearchResultsToolWindow(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOLWINDOW_ID);
    if (toolWindow == null) {
      if (!createIfNotExists) {
        return null;
      }
      toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(
        TOOLWINDOW_ID, true, ToolWindowAnchor.BOTTOM,
        project, true);
      toolWindow.setStripeTitle(TOOLWINDOW_STRIPE_TITLE);
      toolWindow.setToHideOnEmptyContent(true);
      toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowFind);
    }

    Content neededContent = tryGetNeededContentFromRangeSearchToolWindow(toolWindow, virtualFile);
    if (neededContent == null) {
      if (!createIfNotExists) {
        return null;
      }
      EditorManagerAccessor editorManagerAccessor = new EditorManagerAccessorImpl();
      SearchResultsToolWindow searchResultsToolWindow = new SearchResultsToolWindow(
        virtualFile, project, editorManagerAccessor);
      JComponent toolWindowTabContent = searchResultsToolWindow.getComponent();
      String tabName = virtualFile.getName();
      neededContent = ContentFactory.SERVICE.getInstance().createContent(
        toolWindowTabContent, tabName, true);
      neededContent.putUserData(SearchResultsToolWindow.KEY, searchResultsToolWindow);
      neededContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE); // For showing icon when pinned.

      toolWindow.getContentManager().addContent(neededContent);
      searchResultsToolWindow.setRelativeTab(neededContent);

      ContentManager contentManager = toolWindow.getContentManager();
      contentManager.addContentManagerListener(new ContentManagerAdapter() {
        @Override
        public void contentRemoved(@NotNull ContentManagerEvent event) {
          searchResultsToolWindow.tellWasClosed();
        }
      });
      //new ContentManagerWatcher(toolWindow, contentManager);  // for listener working

      return searchResultsToolWindow;
    }

    SearchResultsToolWindow searchResultsToolWindow = neededContent.getUserData(SearchResultsToolWindow.KEY);
    if (searchResultsToolWindow != null) {
      return searchResultsToolWindow;
    }
    else {
      logger.warn("searchResultsToolWindow is null, but it shouldn't be");
      return null;
    }
  }

  @Override
  public void showSearchResultsToolWindow(@NotNull SearchResultsToolWindow searchResultsToolWindow) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(searchResultsToolWindow.getProject())
      .getToolWindow(TOOLWINDOW_ID);
    if (toolWindow != null) {
      toolWindow.show(null);
      Content neededContent = tryGetNeededContentFromRangeSearchToolWindow(toolWindow,
                                                                           searchResultsToolWindow.getVirtualFile());
      if (neededContent != null) {
        toolWindow.getContentManager().setSelectedContent(neededContent);
      }
    }
  }

  @Override
  public void closeToolWindowTab(VirtualFile virtualFile, Project project) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOLWINDOW_ID);

    if (toolWindow == null) {
      return;
    }

    ContentManager contentManager = toolWindow.getContentManager();
    Content[] contents = contentManager.getContents();
    Content neededContent = null;
    for (Content content : contents) {
      SearchResultsToolWindow connectedSearchResultsGUI = content.getUserData(SearchResultsToolWindow.KEY);
      if (connectedSearchResultsGUI == null) {
        logger.warn("connectedSearchResultsToolWindow is null, however it shouldn't.");
        continue;
      }
      if (connectedSearchResultsGUI.getVirtualFile().equals(virtualFile)) {
        neededContent = content;
        break;
      }
    }

    if (neededContent == null) {
      return;
    }

    contentManager.removeContent(neededContent, true);
  }

  private static Content tryGetNeededContentFromRangeSearchToolWindow(ToolWindow rangeSearchToolWindow,
                                                                      VirtualFile virtualFile) {
    ContentManager contentManager = rangeSearchToolWindow.getContentManager();
    Content[] contents = contentManager.getContents();
    for (Content content : contents) {
      SearchResultsToolWindow connectedSearchResultsToolWindow = content.getUserData(SearchResultsToolWindow.KEY);
      if (connectedSearchResultsToolWindow == null) {
        logger.warn("connectedSearchResultsToolWindow is null, however it shouldn't.");
        continue;
      }
      if (connectedSearchResultsToolWindow.getVirtualFile().equals(virtualFile)) {
        return content;
      }
    }
    return null;
  }
}
