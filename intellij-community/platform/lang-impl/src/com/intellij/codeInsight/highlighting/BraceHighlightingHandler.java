// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil.BraceHighlightingAndNavigationContext;
import com.intellij.codeInsight.hint.EditorFragmentComponent;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.HighlighterIteratorWrapper;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.LightweightHint;
import com.intellij.util.Alarm;
import com.intellij.util.IntIntFunction;
import com.intellij.util.Processor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BraceHighlightingHandler {
  private static final Key<List<RangeHighlighter>> BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY = Key.create("BraceHighlighter.BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY");
  private static final Key<RangeHighlighter> LINE_MARKER_IN_EDITOR_KEY = Key.create("BraceHighlighter.LINE_MARKER_IN_EDITOR_KEY");
  private static final Key<LightweightHint> HINT_IN_EDITOR_KEY = Key.create("BraceHighlighter.HINT_IN_EDITOR_KEY");
  static final int LAYER = HighlighterLayer.LAST + 1;

  @NotNull private final Project myProject;
  @NotNull private final EditorEx myEditor;
  private final Alarm myAlarm;

  private final DocumentEx myDocument;
  private final PsiFile myPsiFile;
  private final CodeInsightSettings myCodeInsightSettings;

  BraceHighlightingHandler(@NotNull Project project, @NotNull EditorEx editor, @NotNull Alarm alarm, PsiFile psiFile) {
    myProject = project;

    myEditor = editor;
    myAlarm = alarm;
    myDocument = myEditor.getDocument();

    myPsiFile = psiFile;
    myCodeInsightSettings = CodeInsightSettings.getInstance();
  }

  static void lookForInjectedAndMatchBracesInOtherThread(@NotNull final Editor editor,
                                                         @NotNull final Alarm alarm,
                                                         @NotNull final Processor<? super BraceHighlightingHandler> processor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!isValidEditor(editor)) return;

    Project project = Objects.requireNonNull(editor.getProject());
    int offset = editor.getCaretModel().getOffset();

    ReadAction
      .nonBlocking(() -> {
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        return psiFile == null || psiFile instanceof PsiBinaryFile ? null : getInjectedFileIfAny(offset, psiFile);
      })
      .withDocumentsCommitted(project)
      .expireWhen(() -> !isValidEditor(editor))
      .coalesceBy(BraceHighlightingHandler.class, editor)
      .finishOnUiThread(ModalityState.stateForComponent(editor.getComponent()), foundFile -> {
        if (foundFile == null) return;

        if (foundFile.isValid() && offset == editor.getCaretModel().getOffset()) {
          EditorEx newEditor = (EditorEx)InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, foundFile);
          BraceHighlightingHandler handler = new BraceHighlightingHandler(project, newEditor, alarm, foundFile);
          processor.process(handler);
        } else {
          lookForInjectedAndMatchBracesInOtherThread(editor, alarm, processor);
        }
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private static boolean isValidEditor(@NotNull Editor editor) {
    Project editorProject = editor.getProject();
    return editorProject != null && !editorProject.isDisposed() && !editor.isDisposed() && editor.getComponent().isShowing();
  }

  @NotNull
  private static PsiFile getInjectedFileIfAny(int offset, @NotNull PsiFile psiFile) {
    PsiElement injectedElement = InjectedLanguageManager.getInstance(psiFile.getProject()).findInjectedElementAt(psiFile, offset);
    if (injectedElement != null /*&& !(injectedElement instanceof PsiWhiteSpace)*/) {
      PsiFile injected = injectedElement.getContainingFile();
      if (injected != null) {
        return injected;
      }
    }
    return psiFile;
  }

  @NotNull
  public static EditorHighlighter getLazyParsableHighlighterIfAny(Project project, Editor editor, PsiFile psiFile) {
    if (!PsiDocumentManager.getInstance(project).isCommitted(editor.getDocument())) {
      return ((EditorEx)editor).getHighlighter();
    }
    PsiElement elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
    for (PsiElement e : SyntaxTraverser.psiApi().parents(elementAt).takeWhile(Conditions.notEqualTo(psiFile))) {
      if (!(PsiUtilCore.getElementType(e) instanceof ILazyParseableElementType)) continue;
      Language language = ILazyParseableElementType.LANGUAGE_KEY.get(e.getNode());
      if (language == null) continue;
      TextRange range = e.getTextRange();
      final int offset = range.getStartOffset();
      SyntaxHighlighter syntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, psiFile.getVirtualFile());
      LexerEditorHighlighter highlighter = new LexerEditorHighlighter(syntaxHighlighter, editor.getColorsScheme()) {
        @NotNull
        @Override
        public HighlighterIterator createIterator(int startOffset) {
          return new HighlighterIteratorWrapper(super.createIterator(Math.max(startOffset - offset, 0))) {
            @Override
            public int getStart() {
              return super.getStart() + offset;
            }

            @Override
            public int getEnd() {
              return super.getEnd() + offset;
            }
          };
        }
      };
      highlighter.setText(editor.getDocument().getText(range));
      return highlighter;
    }
    return ((EditorEx)editor).getHighlighter();
  }

  void updateBraces() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    clearBraceHighlighters();

    if (myPsiFile == null || !myPsiFile.isValid()) return;

    if (!myCodeInsightSettings.HIGHLIGHT_BRACES) return;

    if (myEditor.getSelectionModel().hasSelection()) return;
    
    if (myEditor.getSoftWrapModel().isInsideOrBeforeSoftWrap(myEditor.getCaretModel().getVisualPosition())) return;

    int offset = myEditor.getCaretModel().getOffset();
    final CharSequence chars = myEditor.getDocument().getCharsSequence();

    myAlarm.cancelAllRequests();

    BraceHighlightingAndNavigationContext context = BraceMatchingUtil.computeHighlightingAndNavigationContext(myEditor, myPsiFile);
    if (context != null) {
      doHighlight(context.currentBraceOffset);
      offset = context.currentBraceOffset;
    }
    else if (offset > 0 && offset < chars.length()) {
      // There is a possible case that there are paired braces nearby the caret position and the document contains only white
      // space symbols between them. We want to highlight such braces as well.
      // Example: 
      //     public void test() { <caret>
      //     }
      char c = chars.charAt(offset);
      boolean searchForward = c != '\n';

      // Try to find matched brace backwards.
      int backwardNonSpaceEndOffset = CharArrayUtil.shiftBackward(chars, offset - 1, "\t ") + 1;
      if (backwardNonSpaceEndOffset > 0 && backwardNonSpaceEndOffset < offset) {
        context = BraceMatchingUtil.computeHighlightingAndNavigationContext(myEditor, myPsiFile, backwardNonSpaceEndOffset);
        if (context != null) {
          doHighlight(context.currentBraceOffset);
          offset = context.currentBraceOffset;
          searchForward = false;
        }
      }

      // Try to find matched brace forward.
      if (searchForward) {
        int nextNonSpaceCharOffset = CharArrayUtil.shiftForward(chars, offset, "\t ");
        if (nextNonSpaceCharOffset > offset) {
          context = BraceMatchingUtil.computeHighlightingAndNavigationContext(myEditor, myPsiFile, nextNonSpaceCharOffset);
          if (context != null) {
            doHighlight(context.currentBraceOffset);
            offset = context.currentBraceOffset;
          }
        }
      }
    }

    if (myCodeInsightSettings.HIGHLIGHT_SCOPE) {
      highlightScope(offset);
    }
  }

  @NotNull
  private FileType getFileTypeByOffset(int offset) {
    return PsiUtilBase.getPsiFileAtOffset(myPsiFile, offset).getFileType();
  }

  @NotNull
  private EditorHighlighter getEditorHighlighter() {
    return getLazyParsableHighlighterIfAny(myProject, myEditor, myPsiFile);
  }

  private void highlightScope(int offset) {
    if (!myPsiFile.isValid()) return;
    if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) return;
    if (myEditor.getDocument().getTextLength() <= offset) return;
    HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
    final CharSequence chars = myDocument.getCharsSequence();

    FileType fileType = getFileTypeByOffset(offset);

    if (!(BraceMatchingUtil.isStructuralBraceToken(fileType, iterator, chars) &&
          (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType) ||
           BraceMatchingUtil.isLBraceToken(iterator, chars, fileType))) &&
        BraceMatchingUtil.findStructuralLeftBrace(fileType, iterator, chars)) {
      highlightLeftBrace(iterator, true, fileType);
    }
  }

  private void doHighlight(int offset) {
    if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) return;

    HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
    final CharSequence chars = myDocument.getCharsSequence();

    FileType fileType = getFileTypeByOffset(offset);

    if (BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
      highlightLeftBrace(iterator, false, fileType);

      if (offset > 0) {
        iterator = getEditorHighlighter().createIterator(offset - 1);
        if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
          highlightRightBrace(iterator, fileType);
        }
      }
    }
    else if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
      highlightRightBrace(iterator, fileType);
    }
  }

  private void highlightRightBrace(@NotNull HighlighterIterator iterator, @NotNull FileType fileType) {
    TextRange brace1 = TextRange.create(iterator.getStart(), iterator.getEnd());

    boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, false);

    TextRange brace2 = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

    highlightBraces(brace2, brace1, matched, false, fileType);
  }

  private void highlightLeftBrace(@NotNull HighlighterIterator iterator, boolean scopeHighlighting, @NotNull FileType fileType) {
    TextRange brace1Start = TextRange.create(iterator.getStart(), iterator.getEnd());
    boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, true);

    TextRange brace2End = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

    highlightBraces(brace1Start, brace2End, matched, scopeHighlighting, fileType);
  }

  private void highlightBraces(@Nullable TextRange lBrace, @Nullable TextRange rBrace, boolean matched, boolean scopeHighlighting, @NotNull FileType fileType) {
    if (!matched && fileType == FileTypes.PLAIN_TEXT) {
      return;
    }

    if (rBrace != null && !scopeHighlighting) {
      highlightBrace(rBrace, matched);
    }

    if (lBrace != null && !scopeHighlighting) {
      highlightBrace(lBrace, matched);
    }

    FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject); // null in default project
    if (fileEditorManager == null || !myEditor.equals(fileEditorManager.getSelectedTextEditor())) {
      return;
    }

    if (lBrace != null && rBrace !=null) {
      final int startLine = myEditor.offsetToLogicalPosition(lBrace.getStartOffset()).line;
      final int endLine = myEditor.offsetToLogicalPosition(rBrace.getEndOffset()).line;
      if (endLine - startLine > 0) {
        lineMarkFragment(myEditor, myDocument, startLine, endLine, matched);
      }

      if (!scopeHighlighting) {
        showScopeHint(lBrace.getStartOffset(), lBrace.getEndOffset());
      }
    }
  }

  private void highlightBrace(@NotNull TextRange braceRange, boolean matched) {
    EditorColorsScheme scheme = myEditor.getColorsScheme();
    final TextAttributes attributes =
        matched ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
        : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);


    RangeHighlighter rbraceHighlighter =
        myEditor.getMarkupModel().addRangeHighlighter(
          braceRange.getStartOffset(), braceRange.getEndOffset(), LAYER, attributes, HighlighterTargetArea.EXACT_RANGE);
    rbraceHighlighter.setGreedyToLeft(false);
    rbraceHighlighter.setGreedyToRight(false);
    registerHighlighter(rbraceHighlighter);
  }

  private void registerHighlighter(@NotNull RangeHighlighter highlighter) {
    getHighlightersList().add(highlighter);
  }

  @NotNull
  private List<RangeHighlighter> getHighlightersList() {
    // braces are highlighted across the whole editor, not in each injected editor separately
    Editor editor = myEditor instanceof EditorWindow ? ((EditorWindow)myEditor).getDelegate() : myEditor;
    List<RangeHighlighter> highlighters = editor.getUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY);
    if (highlighters == null) {
      highlighters = new ArrayList<>();
      editor.putUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY, highlighters);
    }
    return highlighters;
  }

  private void showScopeHint(final int lbraceStart, final int lbraceEnd) {
    showScopeHint(myEditor, myAlarm, lbraceStart, lbraceEnd, it -> {
      if (!(myPsiFile instanceof PsiPlainTextFile) && myPsiFile.isValid()) {
        return BraceMatchingUtil.getBraceMatcher(getFileTypeByOffset(it), PsiUtilCore
          .getLanguageAtOffset(myPsiFile, it)).getCodeConstructStart(myPsiFile, it);
      }
      return it;
    });
  }

  /**
   * Schedules with {@code alarm} displaying of the scope start in the {@code editor}
   *
   * @param startComputation optional adjuster for the brace start offset
   */
  public static void showScopeHint(@NotNull Editor editor,
                                   @NotNull Alarm alarm,
                                   int leftBraceStart,
                                   int leftBraceEnd,
                                   @Nullable IntIntFunction startComputation) {
    Project project = editor.getProject();
    if (project == null) {
      return;
    }
    LogicalPosition bracePosition = editor.offsetToLogicalPosition(leftBraceStart);
    Point braceLocation = editor.logicalPositionToXY(bracePosition);
    final int y = braceLocation.y;
    alarm.addRequest(() -> ReadAction.run(() -> {
      if (project.isDisposed()) {
        return;
      }
      PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
        if (editor.isDisposed() || !editor.getComponent().isShowing()) {
          return;
        }
        Rectangle viewRect = editor.getScrollingModel().getVisibleArea();
        if (y >= viewRect.y) {
          return;
        }
        TextRange range = new TextRange(startComputation == null ? leftBraceStart : startComputation.fun(leftBraceStart), leftBraceEnd);
        Document document = editor.getDocument();
        int line1 = document.getLineNumber(range.getStartOffset());
        int line2 = document.getLineNumber(range.getEndOffset());
        line1 = Math.max(line1, line2 - EditorFragmentComponent.getAvailableVisualLinesAboveEditor(editor) + 1);
        range = new TextRange(document.getLineStartOffset(line1), range.getEndOffset());
        LightweightHint hint = EditorFragmentComponent.showEditorFragmentHint(editor, range, true, true);
        editor.putUserData(HINT_IN_EDITOR_KEY, hint);
      });
    }), 300, ModalityState.stateForComponent(editor.getComponent()));
  }

  void clearBraceHighlighters() {
    List<RangeHighlighter> highlighters = getHighlightersList();
    for (final RangeHighlighter highlighter : highlighters) {
      highlighter.dispose();
    }
    highlighters.clear();

    LightweightHint hint = myEditor.getUserData(HINT_IN_EDITOR_KEY);
    if (hint != null) {
      hint.hide();
      myEditor.putUserData(HINT_IN_EDITOR_KEY, null);
    }
    removeLineMarkers(myEditor);
  }

  /**
   * Draws a vertical line on the gutter of {@code editor}, covering lines of the {@code document} from the {@code startLine} to the
   * {@code endLine}
   */
  public static void lineMarkFragment(@NotNull EditorEx editor, @NotNull Document document, int startLine, int endLine, boolean matched) {
    removeLineMarkers(editor);

    if (startLine >= endLine || endLine >= document.getLineCount()) return;

    int startOffset = document.getLineStartOffset(startLine);
    int endOffset = document.getLineEndOffset(endLine);

    LineMarkerRenderer renderer = createLineMarkerRenderer(matched);
    if (renderer == null) return;

    RangeHighlighter highlighter = editor.getMarkupModel()
      .addRangeHighlighterAndChangeAttributes(startOffset, endOffset, 0, null, HighlighterTargetArea.LINES_IN_RANGE, false,
                                              h -> h.setLineMarkerRenderer(renderer));
    editor.putUserData(LINE_MARKER_IN_EDITOR_KEY, highlighter);
  }

  private static void removeLineMarkers(@NotNull EditorEx editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    RangeHighlighter marker = editor.getUserData(LINE_MARKER_IN_EDITOR_KEY);
    if (marker != null && editor.getMarkupModel().containsHighlighter(marker)) {
      marker.dispose();
    }
    editor.putUserData(LINE_MARKER_IN_EDITOR_KEY, null);
  }

  @Nullable
  public static LineMarkerRenderer createLineMarkerRenderer(boolean matched) {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    final TextAttributes attributes =
      matched ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
        : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);

    Color color = attributes.getBackgroundColor();
    if (color == null) return null;

    color = ColorUtil.isDark(scheme.getDefaultBackground()) ? ColorUtil.shift(color, 1.5d) : color.darker();
    return new MyLineMarkerRenderer(color);
  }

  private static class MyLineMarkerRenderer implements LineMarkerRenderer {
    private static final int DEEPNESS = 0;
    private static final int THICKNESS = 1;
    private final Color myColor;

    private MyLineMarkerRenderer(@NotNull Color color) {
      myColor = color;
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
      g.setColor(myColor);
      g.fillRect(r.x, r.y, THICKNESS, r.height);
      g.fillRect(r.x + THICKNESS, r.y, DEEPNESS, THICKNESS);
      g.fillRect(r.x + THICKNESS, r.y + r.height - THICKNESS, DEEPNESS, THICKNESS);
    }
  }
}
