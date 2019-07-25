// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.lang;

import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.find.FindManager;
import com.intellij.ide.TitledHandler;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileSystemItemUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ReplacePromptDialog;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;

public abstract class ExtractIncludeFileBase<T extends PsiElement> implements RefactoringActionHandler, TitledHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.lang.ExtractIncludeFileBase");
  private static final String REFACTORING_NAME = RefactoringBundle.message("extract.include.file.title");
  protected PsiFile myIncludingFile;
  public static final String HELP_ID = "refactoring.extractInclude";

  public boolean isAvailableForFile(@NotNull PsiFile file) {
    return true;
  }

  private static class IncludeDuplicate<E extends PsiElement> {
    private final SmartPsiElementPointer<E> myStart;
    private final SmartPsiElementPointer<E> myEnd;

    private IncludeDuplicate(E start, E end) {
      myStart = SmartPointerManager.getInstance(start.getProject()).createSmartPsiElementPointer(start);
      myEnd = SmartPointerManager.getInstance(start.getProject()).createSmartPsiElementPointer(end);
    }

    E getStart() {
      return myStart.getElement();
    }

    E getEnd() {
      return myEnd.getElement();
    }
  }


  protected abstract void doReplaceRange(final String includePath, final T first, final T last);

  @NotNull
  protected String doExtract(final PsiDirectory targetDirectory,
                             final String targetfileName,
                             final T first,
                             final T last,
                             final Language includingLanguage) throws IncorrectOperationException {
    final PsiFile file = targetDirectory.createFile(targetfileName);
    Project project = targetDirectory.getProject();
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    final Document document = documentManager.getDocument(file);
    document.replaceString(0, document.getTextLength(), first.getText().trim());
    documentManager.commitDocument(document);
    CodeStyleManager.getInstance(PsiManager.getInstance(project).getProject()).reformat(file);  //TODO: adjustLineIndent

    final String relativePath = PsiFileSystemItemUtil.findRelativePath(first.getContainingFile(), file);
    if (relativePath == null) throw new IncorrectOperationException("Cannot extract!");
    return relativePath;
  }

  protected abstract boolean verifyChildRange (final T first, final T last);

  private void replaceDuplicates(final String includePath,
                                   final List<IncludeDuplicate<T>> duplicates,
                                   final Editor editor,
                                   final Project project) {
    if (duplicates.size() > 0) {
      final String message = RefactoringBundle.message("idea.has.found.fragments.that.can.be.replaced.with.include.directive",
                                                  ApplicationNamesInfo.getInstance().getProductName());
      final int exitCode = Messages.showYesNoDialog(project, message, getRefactoringName(), Messages.getInformationIcon());
      if (exitCode == Messages.YES) {
        CommandProcessor.getInstance().executeCommand(project, () -> {
          boolean replaceAll = false;
          for (IncludeDuplicate<T> pair : duplicates) {
            if (!replaceAll) {

              highlightInEditor(project, pair, editor);

              ReplacePromptDialog promptDialog = new ReplacePromptDialog(false, RefactoringBundle.message("replace.fragment"), project);
              promptDialog.show();
              final int promptResult = promptDialog.getExitCode();
              if (promptResult == FindManager.PromptResult.SKIP) continue;
              if (promptResult == FindManager.PromptResult.CANCEL) break;

              if (promptResult == FindManager.PromptResult.OK) {
                doReplaceRange(includePath, pair.getStart(), pair.getEnd());
              }
              else if (promptResult == FindManager.PromptResult.ALL) {
                doReplaceRange(includePath, pair.getStart(), pair.getEnd());
                replaceAll = true;
              }
              else {
                LOG.error("Unknown return status");
              }
            }
            else {
              doReplaceRange(includePath, pair.getStart(), pair.getEnd());
            }
          }
        }, RefactoringBundle.message("remove.duplicates.command"), null);
      }
    }
  }

  private static void highlightInEditor(final Project project, final IncludeDuplicate pair, final Editor editor) {
    final HighlightManager highlightManager = HighlightManager.getInstance(project);
    EditorColorsManager colorsManager = EditorColorsManager.getInstance();
    TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    final int startOffset = pair.getStart().getTextRange().getStartOffset();
    final int endOffset = pair.getEnd().getTextRange().getEndOffset();
    highlightManager.addRangeHighlight(editor, startOffset, endOffset, attributes, true, null);
    final LogicalPosition logicalPosition = editor.offsetToLogicalPosition(startOffset);
    editor.getScrollingModel().scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE);
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
  }

  @NotNull
  protected Language getLanguageForExtract(PsiElement firstExtracted) {
    return firstExtracted.getLanguage();
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file, DataContext dataContext) {
    try {
      myIncludingFile = file;
      doInvoke(project, editor, file);
    }
    finally {
      myIncludingFile = null;
    }
  }

  protected void doInvoke(@NotNull Project project, Editor editor, PsiFile file) {
    if (!editor.getSelectionModel().hasSelection()) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("no.selection"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
      return;
    }
    final int start = editor.getSelectionModel().getSelectionStart();
    final int end = editor.getSelectionModel().getSelectionEnd();

    final Pair<T, T> children = findPairToExtract(start, end);
    if (children == null) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("selection.does.not.form.a.fragment.for.extraction"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
      return;
    }

    if (!verifyChildRange(children.getFirst(), children.getSecond())) {
      String message = RefactoringBundle.getCannotRefactorMessage(RefactoringBundle.message("cannot.extract.selected.elements.into.include.file"));
      CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
      return;
    }

    FileType fileType = getLanguageForExtract(children.getFirst()).getAssociatedFileType();
    if (fileType == null) {
      String message = RefactoringBundle.message("the.language.for.selected.elements.has.no.associated.file.type");
      CommonRefactoringUtil.showErrorHint(project, editor, message, getRefactoringName(), HELP_ID);
      return;
    }

    if (!CommonRefactoringUtil.checkReadOnlyStatus(project, file)) return;


    Pair<PsiDirectory, String> directoryAndFileName = getTargetDirectoryAndFileName(file, fileType, children);
    if (directoryAndFileName.first == null || directoryAndFileName.second == null) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        final List<IncludeDuplicate<T>> duplicates = new ArrayList<>();
        final T first = children.getFirst();
        final T second = children.getSecond();
        PsiEquivalenceUtil.findChildRangeDuplicates(first, second, file, (start1, end1) -> duplicates.add(
          new IncludeDuplicate<>((T)start1, (T)end1)));
        String includePath = processPrimaryFragment(first, second, directoryAndFileName.first, directoryAndFileName.second, file);
        editor.getCaretModel().moveToOffset(first.getTextRange().getStartOffset());

        ApplicationManager.getApplication().invokeLater(() -> replaceDuplicates(includePath, duplicates, editor, project));
      }
      catch (IncorrectOperationException e) {
        CommonRefactoringUtil.showErrorMessage(getRefactoringName(), e.getMessage(), null, project);
      }

      editor.getSelectionModel().removeSelection();
    }), getRefactoringName(), null);
  }

  private static PsiDirectory ourTargetDirectory = null;
  private static String ourTargetFileName = null;

  @TestOnly
  public static void setTestingTargetFile(@Nullable PsiDirectory targetDirectory,
                                          @Nullable String targetFileName,
                                          @NotNull Disposable parentDisposable) {
    ourTargetFileName = targetFileName;
    ourTargetDirectory = targetDirectory;
    Disposer.register(parentDisposable, () -> {
      ourTargetFileName = null;
      ourTargetDirectory = null;
    });
  }

  @NotNull
  private Pair<PsiDirectory, String> getTargetDirectoryAndFileName(@NotNull PsiFile file, FileType fileType, @NotNull Pair<T, T> children) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return Pair.create(ourTargetDirectory, ourTargetFileName);
    }

    ExtractIncludeDialog dialog = createDialog(file.getContainingDirectory(), getExtractExtension(fileType, children.first));
    dialog.show();
    if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return Pair.create(null, null);
    }
    PsiDirectory targetDirectory = dialog.getTargetDirectory();
    String targetFileName = dialog.getTargetFileName();
    LOG.assertTrue(targetDirectory != null && targetFileName != null);
    return Pair.create(targetDirectory, targetFileName);
  }

  protected ExtractIncludeDialog createDialog(final PsiDirectory containingDirectory, final String extractExtension) {
    return new ExtractIncludeDialog(containingDirectory, extractExtension);
  }

  @Nullable
  protected abstract Pair<T, T> findPairToExtract(int start, int end);

  @NonNls
  protected String getExtractExtension(final FileType extractFileType, final T first) {
    return extractFileType.getDefaultExtension();
  }

  @Deprecated
  @TestOnly
  public boolean isValidRange(final T firstToExtract, final T lastToExtract) {
    return verifyChildRange(firstToExtract, lastToExtract);
  }

  public String processPrimaryFragment(final T firstToExtract,
                                       final T lastToExtract,
                                       final PsiDirectory targetDirectory,
                                       final String targetfileName,
                                       final PsiFile srcFile) throws IncorrectOperationException {
    final String includePath = doExtract(targetDirectory, targetfileName, firstToExtract, lastToExtract,
                                         srcFile.getLanguage());

    doReplaceRange(includePath, firstToExtract, lastToExtract);
    return includePath;
  }

  @Override
  public String getActionTitle() {
    return "Extract Include File...";
  }

  protected String getRefactoringName() {
    return REFACTORING_NAME;
  }
}
