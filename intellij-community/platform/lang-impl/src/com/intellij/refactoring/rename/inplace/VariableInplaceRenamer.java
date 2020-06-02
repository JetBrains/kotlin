// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.IdeBundle;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.LangBundle;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.FinishMarkAction;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringEventData;
import com.intellij.refactoring.listeners.RefactoringEventListener;
import com.intellij.refactoring.rename.*;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author ven
 */
public class VariableInplaceRenamer extends InplaceRefactoring {
  public static final LanguageExtension<ResolveSnapshotProvider> INSTANCE = new LanguageExtension<>(
    "com.intellij.rename.inplace.resolveSnapshotProvider"
  );
  private ResolveSnapshotProvider.ResolveSnapshot mySnapshot;
  private TextRange mySelectedRange;
  protected Language myLanguage;

  public VariableInplaceRenamer(@NotNull PsiNamedElement elementToRename,
                                @NotNull Editor editor) {
    this(elementToRename, editor, elementToRename.getProject());
  }

  public VariableInplaceRenamer(@Nullable PsiNamedElement elementToRename,
                                @NotNull Editor editor,
                                @NotNull Project project) {
    this(elementToRename, editor, project, elementToRename != null ? elementToRename.getName() : null,
         elementToRename != null ? elementToRename.getName() : null);
  }

  public VariableInplaceRenamer(@Nullable PsiNamedElement elementToRename,
                                @NotNull Editor editor,
                                @NotNull Project project,
                                @Nullable String initialName,
                                @Nullable String oldName) {
    super(editor, elementToRename, project, initialName, oldName);
  }

  @Override
  protected boolean startsOnTheSameElement(RefactoringActionHandler handler, PsiElement element) {
    return super.startsOnTheSameElement(handler, element) && handler instanceof VariableInplaceRenameHandler;
  }

  public boolean performInplaceRename() {
    return performInplaceRefactoring(null);
  }

  @Override
  protected void collectAdditionalElementsToRename(@NotNull final List<Pair<PsiElement, TextRange>> stringUsages) {
    final String stringToSearch = myElementToRename.getName();
    final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (!StringUtil.isEmptyOrSpaces(stringToSearch)) {
      TextOccurrencesUtil.processUsagesInStringsAndComments(
        myElementToRename, GlobalSearchScope.projectScope(myElementToRename.getProject()),
        stringToSearch, true, (psiElement, textRange) -> {
          if (psiElement.getContainingFile() == currentFile) {
            stringUsages.add(Pair.create(psiElement, textRange));
          }
          return true;
        });
    }
  }

