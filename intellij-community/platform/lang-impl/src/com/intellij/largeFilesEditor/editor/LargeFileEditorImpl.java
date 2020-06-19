// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.largeFilesEditor.PlatformActionsReplacer;
import com.intellij.largeFilesEditor.encoding.EncodingWidget;
import com.intellij.largeFilesEditor.encoding.LargeFileEditorAccess;
import com.intellij.largeFilesEditor.encoding.LargeFileEditorAccessorImpl;
import com.intellij.largeFilesEditor.file.LargeFileManager;
import com.intellij.largeFilesEditor.file.LargeFileManagerImpl;
import com.intellij.largeFilesEditor.file.ReadingPageResultHandler;
import com.intellij.largeFilesEditor.search.LfeSearchManager;
import com.intellij.largeFilesEditor.search.LfeSearchManagerImpl;
import com.intellij.largeFilesEditor.search.RangeSearchCreatorImpl;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
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

public class LargeFileEditorImpl extends UserDataHolderBase implements LargeFileEditor {

  private static final Logger logger = Logger.getInstance(LargeFileEditorImpl.class);
  private final Project project;
  private LargeFileManager fileManager;
  private final EditorModel editorModel;
  private final DocumentEx document;
  private final VirtualFile vFile;
  private LfeSearchManager searchManager;

  public LargeFileEditorImpl(Project project, VirtualFile vFile) {
    this.vFile = vFile;
    this.project = project;

    int customPageSize = PropertiesGetter.getPageSize();
    int customBorderShift = PropertiesGetter.getMaxPageBorderShiftBytes();

    document = createSpecialDocument(vFile);

    editorModel = new EditorModel(document, project, implementDataProviderForEditorModel());
    editorModel.putUserDataToEditor(LARGE_FILE_EDITOR_MARK_KEY, new Object());
    editorModel.putUserDataToEditor(LARGE_FILE_EDITOR_KEY, this);

    try {
      fileManager = new LargeFileManagerImpl(vFile, customPageSize, customBorderShift);
    }
    catch (FileNotFoundException e) {
      logger.warn(e);
      editorModel.setBrokenMode();
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.cant.open.file.because.file.not.found"),
                                 EditorBundle.message("large.file.editor.title.warning"));
      requestClosingEditorTab();
      return;
    }

    searchManager = new LfeSearchManagerImpl(
      this, fileManager.getFileDataProviderForSearch(), new RangeSearchCreatorImpl());

    createAndAddSpecialWidgetIfNeed(project);
    PlatformActionsReplacer.makeAdaptingOfPlatformActionsIfNeed();

    editorModel.addCaretListener(new MyCaretListener());

    fileManager.addFileChangeListener((Page lastPage, boolean isLengthIncreased) -> {
      ApplicationManager.getApplication().invokeLater(() -> {
        editorModel.onFileChanged(lastPage, isLengthIncreased);
      });
    });
  }

  private void requestClosingEditorTab() {
    ApplicationManager.getApplication().invokeLater(
      () -> FileEditorManager.getInstance(project).closeFile(vFile));
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
      statusBar.addWidget(new EncodingWidget(project, new LargeFileEditorAccessorImpl()));
    }
  }

  @Override
  public LfeSearchManager getSearchManager() {
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
    return editorModel.getEditor().getContentComponent();
  }

  @NotNull
  @Override
  public String getName() {
    return "Large File Editor";
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
    if (state instanceof LargeFileEditorState) {
      LargeFileEditorState largeFileEditorState = (LargeFileEditorState)state;
      editorModel.setCaretAndShow(largeFileEditorState.caretPageNumber,
                                  largeFileEditorState.caretSymbolOffsetInPage);
    }
  }

  @NotNull
  @Override
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    LargeFileEditorState state = new LargeFileEditorState();
    state.caretPageNumber = editorModel.getCaretPageNumber();
    state.caretSymbolOffsetInPage = editorModel.getCaretPageOffset();
    return state;
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
      Disposer.dispose(fileManager);
    }
    editorModel.dispose();

    vFile.putUserData(FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY, null);
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

  @Override
  public @NotNull VirtualFile getFile() {
    return vFile;
  }

  @Override
  public LargeFileEditorAccess createAccessForEncodingWidget() {
    return new LargeFileEditorAccess() {
      @NotNull
      @Override
      public VirtualFile getVirtualFile() {
        return getFile();
      }

      @NotNull
      @Override
      public Editor getEditor() {
        return LargeFileEditorImpl.this.getEditor();
      }

      @Override
      public boolean tryChangeEncoding(@NotNull Charset charset) {

        if (fileManager.hasBOM()) {
          Messages.showWarningDialog(
            EditorBundle.message("large.file.editor.message.cant.change.encoding.because.it.has.bom.byte.order.mark"),
            EditorBundle.message("large.file.editor.title.warning"));
          return false;
        }

        if (searchManager.isSearchWorkingNow()) {
          Messages.showInfoMessage(EditorBundle.message("large.file.editor.message.cant.change.encoding.because.search.is.working.now"),
                                   EditorBundle.message("large.file.editor.title.cant.change.encoding"));
          return false;
        }

        fileManager.reset(charset);
        editorModel.onEncodingChanged();
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

  @Override
  public int getPageSize() {
    return fileManager.getPageSize();
  }

  private static DocumentEx createSpecialDocument(VirtualFile vFile) {
    DocumentEx doc = new DocumentImpl("", false, false); // restrict "\r\n" line separators
    doc.putUserData(FileDocumentManagerImpl.NOT_RELOADABLE_DOCUMENT_KEY, new Object());  // to protect document from illegal content changes (see usages of the key)
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
      public List<SearchResult> getSearchResultsInPage(Page page) {
        if (searchManager != null) {
          return searchManager.getSearchResultsInPage(page);
        }
        return null;
      }
    };
  }
}