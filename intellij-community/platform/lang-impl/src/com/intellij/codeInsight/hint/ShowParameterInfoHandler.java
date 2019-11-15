// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.lang.Language;
import com.intellij.lang.parameterInfo.LanguageParameterInfo;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LightweightHint;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class ShowParameterInfoHandler implements CodeInsightActionHandler {
  private static final ParameterInfoHandler[] EMPTY_HANDLERS = new ParameterInfoHandler[0];
  private final boolean myRequestFocus;

  public ShowParameterInfoHandler() {
    this(false);
  }

  public ShowParameterInfoHandler(boolean requestFocus) {
    myRequestFocus = requestFocus;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    invoke(project, editor, file, -1, null, myRequestFocus);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  /**
   * @deprecated use {@link #invoke(Project, Editor, PsiFile, int, PsiElement, boolean)} instead
   */
  @Deprecated
  public static void invoke(final Project project, final Editor editor, PsiFile file, int lbraceOffset, PsiElement highlightedElement) {
    invoke(project, editor, file, lbraceOffset, highlightedElement, false);
  }

  public static void invoke(final Project project, final Editor editor, PsiFile file,
                            int lbraceOffset, PsiElement highlightedElement, boolean requestFocus) {
    invoke(project, editor, file, lbraceOffset, highlightedElement, requestFocus, false,
           CodeInsightBundle.message("parameter.info.progress.title"),
           e -> DumbService.getInstance(project)
             .showDumbModeNotification(CodeInsightBundle.message("parameter.info.indexing.mode.not.supported")));
  }

  public static void invoke(final Project project, final Editor editor, PsiFile file,
                            int lbraceOffset, PsiElement highlightedElement,
                            boolean requestFocus, boolean singleParameterHint,
                            String progressTitle,
                            Consumer<IndexNotReadyException> indexNotReadyExceptionConsumer) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final int offset = editor.getCaretModel().getOffset();
    final int fileLength = file.getTextLength();
    if (fileLength == 0) return;

    final ShowParameterInfoContext context = new ShowParameterInfoContext(
      editor,
      project,
      file,
      offset,
      lbraceOffset,
      requestFocus,
      singleParameterHint
    );

    context.setHighlightedElement(highlightedElement);
    context.setRequestFocus(requestFocus);

    // file.findElementAt(file.getTextLength()) returns null but we may need to show parameter info at EOF offset (for example in SQL)
    final int offsetForLangDetection = offset > 0 && offset == fileLength ? offset - 1 : offset;
    final Language language = PsiUtilCore.getLanguageAtOffset(file, offsetForLangDetection);

    final ParameterInfoHandler<PsiElement, Object>[] handlers =
      ObjectUtils.notNull(getHandlers(project, language, file.getViewProvider().getBaseLanguage()), EMPTY_HANDLERS);

    Lookup lookup = LookupManager.getInstance(project).getActiveLookup();

    if (lookup != null) {
      LookupElement item = lookup.getCurrentItem();

      if (item != null) {
        for (ParameterInfoHandler<PsiElement, Object> handler : handlers) {
          if (handler.couldShowInLookup()) {
            final Object[] items = handler.getParametersForLookup(item, context);
            if (items != null && items.length > 0) {
              showLookupEditorHint(items, editor, handler, requestFocus);
            }
            return;
          }
        }
      }
      return;
    }

    final Component focusOwner = IdeFocusManager.getInstance(project).getFocusOwner();

    ProgressManager.getInstance().run(
      new Task.Backgroundable(project, progressTitle, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          PsiElement element = null;
          ParameterInfoHandler<PsiElement, Object> handler = null;

          DumbService dumbService = DumbService.getInstance(project);
          dumbService.setAlternativeResolveEnabled(true);

          try {
            for (int i = 0; i < handlers.length; i++) {
              final ParameterInfoHandler<PsiElement, Object> h = handlers[i];
              handler = h;
              element = ReadAction
                .nonBlocking(() -> {
                  try {
                    return h.findElementForParameterInfo(context);
                  }
                  catch (IndexNotReadyException e) {
                    indexNotReadyExceptionConsumer.consume(e);
                    return null;
                  }
                })
                .cancelWith(indicator)
                .expireWhen(() -> editor.getCaretModel().getOffset() != offset)
                .executeSynchronously();

              if (element != null) {
                break;
              }
            }
          }
          finally {
            dumbService.setAlternativeResolveEnabled(false);
          }

          if (element != null && !indicator.isCanceled()) {
            final PsiElement el = element;
            final ParameterInfoHandler<PsiElement, Object> h = handler;
            ApplicationManager.getApplication().invokeLater(() -> {
              if (!el.isValid()) return;

              if (editor.getCaretModel().getOffset() != context.getOffset() ||
                  !Objects.equals(focusOwner, IdeFocusManager.getInstance(project).getFocusOwner())) return;

              h.showParameterInfo(el, context);
            });
          }
        }
      }
    );
  }

  private static void showLookupEditorHint(Object[] descriptors,
                                           final Editor editor,
                                           ParameterInfoHandler handler,
                                           boolean requestFocus) {
    ParameterInfoComponent component = new ParameterInfoComponent(descriptors, editor, handler, requestFocus, false);
    component.update(false);

    final LightweightHint hint = new LightweightHint(component);
    hint.setSelectingHint(true);
    final HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
    final Pair<Point, Short> pos = ParameterInfoController.chooseBestHintPosition(editor, null, hint, HintManager.DEFAULT, true);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!editor.getComponent().isShowing()) return;
      hintManager.showEditorHint(hint, editor, pos.getFirst(),
                                 HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE | HintManager.UPDATE_BY_SCROLLING,
                                 0, false, pos.getSecond());
    });
  }

  @Nullable
  public static ParameterInfoHandler[] getHandlers(Project project, final Language... languages) {
    Set<ParameterInfoHandler> handlers = new LinkedHashSet<>();
    DumbService dumbService = DumbService.getInstance(project);
    for (final Language language : languages) {
      handlers.addAll(dumbService.filterByDumbAwareness(LanguageParameterInfo.INSTANCE.allForLanguage(language)));
    }
    if (handlers.isEmpty()) return null;
    return handlers.toArray(new ParameterInfoHandler[0]);
  }
}