  @Override
  protected boolean buildTemplateAndStart(final Collection<PsiReference> refs,
                                          Collection<Pair<PsiElement, TextRange>> stringUsages,
                                          final PsiElement scope,
                                          final PsiFile containingFile) {
    if (appendAdditionalElement(refs, stringUsages)) {
      return super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
    }
    else {
      final RenameChooser renameChooser = new RenameChooser(myEditor) {
        @Override
        protected void runRenameTemplate(Collection<Pair<PsiElement, TextRange>> stringUsages) {
          VariableInplaceRenamer.super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);
        }
      };
      renameChooser.showChooser(refs, stringUsages);
    }
    return true;
  }

  protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
    return stringUsages.isEmpty() || StartMarkAction.canStart(myProject) != null;
  }

  protected boolean shouldCreateSnapshot() {
    return true;
  }

  protected String getRefactoringId() {
    return "refactoring.rename";
  }

  @Override
  protected void beforeTemplateStart() {
    super.beforeTemplateStart();
    myLanguage = myScope.getLanguage();
    if (shouldCreateSnapshot()) {
      final ResolveSnapshotProvider resolveSnapshotProvider = INSTANCE.forLanguage(myLanguage);
      mySnapshot = resolveSnapshotProvider != null ? resolveSnapshotProvider.createSnapshot(myScope) : null;
    }

    final SelectionModel selectionModel = myEditor.getSelectionModel();
    mySelectedRange =
      selectionModel.hasSelection() ? new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd()) : null;
  }

  @Override
  protected void restoreSelection() {
    if (mySelectedRange != null) {
      Editor editor = InjectedLanguageUtil.getTopLevelEditor(myEditor);
      TextRange selectedRange;
      if (myEditor instanceof EditorWindow) {
        PsiFile injected = ((EditorWindow)myEditor).getInjectedFile();
        selectedRange = InjectedLanguageManager.getInstance(editor.getProject()).injectedToHost(injected, mySelectedRange);
      } else {
        selectedRange = mySelectedRange;
      }
      TemplateState state = TemplateManagerImpl.getTemplateState(editor);

      if (state != null) {
        for (int i = 0; i < state.getSegmentsCount(); i++) {
          final TextRange segmentRange = state.getSegmentRange(i);
          TextRange intersection = segmentRange.intersection(selectedRange);
          if (intersection != null) {
            editor.getSelectionModel().setSelection(intersection.getStartOffset(), intersection.getEndOffset());
            return;
          }
        }
      }
      myEditor.getSelectionModel().setSelection(mySelectedRange.getStartOffset(), mySelectedRange.getEndOffset());
    }
    else if (!shouldSelectAll()) {
      myEditor.getSelectionModel().removeSelection();
    }
  }

  @Override
  protected int restoreCaretOffset(int offset) {
    if (myCaretRangeMarker.isValid()) {
      if (myCaretRangeMarker.getStartOffset() <= offset && myCaretRangeMarker.getEndOffset() >= offset) {
        return offset;
      }
      return myCaretRangeMarker.getEndOffset();
    }
    return offset;
  }

  @Override
  protected boolean shouldSelectAll() {
    if (myEditor.getSettings().isPreselectRename()) return true;
    final Boolean selectAll = myEditor.getUserData(RenameHandlerRegistry.SELECT_ALL);
    return selectAll != null && selectAll.booleanValue();
  }

  @NotNull
  protected VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
    return new VariableInplaceRenamer(variable, editor, myProject, initialName, myOldName);
  }

  protected void performOnInvalidIdentifier(final String newName, final LinkedHashSet<String> nameSuggestions) {
    final PsiNamedElement variable = getVariable();
    if (variable != null) {
      final int offset = variable.getTextOffset();
      restoreCaretOffset(offset);
      if (ApplicationManager.getApplication().isUnitTestMode()) {
        return;
      }
      JBPopupFactory.getInstance()
        .createConfirmation(LangBundle.message("popup.title.inserted.identifier.valid"), IdeBundle.message("button.continue.editing"),
                            CommonBundle.getCancelButtonText(),
                            () -> createInplaceRenamerToRestart(variable, myEditor, newName).performInplaceRefactoring(nameSuggestions), 0).showInBestPositionFor(myEditor);
    }
  }

  protected void renameSynthetic(String newName) {
  }

  protected void performRefactoringRename(final String newName,
                                          final StartMarkAction markAction) {
    final String refactoringId = getRefactoringId();
    try {
      PsiNamedElement elementToRename = getVariable();
      if (refactoringId != null) {
        final RefactoringEventData beforeData = new RefactoringEventData();
        beforeData.addElement(elementToRename);
        beforeData.addStringProperties(myOldName);
        myProject.getMessageBus()
          .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringStarted(refactoringId, beforeData);
      }
      if (!isIdentifier(newName, myLanguage)) {
        return;
      }
      if (elementToRename != null) {
        WriteCommandAction.writeCommandAction(myProject).withName(getCommandName()).run(() -> renameSynthetic(newName));
      }
      for (AutomaticRenamerFactory renamerFactory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
        if (elementToRename != null && renamerFactory.isApplicable(elementToRename)) {
          final List<UsageInfo> usages = new ArrayList<>();
          final AutomaticRenamer renamer =
            renamerFactory.createRenamer(elementToRename, newName, new ArrayList<>());
          if (renamer.hasAnythingToRename()) {
            if (!ApplicationManager.getApplication().isUnitTestMode()) {
              final AutomaticRenamingDialog renamingDialog = new AutomaticRenamingDialog(myProject, renamer);
              if (!renamingDialog.showAndGet()) {
                continue;
              }
            }

            final Runnable runnable = () -> ApplicationManager.getApplication().runReadAction(() -> renamer.findUsages(usages, false, false));

            if (!ProgressManager.getInstance()
              .runProcessWithProgressSynchronously(runnable, RefactoringBundle.message("searching.for.variables"), true, myProject)) {
              return;
            }

            if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, PsiUtilCore.toPsiElementArray(renamer.getElements()))) return;
            final ThrowableRunnable<RuntimeException> performAutomaticRename = () -> {
              CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
              final UsageInfo[] usageInfos = usages.toArray(UsageInfo.EMPTY_ARRAY);
              final MultiMap<PsiElement, UsageInfo> classified = RenameProcessor.classifyUsages(renamer.getElements(), usageInfos);
              for (final PsiNamedElement element : renamer.getElements()) {
                final String newElementName = renamer.getNewName(element);
                if (newElementName != null) {
                  final Collection<UsageInfo> infos = classified.get(element);
                  RenameUtil.doRename(element, newElementName, infos.toArray(UsageInfo.EMPTY_ARRAY), myProject, RefactoringElementListener.DEAF);
                }
              }
            };
            if (ApplicationManager.getApplication().isUnitTestMode()) {
              WriteCommandAction.writeCommandAction(myProject).withName(getCommandName()).run(performAutomaticRename);
            } else {
              ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.writeCommandAction(myProject).withName(getCommandName()).run(performAutomaticRename));
            }
          }
        }
      }
    }
    finally {

      if (refactoringId != null) {
        final RefactoringEventData afterData = new RefactoringEventData();
        afterData.addElement(getVariable());
        afterData.addStringProperties(newName);
        myProject.getMessageBus()
          .syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC).refactoringDone(refactoringId, afterData);
      }

      try {
        ((EditorImpl)InjectedLanguageUtil.getTopLevelEditor(myEditor)).stopDumbLater();
      }
      finally {
        FinishMarkAction.finish(myProject, myEditor, markAction);
      }
    }
  }

  @Override
  protected String getCommandName() {
    return RefactoringBundle.message("renaming.command.name", myInitialName);
  }

  @Override
  protected boolean performRefactoring() {
    boolean bind = false;
    if (myInsertedName != null) {
      final CommandProcessor commandProcessor = CommandProcessor.getInstance();
      if (commandProcessor.getCurrentCommand() != null && getVariable() != null) {
        commandProcessor.setCurrentCommandName(getCommandName());
      }

      bind = true;
      if (!isIdentifier(myInsertedName, myLanguage)) {
        performOnInvalidIdentifier(myInsertedName, myNameSuggestions);
      }
      else if (mySnapshot != null) {
        ApplicationManager.getApplication().runWriteAction(() -> mySnapshot.apply(myInsertedName));
      }
      performRefactoringRename(myInsertedName, myMarkAction);
    }
    return bind;
  }

  @Override
  public void finish(boolean success) {
    super.finish(success);
    if (success) {
      revertStateOnFinish();
    }
    else {
      ((EditorImpl)InjectedLanguageUtil.getTopLevelEditor(myEditor)).stopDumbLater();
    }
  }

  protected void revertStateOnFinish() {
    if (myInsertedName == null || !isIdentifier(myInsertedName, myLanguage)) {
      revertState();
    }
  }
}