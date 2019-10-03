// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefaultHighlightInfoProcessor extends HighlightInfoProcessor {
  @Override
  public void highlightsInsideVisiblePartAreProduced(@NotNull final HighlightingSession session,
                                                     @Nullable Editor editor,
                                                     @NotNull final List<? extends HighlightInfo> infos,
                                                     @NotNull TextRange priorityRange,
                                                     @NotNull TextRange restrictRange,
                                                     final int groupId) {
    final PsiFile psiFile = session.getPsiFile();
    final Project project = psiFile.getProject();
    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return;
    final long modificationStamp = document.getModificationStamp();
    final TextRange priorityIntersection = priorityRange.intersection(restrictRange);
    List<? extends HighlightInfo> infoCopy = new ArrayList<>(infos);
    ((HighlightingSessionImpl)session).applyInEDT(() -> {
      if (modificationStamp != document.getModificationStamp()) return;
      if (priorityIntersection != null) {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, true);

        EditorColorsScheme scheme = session.getColorsScheme();
        UpdateHighlightersUtil.setHighlightersInRange(project, document, priorityIntersection, scheme, infoCopy, (MarkupModelEx)markupModel, groupId);
      }
      if (editor != null && !editor.isDisposed()) {
        // usability: show auto import popup as soon as possible
        if (!DumbService.isDumb(project)) {
          ShowAutoImportPassFactory siFactory = TextEditorHighlightingPassRegistrarImpl.EP_NAME.findExtensionOrFail(ShowAutoImportPassFactory.class);
          TextEditorHighlightingPass highlightingPass = siFactory.createHighlightingPass(psiFile, editor);
          if (highlightingPass != null) {
            highlightingPass.doApplyInformationToEditor();
          }
        }

        repaintErrorStripeAndIcon(editor, project);
      }
    });
  }

  static void repaintErrorStripeAndIcon(@NotNull Editor editor, @NotNull Project project) {
    EditorMarkupModelImpl markup = (EditorMarkupModelImpl)editor.getMarkupModel();
    markup.repaintTrafficLightIcon();
    ErrorStripeUpdateManager.getInstance(project).repaintErrorStripePanel(editor);
  }

  @Override
  public void highlightsOutsideVisiblePartAreProduced(@NotNull final HighlightingSession session,
                                                      @Nullable Editor editor,
                                                      @NotNull final List<? extends HighlightInfo> infos,
                                                      @NotNull final TextRange priorityRange,
                                                      @NotNull final TextRange restrictedRange, final int groupId) {
    final PsiFile psiFile = session.getPsiFile();
    final Project project = psiFile.getProject();
    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return;
    final long modificationStamp = document.getModificationStamp();
    ((HighlightingSessionImpl)session).applyInEDT(() -> {
      if (project.isDisposed() || modificationStamp != document.getModificationStamp()) return;

      EditorColorsScheme scheme = session.getColorsScheme();

      UpdateHighlightersUtil.setHighlightersOutsideRange(project, document, psiFile, infos, scheme,
                                                         restrictedRange.getStartOffset(), restrictedRange.getEndOffset(),
                                                         ProperTextRange.create(priorityRange),
                                                         groupId);
      if (editor != null) {
        repaintErrorStripeAndIcon(editor, project);
      }
    });
  }

  @Override
  public void allHighlightsForRangeAreProduced(@NotNull HighlightingSession session,
                                               @NotNull TextRange elementRange,
                                               @Nullable List<? extends HighlightInfo> infos) {
    PsiFile psiFile = session.getPsiFile();
    killAbandonedHighlightsUnder(psiFile, elementRange, infos, session);
  }

  private static void killAbandonedHighlightsUnder(@NotNull PsiFile psiFile,
                                                   @NotNull final TextRange range,
                                                   @Nullable final List<? extends HighlightInfo> infos,
                                                   @NotNull final HighlightingSession highlightingSession) {
    final Project project = psiFile.getProject();
    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return;
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, range.getStartOffset(), range.getEndOffset(), existing -> {
        if (existing.isBijective() &&
            existing.getGroup() == Pass.UPDATE_ALL &&
            range.equalsToRange(existing.getActualStartOffset(), existing.getActualEndOffset())) {
          if (infos != null) {
            for (HighlightInfo created : infos) {
              if (existing.equalsByActualOffset(created)) return true;
            }
          }
          // seems that highlight info "existing" is going to disappear
          // remove it earlier
          ((HighlightingSessionImpl)highlightingSession).queueDisposeHighlighterFor(existing);
        }
        return true;
      });
  }

  @Override
  public void infoIsAvailable(@NotNull HighlightingSession session,
                              @NotNull HighlightInfo info,
                              @NotNull TextRange priorityRange,
                              @NotNull TextRange restrictedRange,
                              int groupId) {
    ((HighlightingSessionImpl)session).queueHighlightInfo(info, restrictedRange, groupId);
  }

  @Override
  public void progressIsAdvanced(@NotNull HighlightingSession highlightingSession,
                                 @Nullable Editor editor,
                                 double progress) {
    PsiFile file = highlightingSession.getPsiFile();
    repaintTrafficIcon(file, editor, progress);
  }

  private final Alarm repaintIconAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private void repaintTrafficIcon(@NotNull final PsiFile file, @Nullable Editor editor, double progress) {
    if (ApplicationManager.getApplication().isCommandLine()) return;

    if (repaintIconAlarm.isEmpty() || progress >= 1) {
      repaintIconAlarm.addRequest(() -> {
        Project myProject = file.getProject();
        if (myProject.isDisposed()) return;
        Editor myeditor = editor;
        if (myeditor == null) {
          myeditor = PsiUtilBase.findEditor(file);
        }
        if (myeditor != null && !myeditor.isDisposed()) {
          repaintErrorStripeAndIcon(myeditor, myProject);
        }
      }, 50, null);
    }
  }
}
