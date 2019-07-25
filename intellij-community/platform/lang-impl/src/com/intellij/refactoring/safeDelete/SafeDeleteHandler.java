// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.safeDelete;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author dsl
 */
public class SafeDeleteHandler implements RefactoringActionHandler {
  public static final String REFACTORING_NAME = RefactoringBundle.message("safe.delete.title");

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    if (element == null || !SafeDeleteProcessor.validElement(element)) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("is.not.supported.in.the.current.context", REFACTORING_NAME));
      CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, "refactoring.safeDelete");
      return;
    }
    invoke(project, new PsiElement[]{element}, dataContext);
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    invoke(project, elements, LangDataKeys.MODULE.getData(dataContext), true, null, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates) {
    invoke(project, elements, checkDelegates, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, boolean checkDelegates, @Nullable final Runnable successRunnable) {
    invoke(project, elements, null, checkDelegates, successRunnable, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, @Nullable Module module, boolean checkDelegates,
                            @Nullable final Runnable successRunnable) {
    invoke(project, elements, module, checkDelegates, successRunnable, null);
  }

  public static void invoke(final Project project, PsiElement[] elements, @Nullable Module module, boolean checkDelegates,
                            @Nullable final Runnable successRunnable, @Nullable final  Runnable afterRefactoring) {
    invoke(project, elements, module, checkDelegates, successRunnable, afterRefactoring, false);
  }

  public static void invoke(final Project project, PsiElement[] elements, @Nullable Module module, boolean checkDelegates,
                            @Nullable final Runnable successRunnable, @Nullable final  Runnable afterRefactoring, boolean silent) {
    for (PsiElement element : elements) {
      if (!SafeDeleteProcessor.validElement(element)) {
        return;
      }
    }
    final PsiElement[] temptoDelete = PsiTreeUtil.filterAncestors(elements);
    Set<PsiElement> elementsSet = new HashSet<>(Arrays.asList(temptoDelete));
    Set<PsiElement> fullElementsSet = new LinkedHashSet<>();

    if (checkDelegates) {
      for (PsiElement element : temptoDelete) {
        boolean found = false;
        for(SafeDeleteProcessorDelegate delegate: SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
          if (delegate.handlesElement(element)) {
            found = true;
            Collection<? extends PsiElement> addElements = delegate instanceof SafeDeleteProcessorDelegateBase
                                                           ? ((SafeDeleteProcessorDelegateBase)delegate).getElementsToSearch(element, module, elementsSet)
                                                           : delegate.getElementsToSearch(element, elementsSet);
            if (addElements == null) return;
            fullElementsSet.addAll(addElements);
            break;
          }
        }
        if (!found) {
          fullElementsSet.add(element);
        }
      }
    } else {
      ContainerUtil.addAll(fullElementsSet, temptoDelete);
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, fullElementsSet, true)) return;

    final PsiElement[] elementsToDelete = PsiUtilCore.toPsiElementArray(fullElementsSet);

    if (ApplicationManager.getApplication().isUnitTestMode() || silent) {
      RefactoringSettings settings = RefactoringSettings.getInstance();
      final SafeDeleteProcessor processor =
        SafeDeleteProcessor.createInstance(project, null, elementsToDelete, settings.SAFE_DELETE_SEARCH_IN_COMMENTS,
                                           settings.SAFE_DELETE_SEARCH_IN_NON_JAVA, true);
      if (afterRefactoring != null) processor.setAfterRefactoringCallback(afterRefactoring);
      processor.run();
      if (successRunnable != null) successRunnable.run();
    }
    else {
      final SafeDeleteDialog.Callback callback = new SafeDeleteDialog.Callback() {
        @Override
        public void run(final SafeDeleteDialog dialog) {
          final SafeDeleteProcessor processor = SafeDeleteProcessor.createInstance(project, () -> {
            if (successRunnable != null) {
              successRunnable.run();
            }
            dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
          }, elementsToDelete, dialog.isSearchInComments(), dialog.isSearchForTextOccurences(), true);
          if (afterRefactoring != null) processor.setAfterRefactoringCallback(afterRefactoring);
          processor.run();
        }
      };

      SafeDeleteDialog dialog = new SafeDeleteDialog(project, elementsToDelete, callback);
      dialog.show();
    }
  }
}
