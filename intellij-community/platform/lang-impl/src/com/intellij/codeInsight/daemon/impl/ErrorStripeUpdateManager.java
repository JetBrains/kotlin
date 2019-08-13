// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.ex.ErrorStripTooltipRendererProvider;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ErrorStripeUpdateManager {
  public static ErrorStripeUpdateManager getInstance(Project project) {
    return ServiceManager.getService(project, ErrorStripeUpdateManager.class);
  }

  private final Project myProject;
  private final PsiDocumentManager myPsiDocumentManager;

  public ErrorStripeUpdateManager(Project project, PsiDocumentManager psiDocumentManager) {
    myProject = project;
    myPsiDocumentManager = psiDocumentManager;
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  public void repaintErrorStripePanel(@NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!myProject.isInitialized()) return;

    PsiFile file = myPsiDocumentManager.getPsiFile(editor.getDocument());
    final EditorMarkupModel markup = (EditorMarkupModel) editor.getMarkupModel();
    markup.setErrorPanelPopupHandler(createPopup(file));
    markup.setErrorStripTooltipRendererProvider(createTooltipRenderer());
    markup.setMinMarkHeight(DaemonCodeAnalyzerSettings.getInstance().getErrorStripeMarkMinHeight());
    setOrRefreshErrorStripeRenderer(markup, file);
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  protected void setOrRefreshErrorStripeRenderer(@NotNull EditorMarkupModel editorMarkupModel, @Nullable PsiFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!editorMarkupModel.isErrorStripeVisible() || !DaemonCodeAnalyzer.getInstance(myProject).isHighlightingAvailable(file)) {
      return;
    }
    ErrorStripeRenderer renderer = editorMarkupModel.getErrorStripeRenderer();
    if (renderer instanceof TrafficLightRenderer) {
      TrafficLightRenderer tlr = (TrafficLightRenderer) renderer;
      EditorMarkupModelImpl markupModelImpl = (EditorMarkupModelImpl) editorMarkupModel;
      tlr.refresh(markupModelImpl);
      markupModelImpl.repaintVerticalScrollBar();
      if (tlr.isValid()) return;
    }
    Editor editor = editorMarkupModel.getEditor();
    if (editor.isDisposed()) return;

    editorMarkupModel.setErrorStripeRenderer(createRenderer(editor, file));
  }

  @NotNull
  private static PopupHandler createPopup(@Nullable PsiFile psiFile) {
    return new DaemonEditorPopup(psiFile);
  }

  @NotNull
  private ErrorStripTooltipRendererProvider createTooltipRenderer() {
    return new DaemonTooltipRendererProvider(myProject);
  }

  @Nullable
  protected TrafficLightRenderer createRenderer(@NotNull Editor editor, @Nullable PsiFile file) {
    for (TrafficLightRendererContributor contributor : TrafficLightRendererContributor.EP_NAME.getExtensionList()) {
      TrafficLightRenderer renderer = contributor.createRenderer(editor, file);
      if (renderer != null) {
        return renderer;
      }
    }
    return new TrafficLightRenderer(myProject, editor.getDocument(), file);
  }
}
