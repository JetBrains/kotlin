// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.rename;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.scratch.ScratchFileType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.meta.PsiWritableMetaData;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * created at Nov 13, 2001
 *
 * @author Jeka, dsl
 */
public class PsiElementRenameHandler implements RenameHandler {
  private static final Logger LOG = Logger.getInstance(PsiElementRenameHandler.class);
  private static final ExtensionPointName<Condition<? super PsiElement>> VETO_RENAME_CONDITION_EP = ExtensionPointName.create("com.intellij.vetoRenameCondition");

  public static final DataKey<String> DEFAULT_NAME = DataKey.create("DEFAULT_NAME");

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, @NotNull DataContext dataContext) {
    PsiElement element = getElement(dataContext);
    if (element == null) {
      element = BaseRefactoringAction.getElementAtCaret(editor, file);
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String newName = DEFAULT_NAME.getData(dataContext);
      if (newName != null) {
        rename(element, project, element, editor, newName);
        return;
      }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    final PsiElement nameSuggestionContext = InjectedLanguageUtil.findElementAtNoCommit(file, editor.getCaretModel().getOffset());
    invoke(element, project, nameSuggestionContext, editor, shouldCheckInProject());
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    PsiElement element = elements.length == 1 ? elements[0] : null;
    if (element == null) element = getElement(dataContext);
    LOG.assertTrue(element != null);
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String newName = DEFAULT_NAME.getData(dataContext);
      LOG.assertTrue(newName != null);
      rename(element, project, element, editor, newName);
    }
    else {
      invoke(element, project, element, editor, shouldCheckInProject());
    }
  }

  protected boolean shouldCheckInProject() {
    return true;
  }

  public static void invoke(@NotNull PsiElement element, @NotNull Project project, PsiElement nameSuggestionContext, @Nullable Editor editor) {
    invoke(element, project, nameSuggestionContext, editor, true);
  }

  public static void invoke(@NotNull PsiElement element, @NotNull Project project, PsiElement nameSuggestionContext, @Nullable Editor editor, boolean checkInProject) {
    if (!canRename(project, editor, element)) {
      return;
    }

    VirtualFile contextFile = PsiUtilCore.getVirtualFile(nameSuggestionContext);

    if (checkInProject && nameSuggestionContext != null &&
        nameSuggestionContext.isPhysical() &&
        (contextFile == null || contextFile.getFileType() != ScratchFileType.INSTANCE) &&
        !PsiManager.getInstance(project).isInProject(nameSuggestionContext)) {
      final String message = "Selected element is used from non-project files. These usages won't be renamed. Proceed anyway?";
      if (ApplicationManager.getApplication().isUnitTestMode()) throw new CommonRefactoringUtil.RefactoringErrorHintException(message);
      if (Messages.showYesNoDialog(project, message,
                                   RefactoringBundle.getCannotRefactorMessage(null), Messages.getWarningIcon()) != Messages.YES) {
        return;
      }
    }

    FeatureUsageTracker.getInstance().triggerFeatureUsed("refactoring.rename");

    rename(element, project, nameSuggestionContext, editor);
  }

  public static boolean canRename(@NotNull Project project, Editor editor, PsiElement element) throws CommonRefactoringUtil.RefactoringErrorHintException {
    String message = element == null ? null : renameabilityStatus(project, element);
    if (StringUtil.isNotEmpty(message)) {
      showErrorMessage(project, editor, message);
      return false;
    }
    return true;
  }

  @Nullable
  private static String renameabilityStatus(@NotNull Project project, @NotNull PsiElement element) {
    boolean hasRenameProcessor = RenamePsiElementProcessor.forElement(element) != RenamePsiElementProcessor.DEFAULT;
    boolean hasWritableMetaData = element instanceof PsiMetaOwner && ((PsiMetaOwner)element).getMetaData() instanceof PsiWritableMetaData;

    if (!hasRenameProcessor && !hasWritableMetaData && !(element instanceof PsiNamedElement)) {
      return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.wrong.caret.position.symbol.to.rename"));
    }

    if (!PsiManager.getInstance(project).isInProject(element)) {
      if (element.isPhysical()) {
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
        if (!(virtualFile != null && NonProjectFileWritingAccessProvider.isWriteAccessAllowed(virtualFile, project))) {
          String message = RefactoringBundle.message("error.out.of.project.element", UsageViewUtil.getType(element));
          return RefactoringBundle.getCannotRefactorMessage(message);
        }
      }

      if (!element.isWritable()) {
        return RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("error.cannot.be.renamed"));
      }
    }

    if (InjectedLanguageUtil.isInInjectedLanguagePrefixSuffix(element)) {
      final String message = RefactoringBundle.message("error.in.injected.lang.prefix.suffix", UsageViewUtil.getType(element));
      return RefactoringBundle.getCannotRefactorMessage(message);
    }

    return null;
  }

  private static void showErrorMessage(@NotNull Project project, @Nullable Editor editor, @NotNull String message) {
    CommonRefactoringUtil.showErrorHint(project, editor, message, RefactoringBundle.message("rename.title"), null);
  }

  public static void rename(@NotNull PsiElement element, @NotNull Project project, PsiElement nameSuggestionContext, Editor editor) {
    rename(element, project, nameSuggestionContext, editor, null);
  }

  public static void rename(@NotNull PsiElement element, @NotNull Project project, PsiElement nameSuggestionContext, Editor editor, String defaultName) {
    RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(element);
    rename(element, project, nameSuggestionContext, editor, defaultName, processor);
  }

  public static void rename(@NotNull PsiElement element,
                            @NotNull Project project,
                            PsiElement nameSuggestionContext,
                            Editor editor,
                            String defaultName,
                            RenamePsiElementProcessor processor) {
    PsiElement substituted = processor.substituteElementToRename(element, editor);
    if (substituted == null || !canRename(project, editor, substituted)) return;

    RenameDialog dialog = processor.createRenameDialog(project, substituted, nameSuggestionContext, editor);

    if (defaultName == null && ApplicationManager.getApplication().isUnitTestMode()) {
      String[] strings = dialog.getSuggestedNames();
      if (strings != null && strings.length > 0) {
        Arrays.sort(strings);
        defaultName = strings[0];
      }
      else {
        defaultName = "undefined"; // need to avoid show dialog in test
      }
    }

    if (defaultName != null) {
      try {
        dialog.performRename(defaultName);
      }
      finally {
        dialog.close(DialogWrapper.CANCEL_EXIT_CODE); // to avoid dialog leak
      }
    }
    else {
      dialog.show();
    }
  }

  @Override
  public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    return !isVetoed(getElement(dataContext));
  }

  public static boolean isVetoed(PsiElement element) {
    if (element == null || 
        element instanceof SyntheticElement || 
        element instanceof PsiNamedElement && ((PsiNamedElement)element).getName() == null) {
      return true;
    }
    for(Condition<? super PsiElement> condition: VETO_RENAME_CONDITION_EP.getExtensionList()) {
      if (condition.value(element)) return true;
    }
    return false;
  }

  @Nullable
  public static PsiElement getElement(@NotNull DataContext dataContext) {
    PsiElement[] elementArray = BaseRefactoringAction.getPsiElementArray(dataContext);

    if (elementArray.length != 1) {
      return null;
    }
    return elementArray[0];
  }
}
