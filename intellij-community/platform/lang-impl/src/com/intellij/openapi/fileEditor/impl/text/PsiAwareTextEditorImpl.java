// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.TextEditorBackgroundHighlighter;
import com.intellij.codeInsight.daemon.impl.focusMode.FocusModePassFactory;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PsiAwareTextEditorImpl extends TextEditorImpl {
  private TextEditorBackgroundHighlighter myBackgroundHighlighter;

  public PsiAwareTextEditorImpl(@NotNull final Project project, @NotNull final VirtualFile file, final TextEditorProvider provider) {
    super(project, file, provider);
  }

  @NotNull
  @Override
  protected Runnable loadEditorInBackground() {
    Runnable baseResult = super.loadEditorInBackground();
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);
    Document document = FileDocumentManager.getInstance().getDocument(myFile);
    boolean shouldBuildInitialFoldings =
      document != null && !myProject.isDefault() && PsiDocumentManager.getInstance(myProject).isCommitted(document);
    CodeFoldingState foldingState = shouldBuildInitialFoldings
                                    ? CodeFoldingManager.getInstance(myProject).buildInitialFoldings(document)
                                    : null;

    List<? extends Segment> focusZones = FocusModePassFactory.calcFocusZones(psiFile);

    return () -> {
      baseResult.run();
      Editor editor = getEditor();

      if (foldingState != null) {
        foldingState.setToEditor(editor);
      }

      if (focusZones != null) {
        FocusModePassFactory.setToEditor(focusZones, editor);
        if (editor instanceof EditorImpl) {
          ((EditorImpl)editor).applyFocusMode();
        }
      }

      if (psiFile != null && psiFile.isValid()) {
        DaemonCodeAnalyzer.getInstance(myProject).restart(psiFile);
      }
    };
  }

  @NotNull
  @Override
  protected TextEditorComponent createEditorComponent(final Project project, final VirtualFile file) {
    return new PsiAwareTextEditorComponent(project, file, this);
  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    if (!AsyncEditorLoader.isEditorLoaded(getEditor())) {
      return null;
    }

    if (myBackgroundHighlighter == null) {
      myBackgroundHighlighter = new TextEditorBackgroundHighlighter(myProject, getEditor());
    }
    return myBackgroundHighlighter;
  }

  private static class PsiAwareTextEditorComponent extends TextEditorComponent {
    private final Project myProject;
    private final VirtualFile myFile;

    private PsiAwareTextEditorComponent(@NotNull final Project project,
                                        @NotNull final VirtualFile file,
                                        @NotNull final TextEditorImpl textEditor) {
      super(project, file, textEditor);
      myProject = project;
      myFile = file;
    }

    @Override
    public void dispose() {
      super.dispose();
      CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(myProject);
      if (foldingManager != null) {
        foldingManager.releaseFoldings(getEditor());
      }
    }

    @Nullable
    @Override
    public DataProvider createBackgroundDataProvider() {
      DataProvider superProvider = super.createBackgroundDataProvider();
      if (superProvider == null) return null;

      return dataId -> {
        if (PlatformDataKeys.DOMINANT_HINT_AREA_RECTANGLE.is(dataId)) {
          LookupImpl lookup = (LookupImpl)LookupManager.getInstance(myProject).getActiveLookup();
          if (lookup != null && lookup.isVisible()) {
            return lookup.getBounds();
          }
        }
        if (LangDataKeys.MODULE.is(dataId)) {
          return ModuleUtilCore.findModuleForFile(myFile, myProject);
        }
        return superProvider.getData(dataId);
      };
    }
  }
}
