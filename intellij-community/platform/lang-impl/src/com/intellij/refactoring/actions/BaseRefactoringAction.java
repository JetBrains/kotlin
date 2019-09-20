// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.actions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.ide.IdeEventQueue;
import com.intellij.lang.ContextAwareActionHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseRefactoringAction extends AnAction implements UpdateInBackground {
  private final Condition<Language> myLanguageCondition = this::isAvailableForLanguage;

  protected abstract boolean isAvailableInEditorOnly();

  protected abstract boolean isEnabledOnElements(@NotNull PsiElement[] elements);

  @Override
  public boolean startInTransaction() {
    return true;
  }

  protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element,
                                                        @NotNull Editor editor,
                                                        @NotNull PsiFile file,
                                                        @NotNull DataContext context,
                                                        @NotNull String place) {
    if (ActionPlaces.isPopupPlace(place)) {
      final RefactoringActionHandler handler = getHandler(context);
      if (handler instanceof ContextAwareActionHandler) {
        ContextAwareActionHandler contextAwareActionHandler = (ContextAwareActionHandler)handler;
        if (!contextAwareActionHandler.isAvailableForQuickList(editor, file, context)) {
          return false;
        }
      }
    }

    return isAvailableOnElementInEditorAndFile(element, editor, file, context);
  }

  protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element,
                                                        @NotNull Editor editor,
                                                        @NotNull PsiFile file,
                                                        @NotNull DataContext context) {
    return true;
  }

  protected boolean hasAvailableHandler(@NotNull DataContext dataContext) {
    final RefactoringActionHandler handler = getHandler(dataContext);
    if (handler != null) {
      if (handler instanceof ContextAwareActionHandler) {
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (editor != null && file != null && !((ContextAwareActionHandler)handler).isAvailableForQuickList(editor, file, dataContext)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Nullable
  protected abstract RefactoringActionHandler getHandler(@NotNull DataContext dataContext);

  @Override
  public final void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = e.getProject();
    if (project == null) return;
    int eventCount = IdeEventQueue.getInstance().getEventCount();
    if (!PsiDocumentManager.getInstance(project).commitAllDocumentsUnderProgress()) {
      return;
    }
    IdeEventQueue.getInstance().setEventCount(eventCount);
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    final PsiElement[] elements = getPsiElementArray(dataContext);

    RefactoringActionHandler handler;
    try {
      handler = getHandler(dataContext);
    }
    catch (ProcessCanceledException ignored) {
      return;
    }
    if (handler == null) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.symbol.to.refactor"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.getCannotRefactorMessage(null), null);
      return;
    }

    InplaceRefactoring activeInplaceRenamer = InplaceRefactoring.getActiveInplaceRenamer(editor);
    if (!InplaceRefactoring.canStartAnotherRefactoring(editor, project, handler, elements) && activeInplaceRenamer != null) {
      InplaceRefactoring.unableToStartWarning(project, editor);
      return;
    }

    if (activeInplaceRenamer == null) {
      final LookupEx lookup = LookupManager.getActiveLookup(editor);
      if (lookup instanceof LookupImpl) {
        Runnable command = () -> ((LookupImpl)lookup).finishLookup(Lookup.NORMAL_SELECT_CHAR);
        Document doc = editor.getDocument();
        DocCommandGroupId group = DocCommandGroupId.noneGroupId(doc);
        CommandProcessor.getInstance().executeCommand(editor.getProject(), command, "Completion", group, UndoConfirmationPolicy.DEFAULT, doc);
      }
    }

    IdeEventQueue.getInstance().setEventCount(eventCount);
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, file);
      handler.invoke(project, editor, file, dataContext);
    }
    else {
      handler.invoke(project, elements, dataContext);
    }
  }

  protected boolean isEnabledOnDataContext(@NotNull DataContext dataContext) {
    return false;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(true);
    DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || isHidden()) {
      hideAction(e);
      return;
    }

    Editor editor = e.getData(CommonDataKeys.EDITOR);
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (file != null) {
      if (file instanceof PsiCompiledElement && disableOnCompiledElement() || !isAvailableForFile(file)) {
        hideAction(e);
        return;
      }
    }

    if (editor == null) {
      if (isAvailableInEditorOnly()) {
        hideAction(e);
        return;
      }
      final PsiElement[] elements = getPsiElementArray(dataContext);
      final boolean isEnabled = isEnabledOnDataContext(dataContext) || elements.length != 0 && isEnabledOnElements(elements);
      if (!isEnabled) {
        disableAction(e);
      }
      else {
        updateActionText(e);
      }
    }
    else {
      PsiElement element = findRefactoringTargetInEditor(dataContext);
      if (element != null) {
        boolean isEnabled = file != null && isAvailableOnElementInEditorAndFile(element, editor, file, dataContext, e.getPlace());
        if (!isEnabled) {
          disableAction(e);
        }
        else {
          updateActionText(e);
        }
      }
      else {
        hideAction(e);
      }
    }
  }

  protected PsiElement findRefactoringTargetInEditor(DataContext dataContext) {
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
    PsiElement element = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
    Language[] languages = dataContext.getData(LangDataKeys.CONTEXT_LANGUAGES);
    if (element == null || !isAvailableForLanguage(element.getLanguage())) {
      if (file == null || editor == null) {
        return null;
      }
      element = getElementAtCaret(editor, file);
    }

    if (element == null || element instanceof SyntheticElement || languages == null) {
      return null;
    }

    if (ContainerUtil.find(languages, myLanguageCondition) == null) {
      return null;
    }
    return element;
  }

  private void updateActionText(AnActionEvent e) {
    String actionText = getActionName(e.getDataContext());
    if (actionText != null) {
      e.getPresentation().setText(actionText);
    }
  }

  @Nullable
  protected String getActionName(@NotNull DataContext dataContext) {
    return null;
  }

  protected boolean disableOnCompiledElement() {
    return true;
  }

  private static void hideAction(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(false);
    disableAction(e);
  }

  protected boolean isHidden() {
    return false;
  }

  public static PsiElement getElementAtCaret(@NotNull final Editor editor, final PsiFile file) {
    final int offset = fixCaretOffset(editor);
    PsiElement element = file.findElementAt(offset);
    if (element == null && offset == file.getTextLength()) {
      element = file.findElementAt(offset - 1);
    }

    if (element instanceof PsiWhiteSpace) {
      element = file.findElementAt(element.getTextRange().getStartOffset() - 1);
    }
    return element;
  }

  private static int fixCaretOffset(@NotNull final Editor editor) {
    final int caret = editor.getCaretModel().getOffset();
    if (editor.getSelectionModel().hasSelection()) {
      if (caret == editor.getSelectionModel().getSelectionEnd()) {
        return Math.max(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd() - 1);
      }
    }

    return caret;
  }

  private static void disableAction(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }

  protected boolean isAvailableForLanguage(Language language) {
    return true;
  }

  protected boolean isAvailableForFile(PsiFile file) {
    return true;
  }

  @NotNull
  public static PsiElement[] getPsiElementArray(@NotNull DataContext dataContext) {
    PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext);
    if (psiElements == null || psiElements.length == 0) {
      PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      if (element != null) {
        psiElements = new PsiElement[]{element};
      }
    }

    if (psiElements == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> filtered = null;
    for (PsiElement element : psiElements) {
      if (element instanceof SyntheticElement) {
        if (filtered == null) filtered = new ArrayList<>(Collections.singletonList(element));
        filtered.remove(element);
      }
    }
    return filtered == null ? psiElements : PsiUtilCore.toPsiElementArray(filtered);
  }
}