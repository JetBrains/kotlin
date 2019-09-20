// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.move;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveHandler implements RefactoringActionHandler {
  public static final String REFACTORING_NAME = RefactoringBundle.message("move.title");

  /**
   * called by an Action in AtomicAction when refactoring is invoked from Editor
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    int offset = editor.getCaretModel().getOffset();
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    PsiElement element = file.findElementAt(offset);
    if (element == null) {
      element = file;
    }

    PsiReference reference = findReferenceAtCaret(element, offset);
    if (reference != null) {
      PsiElement refElement = reference.resolve();
      for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
        if (refElement != null && delegate.tryToMove(refElement, project, dataContext, reference, editor)) {
          return;
        }
      }
    }

    List<MoveHandlerDelegate> candidateHandlers = findHandlersForLanguage(element);
    while (true) {
      if (element == null) {
        String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("the.caret.should.be.positioned.at.the.class.method.or.field.to.be.refactored"));
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, null);
        return;
      }

      for(MoveHandlerDelegate delegate: candidateHandlers) {
        if (delegate.tryToMove(element, project, dataContext, null, editor)) {
          return;
        }
      }

      element = element.getParent();
    }
  }

  private static PsiReference findReferenceAtCaret(PsiElement element, int caretOffset) {
    final TextRange range = element.getTextRange();
    if (range != null) {
      int relative = caretOffset - range.getStartOffset();
      return element.findReferenceAt(relative);
    }
    return null;
  }

  /**
   * called by an Action in AtomicAction
   */
  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    final PsiElement targetContainer = dataContext == null ? null : LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext);
    final Set<PsiElement> filesOrDirs = new HashSet<>();
    for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(dataContext) && delegate.isValidTarget(targetContainer, elements)) {
        delegate.collectFilesOrDirsFromContext(dataContext, filesOrDirs);
      }
    }
    if (!filesOrDirs.isEmpty()) {
      for (PsiElement element : elements) {
        if (element instanceof PsiDirectory) {
          filesOrDirs.add(element);
        }
        else {
          final PsiFile containingFile = element.getContainingFile();
          if (containingFile != null) {
            filesOrDirs.add(containingFile);
          }
        }
      }
      MoveFilesOrDirectoriesUtil
        .doMove(project, PsiUtilCore.toPsiElementArray(filesOrDirs), new PsiElement[]{targetContainer}, null);
      return;
    }
    doMove(project, elements, targetContainer, dataContext, null);
  }

  /**
   * must be invoked in AtomicAction
   */
  public static void doMove(Project project, @NotNull PsiElement[] elements, PsiElement targetContainer, DataContext dataContext, MoveCallback callback) {
    if (elements.length == 0) return;

    for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(elements, targetContainer, null)) {
        delegate.doMove(project, elements, delegate.adjustTargetForMove(dataContext, targetContainer), callback);
        break;
      }
    }
  }

  /**
   * Performs some extra checks (that canMove does not)
   * May replace some elements with others which actulaly shall be moved (e.g. directory->package)
   */
  @Nullable
  public static PsiElement[] adjustForMove(Project project, final PsiElement[] sourceElements, final PsiElement targetElement) {
    for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(sourceElements, targetElement, null)) {
        return delegate.adjustForMove(project, sourceElements, targetElement);
      }
    }
    return sourceElements;
  }

  /**
   * Must be invoked in AtomicAction
   * target container can be null => means that container is not determined yet and must be spacify by the user
   */
  public static boolean canMove(@NotNull PsiElement[] elements, PsiElement targetContainer) {
    return findDelegate(elements, targetContainer, null) != null;
  }

  @Nullable
  private static MoveHandlerDelegate findDelegate(@NotNull PsiElement[] elements, @Nullable PsiElement targetContainer, @Nullable PsiReference reference) {
    for (MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(elements, targetContainer, reference)) {
        return delegate;
      }
    }

    return null;
  }

  @Nullable
  public static String getActionName(@NotNull DataContext dataContext) {
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor != null) {
      Project project = dataContext.getData(CommonDataKeys.PROJECT);
      if (project == null) return null;
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return null;
      PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
      if (element == null) element = file;

      PsiReference reference = findReferenceAtCaret(element, editor.getCaretModel().getOffset());
      if (reference != null) {
        PsiElement refElement = reference.resolve();
        if (refElement != null) {
          MoveHandlerDelegate refDelegate = findDelegate(new PsiElement[]{refElement}, null, reference);
          if (refDelegate != null) {
            return refDelegate.getActionName(new PsiElement[] { refElement });
          }
        }
      }

      // invoke() uses regular findElementAtCaret() instead of BaseRefactoringAction.getElementAtCaret(), match it
      List<MoveHandlerDelegate> candidateHandlers = findHandlersForLanguage(element);
      while (element != null) {
        PsiElement[] elementArray = {element};
        for (MoveHandlerDelegate handler : candidateHandlers) {
          if (handler.canMove(elementArray, null, reference)) {
            return handler.getActionName(elementArray);
          }
        }
        element = element.getParent();
      }
      return null;
    }

    PsiElement[] elements = BaseRefactoringAction.getPsiElementArray(dataContext);
    List<MoveHandlerDelegate> delegates = MoveHandlerDelegate.EP_NAME.getExtensionList();
    for(MoveHandlerDelegate delegate: delegates) {
      if (delegate.canMove(elements, null, null)) return delegate.getActionName(elements);
    }

    return null;
  }

  @NotNull
  private static List<MoveHandlerDelegate> findHandlersForLanguage(@NotNull PsiElement element) {
    return ContainerUtil.filter(MoveHandlerDelegate.EP_NAME.getExtensionList(),
                                (delegate) -> delegate.supportsLanguage(element.getLanguage()));
  }

  public static boolean isValidTarget(final PsiElement psiElement, PsiElement[] elements) {
    if (psiElement != null) {
      for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate.isValidTarget(psiElement, elements)){
          return true;
        }
      }
    }

    return false;
  }

  public static boolean canMove(DataContext dataContext) {
    for (MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(dataContext)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isMoveRedundant(PsiElement source, PsiElement target) {
    for(MoveHandlerDelegate delegate: MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.isMoveRedundant(source, target)) return true;
    }
    return false;
  }
}
