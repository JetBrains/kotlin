// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FoldingModelEx;
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.LightweightHint;
import com.intellij.util.containers.WeakList;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

public class CodeFoldingManagerImpl extends CodeFoldingManager implements ProjectComponent, Disposable {
  private final Project myProject;

  private final Collection<Document> myDocumentsWithFoldingInfo = new WeakList<>();

  private final Key<DocumentFoldingInfo> myFoldingInfoInDocumentKey = Key.create("FOLDING_INFO_IN_DOCUMENT_KEY");
  private static final Key<Boolean> FOLDING_STATE_KEY = Key.create("FOLDING_STATE_KEY");

  CodeFoldingManagerImpl(Project project) {
    myProject = project;
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
  public void projectOpened() {
    final EditorMouseMotionListener myMouseMotionListener = new EditorMouseMotionListener() {
      LightweightHint myCurrentHint;
      FoldRegion myCurrentFold;

      @Override
      public void mouseMoved(@NotNull EditorMouseEvent e) {
        if (myProject.isDisposed()) return;
        LightweightHint hint = null;
        try {
          HintManager hintManager = HintManager.getInstance();
          if (hintManager != null && hintManager.hasShownHintsThatWillHideByOtherHint(false)) {
            return;
          }

          if (e.getArea() != EditorMouseEventArea.FOLDING_OUTLINE_AREA) return;

          Editor editor = e.getEditor();
          if (PsiDocumentManager.getInstance(myProject).isUncommited(editor.getDocument())) return;

          MouseEvent mouseEvent = e.getMouseEvent();
          FoldRegion fold = ((EditorEx)editor).getGutterComponentEx().findFoldingAnchorAt(mouseEvent.getX(), mouseEvent.getY());

          if (fold == null || !fold.isValid()) return;
          if (fold == myCurrentFold && myCurrentHint != null) {
            hint = myCurrentHint;
            return;
          }

          TextRange psiElementRange = EditorFoldingInfo.get(editor).getPsiElementRange(fold);
          if (psiElementRange == null) return;

          int textOffset = psiElementRange.getStartOffset();
          // There is a possible case that target PSI element's offset is less than fold region offset (e.g. complete method is
          // returned as PSI element for fold region that corresponds to java method code block). We don't want to show any hint
          // if start of the current fold region is displayed.
          Point foldStartXY = editor.visualPositionToXY(editor.offsetToVisualPosition(Math.max(textOffset, fold.getStartOffset())));
          Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
          if (visibleArea.y > foldStartXY.y) {
            if (myCurrentHint != null) {
              myCurrentHint.hide();
              myCurrentHint = null;
            }


            // We want to show a hint with the top fold region content that is above the current viewport position.
            // However, there is a possible case that complete region has a big height and only a little bottom part
            // is shown at the moment. We can't just show hint with the whole top content because it would hide actual
            // editor content, hence, we show max(2; available visual lines number) instead.
            // P.S. '2' is used here in assumption that many java methods have javadocs which first line is just '/**'.
            // So, it's not too useful to show only it even when available vertical space is not big enough.
            int availableVisualLines = EditorFragmentComponent.getAvailableVisualLinesAboveEditor(editor);
            int startVisualLine = editor.offsetToVisualPosition(textOffset).line;
            int desiredEndVisualLine = Math.max(0, editor.xyToVisualPosition(new Point(0, visibleArea.y)).line - 1);
            int endVisualLine = startVisualLine + availableVisualLines;
            if (endVisualLine > desiredEndVisualLine) {
              endVisualLine = desiredEndVisualLine;
            }

            // Show only the non-displayed top part of the target fold region
            int endOffset = editor.logicalPositionToOffset(editor.visualToLogicalPosition(new VisualPosition(endVisualLine, 0)));
            TextRange textRange = new UnfairTextRange(textOffset, endOffset);
            hint = EditorFragmentComponent.showEditorFragmentHint(editor, textRange, true, true);
            myCurrentFold = fold;
            myCurrentHint = hint;
          }
        }
        finally {
          if (hint == null) {
            if (myCurrentHint != null) {
              myCurrentHint.hide();
              myCurrentHint = null;
            }
            myCurrentFold = null;
          }
        }
      }
    };

    StartupManager.getInstance(myProject).registerPostStartupActivity(
      (DumbAwareRunnable)() -> EditorFactory.getInstance().getEventMulticaster().addEditorMouseMotionListener(myMouseMotionListener, myProject));
  }

  @Override
  public void releaseFoldings(@NotNull Editor editor) {
    ApplicationManagerEx.getApplicationEx().assertIsDispatchThread();
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
      ApplicationManagerEx.getApplicationEx().assertIsDispatchThread();
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
