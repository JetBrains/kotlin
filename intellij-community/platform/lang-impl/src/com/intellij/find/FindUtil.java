// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actions.EditorActionUtil;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.LightweightHint;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class FindUtil {
  private static final Key<Direction> KEY = Key.create("FindUtil.KEY");

  private FindUtil() {
  }

  @Nullable
  private static VirtualFile getVirtualFile(@NotNull Editor myEditor) {
    Project project = myEditor.getProject();
    PsiFile file = project != null ? PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument()) : null;
    return file != null ? file.getVirtualFile() : null;
  }

  public static void initStringToFindWithSelection(FindModel findModel, @Nullable Editor editor) {
    if (editor != null) {
      String s = getSelectedText(editor);
      if (s != null && s.length() < 10000) {
        if (findModel.isRegularExpressions() && Registry.is("ide.find.escape.selected.text.for.regex")) {
          findModel.setStringToFind(StringUtil.escapeToRegexp(s));
        } else {
          FindModel.initStringToFind(findModel, s);
        }
      }
    }
  }

  public static void configureFindModel(boolean replace, @Nullable Editor editor, FindModel model, boolean firstSearch) {
    String selectedText = getSelectedText(editor);
    boolean multiline = selectedText != null && selectedText.contains("\n");
    String stringToFind = firstSearch || model.getStringToFind().contains("\n") ? "" : model.getStringToFind();
    boolean isSelectionUsed = false;
    if (!StringUtil.isEmpty(selectedText)) {
      if (!multiline || !replace) {
        stringToFind = selectedText;
        isSelectionUsed = true;
      } else {
        stringToFind = "";
      }
    }
    model.setReplaceState(replace);
    boolean isGlobal = !multiline || !replace;
    model.setStringToFind(isSelectionUsed
                          && model.isRegularExpressions()
                          && Registry.is("ide.find.escape.selected.text.for.regex")
                          ? StringUtil.escapeToRegexp(stringToFind)
                          : stringToFind);
    model.setMultiline(false);
    model.setGlobal(isGlobal);
    model.setPromptOnReplace(false);
  }

  public static void updateFindInFileModel(@Nullable Project project, @NotNull FindModel with, boolean saveFindString) {
    FindModel model = FindManager.getInstance(project).getFindInFileModel();
    model.setCaseSensitive(with.isCaseSensitive());
    model.setWholeWordsOnly(with.isWholeWordsOnly());
    model.setRegularExpressions(with.isRegularExpressions());
    model.setSearchContext(with.getSearchContext());

    if (saveFindString && !with.getStringToFind().isEmpty()) {
      model.setStringToFind(with.getStringToFind());
    }

    if (with.isReplaceState()) {
      model.setPreserveCase(with.isPreserveCase());
      if (saveFindString) model.setStringToReplace(with.getStringToReplace());
    }
  }

  public static void useFindStringFromFindInFileModel(FindModel findModel, Editor editor) {
    if (editor != null) {
      EditorSearchSession editorSearchSession = EditorSearchSession.get(editor);
      if (editorSearchSession != null) {
        FindModel currentFindModel = editorSearchSession.getFindModel();
        findModel.setStringToFind(currentFindModel.getStringToFind());
        if (findModel.isReplaceState()) findModel.setStringToReplace(currentFindModel.getStringToReplace());
      }
    }
  }

  private enum Direction {
    UP, DOWN
  }

  private static String getSelectedText(@Nullable Editor editor) {
    if (editor == null) return null;
    String selectedText = editor.getSelectionModel().getSelectedText();
    if (selectedText == null && Registry.is("ide.find.select.word.at.caret")) {
      selectedText  = getWordAtCaret(editor, true);
    }
    return selectedText;
  }

  @Nullable
  private static String getWordAtCaret(@Nullable Editor editor, boolean selectWordIfFound) {
    if (editor == null) return null;
    int caretOffset = editor.getCaretModel().getOffset();
    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    int start = 0;
    int end = document.getTextLength();
    if (!editor.getSelectionModel().hasSelection()) {
      for (int i = caretOffset - 1; i >= 0; i--) {
        char c = text.charAt(i);
        if (!Character.isJavaIdentifierPart(c)) {
          start = i + 1;
          break;
        }
      }
      for (int i = caretOffset; i < document.getTextLength(); i++) {
        char c = text.charAt(i);
        if (!Character.isJavaIdentifierPart(c)) {
          end = i;
          break;
        }
      }
      if (start < end && selectWordIfFound) {
        editor.getSelectionModel().setSelection(start, end);
      }
    }
    else {
      start = editor.getSelectionModel().getSelectionStart();
      end = editor.getSelectionModel().getSelectionEnd();
    }
    return start < end? text.subSequence(start, end).toString() : null;
  }

  public static void findWordAtCaret(Project project, @NotNull Editor editor) {
    String s = getWordAtCaret(editor, false);
    if (s == null) {
      return;
    }
    FindManager findManager = FindManager.getInstance(project);
    FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(project);
    findInProjectSettings.addStringToFind(s);
    findManager.getFindInFileModel().setStringToFind(s);
    findManager.setFindWasPerformed();
    findManager.clearFindingNextUsageInFile();
    FindModel model = new FindModel();
    model.setStringToFind(s);
    model.setCaseSensitive(true);
    model.setWholeWordsOnly(!editor.getSelectionModel().hasSelection());

    EditorSearchSession searchSession = EditorSearchSession.get(editor);
    if (searchSession != null) {
      searchSession.setTextInField(model.getStringToFind());
    }

    findManager.setFindNextModel(model);
    doSearch(project, editor, editor.getCaretModel().getOffset(), true, model, true);
  }

  public static void find(@NotNull final Project project, @NotNull final Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final FindManager findManager = FindManager.getInstance(project);
    String s = getSelectedText(editor);

    final FindModel model = findManager.getFindInFileModel().clone();
    if (StringUtil.isEmpty(s)) {
      model.setGlobal(true);
    }
    else {
      if (s.indexOf('\n') >= 0) {
        model.setGlobal(false);
      }
      else {
        model.setStringToFind(s);
        model.setGlobal(true);
      }
    }

    model.setReplaceState(false);
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    model.setFindAllEnabled(psiFile != null);

    findManager.showFindDialog(model, () -> {
      if (model.isFindAll() && psiFile != null) {
        findManager.setFindNextModel(model);
        findAllAndShow(project, psiFile, model);
        return;
      }

      if (!model.isGlobal() && editor.getSelectionModel().hasSelection()) {
        int offset = model.isForward()
                     ? editor.getSelectionModel().getSelectionStart()
                     : editor.getSelectionModel().getSelectionEnd();
        ScrollType scrollType = model.isForward() ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;
        moveCaretAndDontChangeSelection(editor, offset, scrollType);
      }

      int offset;
      if (model.isGlobal()) {
        if (model.isFromCursor()) {
          offset = editor.getCaretModel().getOffset();
        }
        else {
          offset = model.isForward() ? 0 : editor.getDocument().getTextLength();
        }
      }
      else {
        // in selection

        if (!editor.getSelectionModel().hasSelection()) {
          // TODO[anton] actually, this should never happen - Find dialog should not allow such combination
          findManager.setFindNextModel(null);
          return;
        }

        offset = model.isForward() ? editor.getSelectionModel().getSelectionStart() : editor.getSelectionModel().getSelectionEnd();
      }

      findManager.setFindNextModel(null);
      findManager.getFindInFileModel().copyFrom(model);
      doSearch(project, editor, offset, true, model, true);
    });
  }

  @Nullable
  static List<Usage> findAll(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull FindModel findModel) {
    if (project.isDisposed()) {
      return null;
    }
    psiFile = (PsiFile)psiFile.getNavigationElement();
    if (psiFile == null) {
      return null;
    }
    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    if (document == null) return null;

    CharSequence text = document.getCharsSequence();
    int textLength = document.getTextLength();
    FindManager findManager = FindManager.getInstance(project);
    findModel.setForward(true); // when find all there is no diff in direction

    int offset = 0;
    VirtualFile virtualFile = psiFile.getVirtualFile();

    final List<Usage> usages = new ArrayList<>();
    while (offset < textLength) {
      FindResult result = findManager.findString(text, offset, findModel, virtualFile);
      if (!result.isStringFound()) break;

      usages.add(new UsageInfo2UsageAdapter(new UsageInfo(psiFile, result.getStartOffset(), result.getEndOffset())));

      final int prevOffset = offset;
      offset = result.getEndOffset();

      if (prevOffset == offset) {
        // for regular expr the size of the match could be zero -> could be infinite loop in finding usages!
        ++offset;
      }
    }
    return usages;
  }

  static void findAllAndShow(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull FindModel findModel) {
    findModel.setCustomScope(true);
    findModel.setProjectScope(false);
    findModel.setCustomScopeName("File " + psiFile.getName());
    List<Usage> usages = findAll(project, psiFile, findModel);
    if (usages == null) return;
    final UsageTarget[] usageTargets = {new FindInProjectUtil.StringUsageTarget(project, findModel)};
    final UsageViewPresentation usageViewPresentation = FindInProjectUtil.setupViewPresentation(false, findModel);
    UsageView view =
      UsageViewManager.getInstance(project).showUsages(usageTargets, usages.toArray(Usage.EMPTY_ARRAY), usageViewPresentation);
    view.setRerunAction(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        findAllAndShow(project, psiFile, findModel);
      }

      @Override
      public boolean isEnabled() {
        return !project.isDisposed() && psiFile.isValid();
      }
    });
  }

  public static void searchBack(final Project project, final Editor editor, @Nullable DataContext context) {
    FindManager findManager = FindManager.getInstance(project);
    if (!findManager.findWasPerformed() && !findManager.selectNextOccurrenceWasPerformed()) {
      new IncrementalFindAction().getHandler().execute(editor, null, context);
      return;
    }

    FindModel model = findManager.getFindNextModel(editor);
    if (model == null) {
      model = findManager.getFindInFileModel();
    }
    model = model.clone();
    model.setForward(!model.isForward());
    if (!model.isGlobal() && !editor.getSelectionModel().hasSelection()) {
      model.setGlobal(true);
    }

    int offset;
    if (Direction.UP.equals(editor.getUserData(KEY)) && !model.isForward()) {
      offset = editor.getDocument().getTextLength();
    }
    else if (Direction.DOWN.equals(editor.getUserData(KEY)) && model.isForward()) {
      offset = 0;
    }
    else {
      editor.putUserData(KEY, null);
      offset = editor.getCaretModel().getOffset();
      if (!model.isForward() && offset > 0) {
        offset--;
      }
    }
    searchAgain(project, editor, offset, model);
  }

  public static boolean searchAgain(final Project project, final Editor editor, @Nullable DataContext context) {
    FindManager findManager = FindManager.getInstance(project);
    if (!findManager.findWasPerformed() && !findManager.selectNextOccurrenceWasPerformed()) {
      new IncrementalFindAction().getHandler().execute(editor, null, context);
      return false;
    }

    FindModel model = findManager.getFindNextModel(editor);
    if (model == null) {
      model = findManager.getFindInFileModel();
    }
    model = model.clone();

    int offset;
    if (Direction.DOWN.equals(editor.getUserData(KEY)) && model.isForward()) {
      offset = 0;
    }
    else if (Direction.UP.equals(editor.getUserData(KEY)) && !model.isForward()) {
      offset = editor.getDocument().getTextLength();
    }
    else {
      editor.putUserData(KEY, null);
      offset = model.isGlobal() && model.isForward() ? editor.getSelectionModel().getSelectionEnd() : editor.getCaretModel().getOffset();
      if (!model.isForward() && offset > 0) {
        offset--;
      }
    }
    return searchAgain(project, editor, offset, model);
  }

  private static boolean searchAgain(Project project, Editor editor, int offset, FindModel model) {
    if (!model.isGlobal() && !editor.getSelectionModel().hasSelection()) {
      model.setGlobal(true);
    }
    model.setFromCursor(false);
    if (model.isReplaceState()) {
      model.setPromptOnReplace(true);
      model.setReplaceAll(false);
      replace(project, editor, offset, model);
      return true;
    }
    else {
      doSearch(project, editor, offset, true, model, true);
      return false;
    }
  }

  public static void replace(@NotNull Project project, @NotNull Editor editor) {
    final FindManager findManager = FindManager.getInstance(project);
    final FindModel model = findManager.getFindInFileModel().clone();
    final String s = editor.getSelectionModel().getSelectedText();
    if (!StringUtil.isEmpty(s)) {
      if (s.indexOf('\n') >= 0) {
        model.setGlobal(false);
      }
      else {
        model.setStringToFind(s);
        model.setGlobal(true);
      }
    }
    else {
      model.setGlobal(true);
    }
    model.setReplaceState(true);

    findManager.showFindDialog(model, () -> {
      if (!model.isGlobal() && editor.getSelectionModel().hasSelection()) {
        int offset = model.isForward()
                     ? editor.getSelectionModel().getSelectionStart()
                     : editor.getSelectionModel().getSelectionEnd();
        ScrollType scrollType = model.isForward() ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;
        moveCaretAndDontChangeSelection(editor, offset, scrollType);
      }
      int offset;
      if (model.isGlobal()) {
        if (model.isFromCursor()) {
          offset = editor.getCaretModel().getOffset();
          if (!model.isForward()) {
            offset++;
          }
        }
        else {
          offset = model.isForward() ? 0 : editor.getDocument().getTextLength();
        }
      }
      else {
        // in selection

        if (!editor.getSelectionModel().hasSelection()) {
          // TODO[anton] actually, this should never happen - Find dialog should not allow such combination
          findManager.setFindNextModel(null);
          return;
        }

        offset = model.isForward() ? editor.getSelectionModel().getSelectionStart() : editor.getSelectionModel().getSelectionEnd();
      }

      if (s != null && editor.getSelectionModel().hasSelection() && s.equals(model.getStringToFind())) {
        if (model.isFromCursor() && model.isForward()) {
          offset = Math.min(editor.getSelectionModel().getSelectionStart(), offset);
        }
        else if (model.isFromCursor() && !model.isForward()) {
          offset = Math.max(editor.getSelectionModel().getSelectionEnd(), offset);
        }
      }
      findManager.setFindNextModel(null);
      findManager.getFindInFileModel().copyFrom(model);
      replace(project, editor, offset, model);
    });
  }

  public static boolean replace(@NotNull Project project, @NotNull Editor editor, int offset, @NotNull FindModel model) {
    return replace(project, editor, offset, model, (range, replace) -> true);
  }

  public static boolean replace(@NotNull Project project, @NotNull Editor editor, int offset, @NotNull FindModel model, ReplaceDelegate delegate) {
    Document document = editor.getDocument();

    if (!FileDocumentManager.getInstance().requestWriting(document, project)) {
      return false;
    }

    document.startGuardedBlockChecking();
    boolean toPrompt = model.isPromptOnReplace();

    try {
      doReplace(project, editor, model, offset, toPrompt, delegate);
    }
    catch (ReadOnlyFragmentModificationException e) {
      EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document).handle(e);
    }
    finally {
      document.stopGuardedBlockChecking();
    }

    return true;
  }

  private static void doReplace(@NotNull Project project,
                                @NotNull Editor editor,
                                @NotNull FindModel aModel,
                                int caretOffset,
                                boolean toPrompt,
                                ReplaceDelegate delegate) {
    FindManager findManager = FindManager.getInstance(project);
    final FindModel model = aModel.clone();
    int occurrences = 0;

    List<Pair<TextRange, String>> rangesToChange = new ArrayList<>();

    boolean replaced = false;
    boolean reallyReplaced = false;

    int offset = caretOffset;
    Document document = editor.getDocument();
    while (offset >= 0 && offset < document.getTextLength()) {
      caretOffset = offset;
      FindResult result = doSearch(project, editor, offset, !replaced, model, toPrompt);
      if (result == null) {
        break;
      }
      int startResultOffset = result.getStartOffset();
      model.setFromCursor(true);

      int startOffset = result.getStartOffset();
      int endOffset = result.getEndOffset();
      String foundString = document.getCharsSequence().subSequence(startOffset, endOffset).toString();
      String toReplace;
      try {
        toReplace = findManager.getStringToReplace(foundString, model, startOffset, document.getCharsSequence());
      }
      catch (FindManager.MalformedReplacementStringException e) {
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
          Messages.showErrorDialog(project, e.getMessage(), FindBundle.message("find.replace.invalid.replacement.string.title"));
        }
        break;
      }

      if (toPrompt) {
        int promptResult = findManager.showPromptDialog(model, FindBundle.message("find.replace.dialog.title"));
        if (promptResult == FindManager.PromptResult.SKIP) {
          offset = model.isForward() ? result.getEndOffset() : startResultOffset;
          continue;
        }
        if (promptResult == FindManager.PromptResult.CANCEL) {
          break;
        }
        if (promptResult == FindManager.PromptResult.ALL) {
          toPrompt = false;
        }
      }
      int newOffset;
      if (delegate == null || delegate.shouldReplace(result, toReplace)) {
        if (toPrompt) {
          //[SCR 7258]
          if (!reallyReplaced) {
            editor.getCaretModel().moveToOffset(0);
            reallyReplaced = true;
          }
        }
        TextRange textRange = doReplace(project, document, model, result, toReplace, toPrompt, rangesToChange);
        replaced = true;
        newOffset = model.isForward() ? textRange.getEndOffset() : textRange.getStartOffset();
        if (textRange.isEmpty()) ++newOffset;
        occurrences++;
      }
      else {
        newOffset = model.isForward() ? result.getEndOffset() : result.getStartOffset();
      }

      if (newOffset == offset) {
        newOffset += model.isForward() ? 1 : -1;
      }
      offset = newOffset;
    }

    if (replaced) {
      if (toPrompt) {
        if (caretOffset > document.getTextLength()) {
          caretOffset = document.getTextLength();
        }
        editor.getCaretModel().moveToOffset(caretOffset);
      }
      else {
        CharSequence text = document.getCharsSequence();
        final StringBuilder newText = new StringBuilder(document.getTextLength());
        rangesToChange.sort(Comparator.comparingInt(o -> o.getFirst().getStartOffset()));
        int offsetBefore = 0;
        for (Pair<TextRange, String> pair : rangesToChange) {
          TextRange range = pair.getFirst();
          String replace = pair.getSecond();
          newText.append(text, offsetBefore, range.getStartOffset()); //before change
          if (delegate == null || delegate.shouldReplace(range, replace)) {
            newText.append(replace);
          }
          else {
            newText.append(text.subSequence(range.getStartOffset(), range.getEndOffset()));
          }
          offsetBefore = range.getEndOffset();
          if (offsetBefore < caretOffset) {
            caretOffset += replace.length() - range.getLength();
          }
        }
        newText.append(text, offsetBefore, text.length()); //tail
        if (caretOffset > newText.length()) {
          caretOffset = newText.length();
        }
        final int finalCaretOffset = caretOffset;
        CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
          document.setText(newText);
          editor.getCaretModel().moveToOffset(finalCaretOffset);
          if (model.isGlobal()) {
            editor.getSelectionModel().removeSelection();
          }
        }), null, null);
      }
    }

    ReplaceInProjectManager.reportNumberReplacedOccurrences(project, occurrences);
  }


  private static boolean selectionMayContainRange(SelectionModel selection, TextRange range) {
    int[] starts = selection.getBlockSelectionStarts();
    int[] ends = selection.getBlockSelectionEnds();
    return starts.length != 0 && new TextRange(starts[0], ends[starts.length - 1]).contains(range);
  }

  private static boolean selectionStrictlyContainsRange(SelectionModel selection, TextRange range) {
    int[] starts = selection.getBlockSelectionStarts();
    int[] ends = selection.getBlockSelectionEnds();
    for (int i = 0; i < starts.length; ++i) {
      if (new TextRange(starts[i], ends[i]).contains(range)) {  //todo
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static FindResult doSearch(@NotNull Project project,
                                     @NotNull final Editor editor,
                                     int offset,
                                     boolean toWarn,
                                     @NotNull FindModel model,
                                     boolean adjustEditor) {
    FindManager findManager = FindManager.getInstance(project);
    Document document = editor.getDocument();

    final FindResult result = findManager.findString(document.getCharsSequence(), offset, model, getVirtualFile(editor));

    boolean isFound = result.isStringFound();
    final SelectionModel selection = editor.getSelectionModel();
    if (isFound && !model.isGlobal()) {
      if (!selectionMayContainRange(selection, result)) {
        isFound = false;
      }
      else if (!selectionStrictlyContainsRange(selection, result)) {
        final int[] starts = selection.getBlockSelectionStarts();
        for (int newOffset : starts) {
          if (newOffset > result.getStartOffset()) {
            return doSearch(project, editor, newOffset, toWarn, model, adjustEditor);
          }
        }
      }
    }
    if (!isFound) {
      if (toWarn) {
        processNotFound(editor, model.getStringToFind(), model, project);
      }
      return null;
    }

    if (adjustEditor) {
      final CaretModel caretModel = editor.getCaretModel();
      final ScrollingModel scrollingModel = editor.getScrollingModel();
      int oldCaretOffset = caretModel.getOffset();
      boolean forward = oldCaretOffset < result.getStartOffset();
      final ScrollType scrollType = forward ? ScrollType.CENTER_DOWN : ScrollType.CENTER_UP;

      if (model.isGlobal()) {
        int targetCaretPosition = result.getEndOffset();
        if (selection.getSelectionEnd() - selection.getSelectionStart() == result.getLength()) {
          // keeping caret's position relative to selection
          // use case: FindNext is used after SelectNextOccurrence action
          targetCaretPosition = caretModel.getOffset() - selection.getSelectionStart() + result.getStartOffset();
        }
        if (caretModel.getCaretAt(editor.offsetToVisualPosition(targetCaretPosition)) != null) {
          // if there's a different caret at target position, don't move current caret/selection
          // use case: FindNext is used after SelectNextOccurrence action
          return result;
        }
        caretModel.moveToOffset(targetCaretPosition);
        selection.removeSelection();
        scrollingModel.scrollToCaret(scrollType);
        scrollingModel.runActionOnScrollingFinished(
          () -> {
            scrollingModel.scrollTo(editor.offsetToLogicalPosition(result.getStartOffset()), scrollType);
            scrollingModel.scrollTo(editor.offsetToLogicalPosition(result.getEndOffset()), scrollType);
          }
        );
      }
      else {
        moveCaretAndDontChangeSelection(editor, result.getStartOffset(), scrollType);
        moveCaretAndDontChangeSelection(editor, result.getEndOffset(), scrollType);
      }
      IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();

      if (!model.isGlobal()) {
        final RangeHighlighterEx segmentHighlighter = (RangeHighlighterEx)editor.getMarkupModel().addRangeHighlighter(
          EditorColors.SEARCH_RESULT_ATTRIBUTES,
          result.getStartOffset(),
          result.getEndOffset(),
          HighlighterLayer.SELECTION + 1,
          HighlighterTargetArea.EXACT_RANGE);
        MyListener listener = new MyListener(editor, segmentHighlighter);
        caretModel.addCaretListener(listener);
      }
      else {
        selection.setSelection(result.getStartOffset(), result.getEndOffset());
      }
    }

    return result;
  }

  private static class MyListener implements CaretListener {
    private final Editor myEditor;
    private final RangeHighlighter mySegmentHighlighter;

    private MyListener(@NotNull Editor editor, @NotNull RangeHighlighter segmentHighlighter) {
      myEditor = editor;
      mySegmentHighlighter = segmentHighlighter;
    }

    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      removeAll();
    }

    private void removeAll() {
      myEditor.getCaretModel().removeCaretListener(this);
      mySegmentHighlighter.dispose();
    }
  }

  public static void processNotFound(final Editor editor, String stringToFind, FindModel model, Project project) {
    String message = FindBundle.message("find.search.string.not.found.message", stringToFind);

    short position = HintManager.UNDER;
    if (model.isGlobal()) {
      final FindModel newModel = model.clone();
      FindManager findManager = FindManager.getInstance(project);
      Document document = editor.getDocument();
      FindResult result = findManager.findString(document.getCharsSequence(),
                                                 newModel.isForward() ? 0 : document.getTextLength(), model, getVirtualFile(editor));
      if (!result.isStringFound()) {
        result = null;
      }

      FindModel modelForNextSearch = findManager.getFindNextModel(editor);
      if (modelForNextSearch == null) {
        modelForNextSearch = findManager.getFindInFileModel();
      }

      if (result != null) {
        if (newModel.isForward()) {
          AnAction action = ActionManager.getInstance().getAction(
            modelForNextSearch.isForward() ? IdeActions.ACTION_FIND_NEXT : IdeActions.ACTION_FIND_PREVIOUS);
          String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
          if (!shortcutsText.isEmpty()) {
            message = FindBundle.message("find.search.again.from.top.hotkey.message", message, shortcutsText);
          }
          else {
            message = FindBundle.message("find.search.again.from.top.action.message", message);
          }
          editor.putUserData(KEY, Direction.DOWN);
        }
        else {
          AnAction action = ActionManager.getInstance().getAction(
            modelForNextSearch.isForward() ? IdeActions.ACTION_FIND_PREVIOUS : IdeActions.ACTION_FIND_NEXT);
          String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
          if (!shortcutsText.isEmpty()) {
            message = FindBundle.message("find.search.again.from.bottom.hotkey.message", message, shortcutsText);
          }
          else {
            message = FindBundle.message("find.search.again.from.bottom.action.message", message);
          }
          editor.putUserData(KEY, Direction.UP);
          position = HintManager.ABOVE;
        }
      }
      CaretListener listener = new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent e) {
          editor.putUserData(KEY, null);
          editor.getCaretModel().removeCaretListener(this);
        }
      };
      editor.getCaretModel().addCaretListener(listener);
    }
    JComponent component = HintUtil.createInformationLabel(JDOMUtil.escapeText(message, false, false));
    final LightweightHint hint = new LightweightHint(component);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, position,
                                                     HintManager.HIDE_BY_ANY_KEY |
                                                     HintManager.HIDE_BY_TEXT_CHANGE |
                                                     HintManager.HIDE_BY_SCROLLING,
                                                     0, false);
  }

  public static TextRange doReplace(final Project project,
                                    final Document document,
                                    @NotNull FindModel model,
                                    FindResult result,
                                    @NotNull String stringToReplace,
                                    boolean reallyReplace,
                                    List<? super Pair<TextRange, String>> rangesToChange) {
    final int startOffset = result.getStartOffset();
    final int endOffset = result.getEndOffset();

    int newOffset;
    if (reallyReplace) {
      newOffset = doReplace(project, document, startOffset, endOffset, stringToReplace);
    }
    else {
      final String converted = StringUtil.convertLineSeparators(stringToReplace);
      TextRange textRange = new TextRange(startOffset, endOffset);
      rangesToChange.add(Pair.create(textRange, converted));

      newOffset = endOffset;
    }

    int start = startOffset;
    int end = newOffset;
    if (model.isRegularExpressions()) {
      String toFind = model.getStringToFind();
      if (model.isForward()) {
        if (StringUtil.endsWithChar(toFind, '$')) {
          int i = 0;
          int length = toFind.length();
          while (i + 2 <= length && toFind.charAt(length - i - 2) == '\\') i++;
          if (i % 2 == 0) end++; //This $ is a special symbol in regexp syntax
        }
        else if (StringUtil.startsWithChar(toFind, '^')) {
          while (end < document.getTextLength() && document.getCharsSequence().charAt(end) != '\n') end++;
        }
      }
      else {
        if (StringUtil.startsWithChar(toFind, '^')) {
          start--;
        }
        else if (StringUtil.endsWithChar(toFind, '$')) {
          while (start >= 0 && document.getCharsSequence().charAt(start) != '\n') start--;
        }
      }
    }
    return new TextRange(start, end);
  }

  private static int doReplace(Project project,
                               final Document document,
                               final int startOffset,
                               final int endOffset,
                               final String stringToReplace) {
    final String converted = StringUtil.convertLineSeparators(stringToReplace);
    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      //[ven] I doubt converting is a good solution to SCR 21224
      document.replaceString(startOffset, endOffset, converted);
    }), null, null);
    return startOffset + converted.length();
  }

  private static void moveCaretAndDontChangeSelection(final Editor editor, int offset, ScrollType scrollType) {
    LogicalPosition pos = editor.offsetToLogicalPosition(offset);
    editor.getCaretModel().moveToLogicalPosition(pos);
    editor.getScrollingModel().scrollToCaret(scrollType);
  }

  @FunctionalInterface
  public interface ReplaceDelegate {
    boolean shouldReplace(TextRange range, String replace);
  }

  public static <T> UsageView showInUsageView(@Nullable PsiElement sourceElement,
                                              T @NotNull [] targets,
                                              @NotNull Function<? super T, ? extends Usage> usageConverter,
                                              @NotNull String title,
                                              @Nullable Consumer<? super UsageViewPresentation> presentationSetup,
                                              @NotNull Project project) {
    if (targets.length == 0) return null;
    final UsageViewPresentation presentation = new UsageViewPresentation();
    presentation.setCodeUsagesString(title);
    presentation.setTabName(title);
    presentation.setTabText(title);
    if (presentationSetup != null) {
      presentationSetup.consume(presentation);
    }
    UsageTarget[] usageTargets = sourceElement == null ? UsageTarget.EMPTY_ARRAY : new UsageTarget[]{new PsiElement2UsageTargetAdapter(sourceElement)};

    UsageView view = UsageViewManager.getInstance(project).showUsages(usageTargets, Usage.EMPTY_ARRAY, presentation);

    ProgressManager.getInstance().run(new Task.Backgroundable(project, FindBundle.message("progress.title.updating.usage.view")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        UsageViewImpl impl = (UsageViewImpl)view;
        for (T pointer : targets) {
          if (impl.isDisposed()) break;
          ApplicationManager.getApplication().runReadAction(() -> {
            Usage usage = usageConverter.fun(pointer);
            if (usage != null) {
              view.appendUsage(usage);
            }
          });
        }
        UIUtil.invokeLaterIfNeeded(() -> {
          if (!impl.isDisposed()) impl.expandRoot();
        });
      }
    });
    return view;
  }

  @Nullable
  public static UsageView showInUsageView(@Nullable PsiElement sourceElement,
                                          PsiElement @NotNull [] targets,
                                          @NotNull String title,
                                          @NotNull Project project) {
    if (targets.length == 0) return null;
    PsiElement[] primary = sourceElement == null ? PsiElement.EMPTY_ARRAY : new PsiElement[]{sourceElement};

    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(project);
    SmartPsiElementPointer<?>[] pointers = Stream.of(targets).map(smartPointerManager::createSmartPsiElementPointer).toArray(SmartPsiElementPointer[]::new);
    // usage view will load document/AST so still referencing all these PSI elements might lead to out of memory
    //noinspection UnusedAssignment
    targets = PsiElement.EMPTY_ARRAY;
    return showInUsageView(sourceElement, pointers, p -> {
      PsiElement element = p.getElement();
      return element == null ? null : UsageInfoToUsageConverter.convert(primary, new UsageInfo(element));
    }, title, null, project);
  }

  /**
   * Creates a selection in editor per each search result. Existing carets and selections in editor are discarded.
   *
   * @param caretShiftFromSelectionStart if non-negative, defines caret position relative to selection start, for each created selection.
   *                                     if negative, carets will be positioned at selection ends
   */
  public static void selectSearchResultsInEditor(@NotNull Editor editor,
                                                 @NotNull Iterator<? extends FindResult> resultIterator,
                                                 int caretShiftFromSelectionStart) {
    if (!editor.getCaretModel().supportsMultipleCarets()) {
      return;
    }
    ArrayList<CaretState> caretStates = new ArrayList<>();
    while (resultIterator.hasNext()) {
      FindResult findResult = resultIterator.next();
      int caretOffset = getCaretPosition(findResult, caretShiftFromSelectionStart);
      int selectionStartOffset = findResult.getStartOffset();
      int selectionEndOffset = findResult.getEndOffset();
      EditorActionUtil.makePositionVisible(editor, caretOffset);
      EditorActionUtil.makePositionVisible(editor, selectionStartOffset);
      EditorActionUtil.makePositionVisible(editor, selectionEndOffset);
      caretStates.add(new CaretState(editor.offsetToLogicalPosition(caretOffset),
                                     editor.offsetToLogicalPosition(selectionStartOffset),
                                     editor.offsetToLogicalPosition(selectionEndOffset)));
    }
    if (caretStates.isEmpty()) {
      return;
    }
    editor.getCaretModel().setCaretsAndSelections(caretStates);
  }

  /**
   * Attempts to add a new caret to editor, with selection corresponding to given search result.
   *
   * @param caretShiftFromSelectionStart if non-negative, defines caret position relative to selection start, for each created selection.
   *                                     if negative, caret will be positioned at selection end
   * @return {@code true} if caret was added successfully, {@code false} if it cannot be done, e.g. because a caret already
   * exists at target position
   */
  public static boolean selectSearchResultInEditor(@NotNull Editor editor, @NotNull FindResult result, int caretShiftFromSelectionStart) {
    if (!editor.getCaretModel().supportsMultipleCarets()) {
      return false;
    }
    int caretOffset = getCaretPosition(result, caretShiftFromSelectionStart);
    LogicalPosition caretPosition = editor.offsetToLogicalPosition(caretOffset);
    if (caretShiftFromSelectionStart == 0) caretPosition = caretPosition.leanForward(true);
    EditorActionUtil.makePositionVisible(editor, caretOffset);
    Caret newCaret = editor.getCaretModel().addCaret(caretPosition, true);
    if (newCaret == null) {
      return false;
    }
    else {
      int selectionStartOffset = result.getStartOffset();
      int selectionEndOffset = result.getEndOffset();
      EditorActionUtil.makePositionVisible(editor, selectionStartOffset);
      EditorActionUtil.makePositionVisible(editor, selectionEndOffset);
      newCaret.setSelection(selectionStartOffset, selectionEndOffset);
      return true;
    }
  }

  private static int getCaretPosition(FindResult findResult, int caretShiftFromSelectionStart) {
    return caretShiftFromSelectionStart < 0
           ? findResult.getEndOffset() : Math.min(findResult.getStartOffset() + caretShiftFromSelectionStart, findResult.getEndOffset());
  }

  public static void triggerUsedOptionsStats(@NotNull String type, @NotNull FindModel model) {
    FeatureUsageData data = new FeatureUsageData().
      addData("type", type).
      addData("case_sensitive", model.isCaseSensitive()).
      addData("whole_words_only", model.isWholeWordsOnly()).
      addData("regular_expressions", model.isRegularExpressions()).
      addData("with_file_filter", model.getFileFilter() != null).
      addData("context", model.getSearchContext().name());
    FUCounterUsageLogger.getInstance().logEvent("find", "search.session.started", data);
  }

  public static void triggerRegexHelpClicked(@Nullable String type) {
    FeatureUsageData data = new FeatureUsageData().addData("type", StringUtil.notNullize(type, "Unknown"));
    FUCounterUsageLogger.getInstance().logEvent("find", "regexp.help.clicked", data);
  }
}
