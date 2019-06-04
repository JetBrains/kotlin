// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.largeFilesEditor.PlatformActionsReplacer;
import com.intellij.largeFilesEditor.encoding.EditorManagerAccess;
import com.intellij.largeFilesEditor.encoding.EditorManagerAccessorImpl;
import com.intellij.largeFilesEditor.encoding.EncodingWidget;
import com.intellij.largeFilesEditor.file.FileManager;
import com.intellij.largeFilesEditor.file.FileManagerImpl;
import com.intellij.largeFilesEditor.file.ReadingPageResultHandler;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.SearchManagerImpl;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.SearchResultsPanelManagerAccessorImpl;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class EditorManagerImpl extends UserDataHolderBase implements EditorManager {

  private static final Logger logger = Logger.getInstance(EditorManagerImpl.class);
  private final Project project;
  private FileManager fileManager;
  private final EditorModel editorModel;
  private final DocumentEx document;
  private final VirtualFile vFile;
  private SearchManager searchManager;

  public EditorManagerImpl(Project project, VirtualFile vFile) {
    this.vFile = vFile;
    this.project = project;

    int customPageSize = PropertiesGetter.getPageSize();
    int customBorderShift = PropertiesGetter.getMaxPageBorderShiftBytes();

    document = createSpecialDocument();

    editorModel = new EditorModel(document, project, implementDataProviderForEditorModel());

    try {
      fileManager = new FileManagerImpl(vFile, customPageSize, customBorderShift);
    }
    catch (FileNotFoundException e) {
      logger.warn(e);
      Messages.showWarningDialog("Can't open file: file not found.", "Warning");
      return;
    }

    editorModel.putUserDataToEditor(KEY_EDITOR_MARK, new Object());
    editorModel.putUserDataToEditor(KEY_EDITOR_MANAGER, this);

    searchManager = new SearchManagerImpl(
      this, fileManager.getFileDataProviderForSearch(), new SearchResultsPanelManagerAccessorImpl());

    createAndAddSpecialWidgetIfNeed(project);
    PlatformActionsReplacer.makeAdaptingOfPlatformActionsIfNeed();

    editorModel.addCaretListener(new MyCaretListener());
  }

  private void createAndAddSpecialWidgetIfNeed(Project project) {
    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    final StatusBarWidget existedWidget = statusBar.getWidget(EncodingWidget.WIDGET_ID);
    boolean needToAddNewWidget = false;
    if (existedWidget == null) {
      needToAddNewWidget = true;
    }
    else {
      if (existedWidget instanceof EncodingWidget &&
          ((EncodingWidget)existedWidget)._getProject() != project) {
        statusBar.removeWidget(existedWidget.ID());
        needToAddNewWidget = true;
      }
    }
    if (needToAddNewWidget) {
      statusBar.addWidget(new EncodingWidget(project, new EditorManagerAccessorImpl()));
    }
  }

  @Override
  public SearchManager getSearchManager() {
    return searchManager;
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return editorModel.getComponent();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return editorModel.getComponent();
  }

  @NotNull
  @Override
  public String getName() {
    return "Large File Editor";
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {
  }

  @Override
  public void deselectNotify() {
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Override
  public void dispose() {
    if (searchManager != null) {
      searchManager.dispose();
    }
    if (fileManager != null) {
      fileManager.dispose();
    }
    editorModel.dispose();
  }

  @CalledInAwt
  @Override
  public void showSearchResult(SearchResult searchResult) {
    editorModel.showSearchResult(searchResult);
  }

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public long getCaretPageNumber() {
    return editorModel.getCaretPageNumber();
  }

  @Override
  public int getCaretPageOffset() {
    return editorModel.getCaretPageOffset();
  }

  @Override
  public Editor getEditor() {
    return editorModel.getEditor();
  }

  @Nullable
  @Override
  public VirtualFile getFile() {
    return getVirtualFile();
  }

  @NotNull
  @Override
  public VirtualFile getVirtualFile() {
    return vFile;
  }

  @Override
  public EditorManagerAccess createAccessForEncodingWidget() {
    return new EditorManagerAccess() {
      @NotNull
      @Override
      public VirtualFile getVirtualFile() {
        return EditorManagerImpl.this.getVirtualFile();
      }

      @NotNull
      @Override
      public Editor getEditor() {
        return EditorManagerImpl.this.getEditor();
      }

      @Override
      public boolean tryChangeEncoding(@NotNull Charset charset) {

        if (fileManager.hasBOM()) {
          Messages.showWarningDialog("Can't change file encoding, because it has BOM (Byte order mark)", "Warning");
          return false;
        }

        if (searchManager.isSearchWorkingNow()) {
          Messages.showInfoMessage("Can't change file encoding, because search is working now.", "Can't Change Encoding");
          return false;
        }

        fileManager.reset(charset);
        editorModel.fireEncodingWasChanged();
        return true;
      }

      @Override
      public String getCharsetName() {
        return fileManager.getCharsetName();
      }
    };
  }

  @Override
  public FileDataProviderForSearch getFileDataProviderForSearch() {
    return fileManager.getFileDataProviderForSearch();
  }

  @NotNull
  @Override
  public EditorModel getEditorModel() {
    return editorModel;
  }

  private static DocumentEx createSpecialDocument() {
    DocumentEx doc = new DocumentImpl("", false, false); // restrict "\r\n" line separators
    UndoUtil.disableUndoFor(doc); // disabling Undo-functionality, provided by IDEA
    return doc;
  }

  private class MyCaretListener implements CaretListener {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      searchManager.onCaretPositionChanged(e);
    }
  }

  private EditorModel.DataProvider implementDataProviderForEditorModel() {
    return new EditorModel.DataProvider() {
      @Override
      public Page getPage(long pageNumber) throws IOException {
        return fileManager.getPage_wait(pageNumber);
      }

      @Override
      public long getPagesAmount() throws IOException {
        return fileManager.getPagesAmount();
      }

      @Override
      public Project getProject() {
        return project;
      }

      @Override
      public void requestReadPage(long pageNumber, ReadingPageResultHandler readingPageResultHandler) {
        fileManager.requestReadPage(pageNumber, readingPageResultHandler);
      }

      @Override
      public List<TextRange> getAllSearchResultsInDocument(Document document) {
        if (searchManager != null) {
          return searchManager.getAllSearchResultsInDocument(document);
        }
        return null;
      }
    };
  }
}