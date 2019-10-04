// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.containers.WeakList;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CodeFoldingManagerImpl extends CodeFoldingManager implements Disposable {
  private final Project myProject;

  private final Collection<Document> myDocumentsWithFoldingInfo = new WeakList<>();

  private final Key<DocumentFoldingInfo> myFoldingInfoInDocumentKey = Key.create("FOLDING_INFO_IN_DOCUMENT_KEY");
  private static final Key<Boolean> FOLDING_STATE_KEY = Key.create("FOLDING_STATE_KEY");

  public CodeFoldingManagerImpl(Project project) {
    myProject = project;

    LanguageFolding.EP_NAME.addExtensionPointListener(
      new ExtensionPointListener<KeyedLazyInstance<FoldingBuilder>>() {
        @Override
        public void extensionAdded(@NotNull KeyedLazyInstance<FoldingBuilder> extension, @NotNull PluginDescriptor pluginDescriptor) {
          // Asynchronously remove foldings when extension is added
          for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (fileEditor instanceof TextEditor) {
              scheduleAsyncFoldingUpdate(((TextEditor) fileEditor).getEditor());
            }
          }
        }

        @Override
        public void extensionRemoved(@NotNull KeyedLazyInstance<FoldingBuilder> extension, @NotNull PluginDescriptor pluginDescriptor) {
          // Synchronously remove foldings when extension is removed
          for (FileEditor fileEditor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (fileEditor instanceof TextEditor) {
              updateFoldRegions(((TextEditor)fileEditor).getEditor());
            }
          }
        }
      }, myProject);
  }

  @Override
  public void dispose() {
    for (Document document : myDocumentsWithFoldingInfo) {
      if (document != null) {
        document.putUserData(myFoldingInfoInDocumentKey, null);
      }
    }
  }

  @Override
  public void releaseFoldings(@NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final Project project = editor.getProject();
    if (project != null && (!project.equals(myProject) || !project.isOpen())) return;

    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (file == null || !file.getViewProvider().isPhysical() || !file.isValid()) return;

    EditorFoldingInfo.get(editor).dispose();
  }

  @Override
  public void buildInitialFoldings(@NotNull final Editor editor) {
    final Project project = editor.getProject();
    if (project == null || !project.equals(myProject) || editor.isDisposed()) return;
    if (!((FoldingModelEx)editor.getFoldingModel()).isFoldingEnabled()) return;
    if (!FoldingUpdate.supportsDumbModeFolding(editor)) return;

    Document document = editor.getDocument();
    PsiDocumentManager.getInstance(myProject).commitDocument(document);
    CodeFoldingState foldingState = buildInitialFoldings(document);
    if (foldingState != null) {
      foldingState.setToEditor(editor);
    }
  }

  @Nullable
  @Override
  public CodeFoldingState buildInitialFoldings(@NotNull final Document document) {
    if (myProject.isDisposed()) {
      return null;
    }
    ApplicationManager.getApplication().assertReadAccessAllowed();
    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);
    if (psiDocumentManager.isUncommited(document)) {
      // skip building foldings for uncommitted document, CodeFoldingPass invoked by daemon will do it later
      return null;
    }
    //Do not save/restore folding for code fragments
    final PsiFile file = psiDocumentManager.getPsiFile(document);
    if (file == null || !file.isValid() || !file.getViewProvider().isPhysical() && !ApplicationManager.getApplication().isUnitTestMode()) {
      return null;
    }


    final List<FoldingUpdate.RegionInfo> regionInfos = FoldingUpdate.getFoldingsFor(file, document, true);

    return editor -> {
      ApplicationManager.getApplication().assertIsDispatchThread();
      if (myProject.isDisposed() || editor.isDisposed()) return;
      final FoldingModelEx foldingModel = (FoldingModelEx)editor.getFoldingModel();
      if (!foldingModel.isFoldingEnabled()) return;
      if (isFoldingsInitializedInEditor(editor)) return;
      if (DumbService.isDumb(myProject) && !FoldingUpdate.supportsDumbModeFolding(editor)) return;

      foldingModel.runBatchFoldingOperationDoNotCollapseCaret(new UpdateFoldRegionsOperation(myProject, editor, file, regionInfos,
                                                                                             UpdateFoldRegionsOperation.ApplyDefaultStateMode.YES,
                                                                                             false, false));
      initFolding(editor);
    };
  }

  @Nullable
  @Override
  public Boolean isCollapsedByDefault(@NotNull FoldRegion region) {
    return region.getUserData(UpdateFoldRegionsOperation.COLLAPSED_BY_DEFAULT);
  }

  @Override
  public void scheduleAsyncFoldingUpdate(@NotNull Editor editor) {
    FoldingUpdate.clearFoldingCache(editor);
    DaemonCodeAnalyzer.getInstance(myProject).restart();
  }

  private void initFolding(@NotNull final Editor editor) {
    final Document document = editor.getDocument();
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      DocumentFoldingInfo documentFoldingInfo = getDocumentFoldingInfo(document);
      Editor[] editors = EditorFactory.getInstance().getEditors(document, myProject);
      for (Editor otherEditor : editors) {
        if (otherEditor == editor || !isFoldingsInitializedInEditor(otherEditor)) continue;
        documentFoldingInfo.loadFromEditor(otherEditor);
        break;
      }
      documentFoldingInfo.setToEditor(editor);
      documentFoldingInfo.clear();

      editor.putUserData(FOLDING_STATE_KEY, Boolean.TRUE);
    });
  }

  @Override
  @Nullable
  public FoldRegion findFoldRegion(@NotNull Editor editor, int startOffset, int endOffset) {
    return FoldingUtil.findFoldRegion(editor, startOffset, endOffset);
  }

  @Override
  public FoldRegion[] getFoldRegionsAtOffset(@NotNull Editor editor, int offset) {
    return FoldingUtil.getFoldRegionsAtOffset(editor, offset);
  }

  @Override
  public void updateFoldRegions(@NotNull Editor editor) {
    updateFoldRegions(editor, false);
  }

  public void updateFoldRegions(Editor editor, boolean quick) {
    if (!editor.getSettings().isAutoCodeFoldingEnabled()) return;
    PsiDocumentManager.getInstance(myProject).commitDocument(editor.getDocument());
    Runnable runnable = updateFoldRegions(editor, false, quick);
    if (runnable != null) {
      runnable.run();
    }
  }

  @Override
  @Nullable
  public Runnable updateFoldRegionsAsync(@NotNull final Editor editor, final boolean firstTime) {
    if (!editor.getSettings().isAutoCodeFoldingEnabled()) return null;
    final Runnable runnable = updateFoldRegions(editor, firstTime, false);
    return () -> {
      if (runnable != null) {
        runnable.run();
      }
      if (firstTime && !isFoldingsInitializedInEditor(editor)) {
        initFolding(editor);
      }
    };
  }

  @Nullable
  private Runnable updateFoldRegions(@NotNull Editor editor, boolean applyDefaultState, boolean quick) {
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
    return file == null ? null : FoldingUpdate.updateFoldRegions(editor, file, applyDefaultState, quick);
  }

  @Override
  public CodeFoldingState saveFoldingState(@NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    DocumentFoldingInfo info = getDocumentFoldingInfo(editor.getDocument());
    if (isFoldingsInitializedInEditor(editor)) {
      info.loadFromEditor(editor);
    }
    return info;
  }

  @Override
  public void restoreFoldingState(@NotNull Editor editor, @NotNull CodeFoldingState state) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isFoldingsInitializedInEditor(editor)) {
      state.setToEditor(editor);
    }
  }

  @Override
  public void writeFoldingState(@NotNull CodeFoldingState state, @NotNull Element element) {
    if (state instanceof DocumentFoldingInfo) {
      ((DocumentFoldingInfo)state).writeExternal(element);
    }
  }

  @Override
  public CodeFoldingState readFoldingState(@NotNull Element element, @NotNull Document document) {
    DocumentFoldingInfo info = getDocumentFoldingInfo(document);
    info.readExternal(element);
    return info;
  }

  @NotNull
  private DocumentFoldingInfo getDocumentFoldingInfo(@NotNull Document document) {
    DocumentFoldingInfo info = document.getUserData(myFoldingInfoInDocumentKey);
    if (info == null) {
      info = new DocumentFoldingInfo(myProject, document);
      DocumentFoldingInfo written = ((UserDataHolderEx)document).putUserDataIfAbsent(myFoldingInfoInDocumentKey, info);
      if (written == info) {
        myDocumentsWithFoldingInfo.add(document);
      }
      else {
        info = written;
      }
    }
    return info;
  }

  private static boolean isFoldingsInitializedInEditor(@NotNull Editor editor) {
    return Boolean.TRUE.equals(editor.getUserData(FOLDING_STATE_KEY));
  }
}
