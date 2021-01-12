// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.extractMethod;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.find.FindManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.ReplacePromptDialog;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public final class ExtractMethodHelper {
  public static void processDuplicates(@NotNull final PsiElement callElement,
                                       @NotNull final PsiElement generatedMethod,
                                       @NotNull final List<PsiElement> scope,
                                       @NotNull final SimpleDuplicatesFinder finder,
                                       @NotNull final Editor editor,
                                       @NotNull final Consumer<? super Pair<SimpleMatch, PsiElement>> replacer) {
    finder.setReplacement(callElement);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      replaceDuplicates(callElement, editor, replacer, finder.findDuplicates(scope, generatedMethod));
      return;
    }
    final Project project = callElement.getProject();
    ProgressManager.getInstance().run(new Task.Backgroundable(project, RefactoringBundle.message("searching.for.duplicates"), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (myProject == null || myProject.isDisposed()) return;
        final List<SimpleMatch> duplicates = ReadAction.compute(() -> finder.findDuplicates(scope, generatedMethod));

        ApplicationManager.getApplication().invokeLater(() -> replaceDuplicates(callElement, editor, replacer, duplicates));
      }
    });
  }


  /**
   * Finds duplicates of the code fragment specified in the finder in given scopes.
   * Note that in contrast to {@link #processDuplicates} the search is performed synchronously because normally you need the results in
   * order to complete the refactoring. If user cancels it, empty list will be returned.
   *
   * @param finder          finder object to seek for duplicates
   * @param searchScopes    scopes where to look them in
   * @param generatedMethod new method that should be excluded from the search
   * @return list of discovered duplicate code fragments or empty list if user interrupted the search
   * @see #replaceDuplicates(PsiElement, Editor, Consumer, List)
   */
  @NotNull
  public static List<SimpleMatch> collectDuplicates(@NotNull SimpleDuplicatesFinder finder,
                                                    @NotNull List<PsiElement> searchScopes,
                                                    @NotNull PsiElement generatedMethod) {
    final Project project = generatedMethod.getProject();
    try {
      //noinspection RedundantCast
      return ProgressManager.getInstance().runProcessWithProgressSynchronously(
        (ThrowableComputable<List<SimpleMatch>, RuntimeException>)() -> {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
          return ReadAction.compute(() -> finder.findDuplicates(searchScopes, generatedMethod));
        }, RefactoringBundle.message("searching.for.duplicates"), true, project);
    }
    catch (ProcessCanceledException e) {
      return Collections.emptyList();
    }
  }


  /**
   * Notifies user about found duplicates and then highlights each of them in the editor and asks user how to proceed.
   *
   * @param callElement generated expression or statement that contains invocation of the new method
   * @param editor      instance of editor where refactoring is performed
   * @param replacer    strategy of substituting each duplicate occurence with the replacement fragment
   * @param duplicates  discovered duplicates of extracted code fragment
   * @see #collectDuplicates(SimpleDuplicatesFinder, List, PsiElement)
   */
  public static void replaceDuplicates(@NotNull PsiElement callElement,
                                       @NotNull Editor editor,
                                       @NotNull Consumer<? super Pair<SimpleMatch, PsiElement>> replacer,
                                       @NotNull List<? extends SimpleMatch> duplicates) {
    if (!duplicates.isEmpty()) {
      final String message = RefactoringBundle
        .message("0.has.detected.1.code.fragments.in.this.file.that.can.be.replaced.with.a.call.to.extracted.method",
                 ApplicationNamesInfo.getInstance().getProductName(), duplicates.size());
      final boolean isUnittest = ApplicationManager.getApplication().isUnitTestMode();
      boolean isPerformanceScript = System.getProperty("testscript.filename") != null;
      final Project project = callElement.getProject();
      final int exitCode = !isUnittest && !isPerformanceScript ? Messages.showYesNoDialog(project, message,
                                                                  RefactoringBundle.message("refactoring.extract.method.dialog.title"),
                                                                  Messages.getInformationIcon()) :
                           Messages.YES;
      if (exitCode == Messages.YES) {
        boolean replaceAll = false;
        final Map<SimpleMatch, RangeHighlighter> highlighterMap = new HashMap<>();
        for (SimpleMatch match : duplicates) {
          if (!match.getStartElement().isValid() || !match.getEndElement().isValid()) continue;
          final Pair<SimpleMatch, PsiElement> replacement = Pair.create(match, callElement);
          if (!replaceAll) {
            highlightInEditor(project, match, editor, highlighterMap);

            int promptResult = FindManager.PromptResult.ALL;
            if (!isUnittest && !isPerformanceScript) {
              ReplacePromptDialog promptDialog =
                new ReplacePromptDialog(false, RefactoringBundle.message("replace.fragment"), project);
              promptDialog.show();
              promptResult = promptDialog.getExitCode();
            }
            if (promptResult == FindManager.PromptResult.SKIP) {
              final HighlightManager highlightManager = HighlightManager.getInstance(project);
              final RangeHighlighter highlighter = highlighterMap.get(match);
              if (highlighter != null) highlightManager.removeSegmentHighlighter(editor, highlighter);
              continue;
            }
            if (promptResult == FindManager.PromptResult.CANCEL) break;

            if (promptResult == FindManager.PromptResult.OK) {
              replaceDuplicate(project, replacer, replacement);
            }
            else if (promptResult == FindManager.PromptResult.ALL) {
              replaceDuplicate(project, replacer, replacement);
              replaceAll = true;
            }
          }
          else {
            replaceDuplicate(project, replacer, replacement);
          }
        }
      }
    }
  }

  private static void replaceDuplicate(final Project project, final Consumer<? super Pair<SimpleMatch, PsiElement>> replacer,
                                       final Pair<SimpleMatch, PsiElement> replacement) {
    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> replacer.consume(replacement)),
                                                  RefactoringBundle.message("extract.method.replace.duplicate.command.name"), null);
  }


  private static void highlightInEditor(@NotNull final Project project, @NotNull final SimpleMatch match,
                                 @NotNull final Editor editor, Map<SimpleMatch, RangeHighlighter> highlighterMap) {
    final List<RangeHighlighter> highlighters = new ArrayList<>();
    final HighlightManager highlightManager = HighlightManager.getInstance(project);
    final int startOffset = match.getStartElement().getTextRange().getStartOffset();
    final int endOffset = match.getEndElement().getTextRange().getEndOffset();
    highlightManager.addRangeHighlight(editor, startOffset, endOffset, EditorColors.SEARCH_RESULT_ATTRIBUTES, true, highlighters);
    highlighterMap.put(match, highlighters.get(0));
    final LogicalPosition logicalPosition = editor.offsetToLogicalPosition(startOffset);
    editor.getScrollingModel().scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE);
  }
}
