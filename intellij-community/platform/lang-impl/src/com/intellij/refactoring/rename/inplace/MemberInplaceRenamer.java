// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.inplace;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.Language;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.FinishMarkAction;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MemberInplaceRenamer extends VariableInplaceRenamer {
  private final PsiElement mySubstituted;
  private RangeMarker mySubstitutedRange;

  public MemberInplaceRenamer(@NotNull PsiNamedElement elementToRename,
                              @Nullable PsiElement substituted,
                              @NotNull Editor editor) {
    this(elementToRename, substituted, editor, elementToRename.getName(), elementToRename.getName());
  }

  public MemberInplaceRenamer(@NotNull PsiNamedElement elementToRename,
                              @Nullable PsiElement substituted,
                              @NotNull Editor editor,
                              @Nullable String initialName,
                              @Nullable String oldName) {
    super(elementToRename, editor, elementToRename.getProject(), initialName, oldName);
    mySubstituted = substituted;
    if (mySubstituted != null && mySubstituted != myElementToRename && mySubstituted.getTextRange() != null) {
      final PsiFile containingFile = mySubstituted.getContainingFile();
      if (!notSameFile(containingFile.getVirtualFile(), containingFile)) {
        mySubstitutedRange = myEditor.getDocument().createRangeMarker(mySubstituted.getTextRange());
        mySubstitutedRange.setGreedyToLeft(true);
        mySubstitutedRange.setGreedyToRight(true);
      }
    }
    else {
      mySubstitutedRange = null;
    }

    showDialogAdvertisement("RenameElement");
  }

  @NotNull
  @Override
  protected VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
    return new MemberInplaceRenamer(variable, getSubstituted(), editor, initialName, myOldName);
  }

  @Override
  protected boolean acceptReference(PsiReference reference) {
    final PsiElement element = reference.getElement();
    final TextRange textRange = getRangeToRename(reference);
    final String referenceText = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
    return Comparing.strEqual(referenceText, myElementToRename.getName());
  }

  @Override
  protected PsiElement checkLocalScope() {
    PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (currentFile != null) {
      return currentFile;
    }
    return super.checkLocalScope();
  }

  @Override
  protected PsiElement getNameIdentifier() {
    final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (currentFile == myElementToRename.getContainingFile()){
      return super.getNameIdentifier();
    }
    if (currentFile != null) {
      int offset = myEditor.getCaretModel().getOffset();
      offset = TargetElementUtil.adjustOffset(currentFile, myEditor.getDocument(), offset);
      final PsiElement elementAt = currentFile.findElementAt(offset);
      if (elementAt != null) {
        final PsiElement referenceExpression = elementAt.getParent();
        if (referenceExpression != null) {
          final PsiReference reference = referenceExpression.getReference();
          if (reference != null && reference.resolve() == myElementToRename) {
            return elementAt;
          }
        }
      }
      return null;
    }
    return null;
  }

  @Override
  protected boolean isIdentifier(String newName, Language language) {
    PsiNamedElement namedElement = getVariable();
    return namedElement != null ? RenameUtil.isValidName(myProject, namedElement, newName) : super.isIdentifier(newName, language);
  }

  @Override
  protected Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
    final ArrayList<PsiReference> references = new ArrayList<>(super.collectRefs(referencesSearchScope));
    final PsiNamedElement variable = getVariable();
    if (variable != null) {
      final PsiElement substituted = getSubstituted();
      if (substituted != null && substituted != variable) {
        references.addAll(ReferencesSearch.search(substituted, referencesSearchScope, false).findAll());
      }
    }
    return references;
  }

  @Override
  protected boolean notSameFile(@Nullable VirtualFile file, @NotNull PsiFile containingFile) {
    final PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (currentFile == null) return true;
    InjectedLanguageManager manager = InjectedLanguageManager.getInstance(containingFile.getProject());
    return manager.getTopLevelFile(containingFile) != manager.getTopLevelFile(currentFile);
  }

  @Override
  protected SearchScope getReferencesSearchScope(VirtualFile file) {
    PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    return currentFile != null ? new LocalSearchScope(currentFile)
                               : ProjectScope.getProjectScope(myProject);
  }

  @Override
  protected boolean appendAdditionalElement(Collection<PsiReference> refs, Collection<Pair<PsiElement, TextRange>> stringUsages) {
    boolean showChooser = super.appendAdditionalElement(refs, stringUsages);
    PsiNamedElement variable = getVariable();
    if (variable != null) {
      final PsiElement substituted = getSubstituted();
      if (substituted != null) {
        appendAdditionalElement(stringUsages, variable, substituted);
        RenamePsiElementProcessor processor = RenamePsiElementProcessor.forElement(substituted);
        final HashMap<PsiElement, String> allRenames = new HashMap<>();
        PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        processor.prepareRenaming(substituted, "", allRenames, new LocalSearchScope(currentFile));
        for (PsiElement element : allRenames.keySet()) {
          appendAdditionalElement(stringUsages, variable, element);
        }
      }
    }
    return showChooser;
  }

  @Override
  protected boolean shouldCreateSnapshot() {
    return false;
  }

  @Override
  protected String getRefactoringId() {
    return null;
  }

  private void appendAdditionalElement(Collection<? super Pair<PsiElement, TextRange>> stringUsages,
                                       PsiNamedElement variable,
                                       PsiElement element) {
    if (element != variable && element instanceof PsiNameIdentifierOwner &&
        !notSameFile(null, element.getContainingFile())) {
      final PsiElement identifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
      if (identifier != null) {
        stringUsages.add(Pair.create(identifier, new TextRange(0, identifier.getTextLength())));
      }
    }
  }

  @Override
  protected void performRefactoringRename(final String newName,
                                          final StartMarkAction markAction) {
    try {
      final PsiNamedElement variable = getVariable();
      if (variable != null && !newName.equals(myOldName)) {
        if (isIdentifier(newName, variable.getLanguage())) {
          final PsiElement substituted = getSubstituted();
          if (substituted == null) {
            return;
          }

          Runnable performRunnable = () -> {
            if (DumbService.isDumb(myProject)) {
              DumbService.getInstance(myProject).showDumbModeNotification("Refactorings cannot be performed while indexing is in progress");
              return;
            }

            final String commandName = RefactoringBundle.message("renaming.0.1.to.2",
                                                                 UsageViewUtil.getType(variable),
                                                                 DescriptiveNameUtil.getDescriptiveName(variable), newName);
            CommandProcessor.getInstance().executeCommand(myProject, () -> {
              performRenameInner(substituted, newName);
              PsiDocumentManager.getInstance(myProject).commitAllDocuments();
            }, commandName, null);
          };

          if (ApplicationManager.getApplication().isUnitTestMode()) {
            performRunnable.run();
          }
          else {
            TransactionGuard.getInstance().submitTransactionLater(myProject, performRunnable);
          }
        }
      }
    }
    finally {
      try {
        ((EditorImpl)InjectedLanguageUtil.getTopLevelEditor(myEditor)).stopDumbLater();
      }
      finally {
        FinishMarkAction.finish(myProject, myEditor, markAction);
      }
    }
  }

  protected void performRenameInner(PsiElement element, String newName) {
    final RenameProcessor renameProcessor = createRenameProcessor(element, newName);
    for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
      if (factory.getOptionName() != null && factory.isEnabled() && factory.isApplicable(element)) {
        renameProcessor.addRenamerFactory(factory);
      }
    }
    renameProcessor.run();
  }

  protected RenameProcessor createRenameProcessor(PsiElement element, String newName) {
    return new MyRenameProcessor(element, newName);
  }

  protected void restoreCaretOffsetAfterRename() {
    if (myBeforeRevert != null) {
      if (!myEditor.isDisposed()) {
        myEditor.getCaretModel().moveToOffset(myBeforeRevert.getEndOffset());
      }
      myBeforeRevert.dispose();
    }
  }

  @Override
  protected void collectAdditionalElementsToRename(@NotNull List<Pair<PsiElement, TextRange>> stringUsages) {
    //do not highlight non-code usages in file
  }

  @Override
  protected void revertStateOnFinish() {
    final Editor editor = InjectedLanguageUtil.getTopLevelEditor(myEditor);
    if (editor == FileEditorManager.getInstance(myProject).getSelectedTextEditor()) {
      ((EditorImpl)editor).startDumb();
    }
    revertState();
  }

  @Nullable
  public PsiElement getSubstituted() {
    if (mySubstituted != null && mySubstituted.isValid()){
      if (mySubstituted instanceof PsiNameIdentifierOwner) {
        if (Comparing.strEqual(myOldName, ((PsiNameIdentifierOwner)mySubstituted).getName())) return mySubstituted;

        final RangeMarker rangeMarker = mySubstitutedRange != null ? mySubstitutedRange : myRenameOffset;
        if (rangeMarker != null)
          return PsiTreeUtil.findElementOfClassAtRange(mySubstituted.getContainingFile(), rangeMarker.getStartOffset(), rangeMarker.getEndOffset(), PsiNameIdentifierOwner.class);
      }
      return mySubstituted;
    }
    if (mySubstitutedRange != null) {
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
      if (psiFile != null) {
        return PsiTreeUtil.findElementOfClassAtRange(psiFile, mySubstitutedRange.getStartOffset(), mySubstitutedRange.getEndOffset(), PsiNameIdentifierOwner.class);
      }
    }
    return getVariable();
  }

  protected class MyRenameProcessor extends RenameProcessor {
    public MyRenameProcessor(PsiElement element, String newName) {
      this(element, newName, RenamePsiElementProcessor.forElement(element));
    }

    public MyRenameProcessor(PsiElement element, String newName, RenamePsiElementProcessor elementProcessor) {
      super(MemberInplaceRenamer.this.myProject, element, newName, elementProcessor.isToSearchInComments(element),
            elementProcessor.isToSearchForTextOccurrences(element) && TextOccurrencesUtil.isSearchTextOccurrencesEnabled(element));
    }

    @Nullable
    @Override
    protected String getRefactoringId() {
      return "refactoring.inplace.rename";
    }

    @Override
    public void doRun() {
      try {
        super.doRun();
      }
      finally {
        restoreCaretOffsetAfterRename();
      }
    }
  }
}