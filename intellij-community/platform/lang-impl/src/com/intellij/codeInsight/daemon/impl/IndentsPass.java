// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.highlighting.BraceMatcher;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.util.DocumentUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.containers.IntStack;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;

public class IndentsPass extends TextEditorHighlightingPass implements DumbAware {
  private static final Key<List<RangeHighlighter>> INDENT_HIGHLIGHTERS_IN_EDITOR_KEY = Key.create("INDENT_HIGHLIGHTERS_IN_EDITOR_KEY");
  private static final Key<Long> LAST_TIME_INDENTS_BUILT = Key.create("LAST_TIME_INDENTS_BUILT");

  private final EditorEx myEditor;
  private final PsiFile  myFile;

  private volatile List<TextRange> myRanges = Collections.emptyList();
  private volatile List<IndentGuideDescriptor> myDescriptors = Collections.emptyList();

  private static final CustomHighlighterRenderer RENDERER = (editor, highlighter, g) -> {
    int startOffset = highlighter.getStartOffset();
    final Document doc = highlighter.getDocument();
    if (startOffset >= doc.getTextLength()) return;

    final int endOffset = highlighter.getEndOffset();
    final int endLine = doc.getLineNumber(endOffset);

    int off;
    int startLine = doc.getLineNumber(startOffset);
    IndentGuideDescriptor descriptor = editor.getIndentsModel().getDescriptor(startLine, endLine);

    final CharSequence chars = doc.getCharsSequence();
    do {
      int start = doc.getLineStartOffset(startLine);
      int end = doc.getLineEndOffset(startLine);
      off = CharArrayUtil.shiftForward(chars, start, end, " \t");
      startLine--;
    }
    while (startLine > 1 && off < doc.getTextLength() && chars.charAt(off) == '\n');

    final VisualPosition startPosition = editor.offsetToVisualPosition(off);
    int indentColumn = startPosition.column;

    // It's considered that indent guide can cross not only white space but comments, javadoc etc. Hence, there is a possible
    // case that the first indent guide line is, say, single-line comment where comment symbols ('//') are located at the first
    // visual column. We need to calculate correct indent guide column then.
    int lineShift = 1;
    if (indentColumn <= 0 && descriptor != null) {
      indentColumn = descriptor.indentLevel;
      lineShift = 0;
    }
    if (indentColumn <= 0) return;

    final FoldingModel foldingModel = editor.getFoldingModel();
    if (foldingModel.isOffsetCollapsed(off)) return;

    final FoldRegion headerRegion = foldingModel.getCollapsedRegionAtOffset(doc.getLineEndOffset(doc.getLineNumber(off)));
    final FoldRegion tailRegion = foldingModel.getCollapsedRegionAtOffset(doc.getLineStartOffset(doc.getLineNumber(endOffset)));

    if (tailRegion != null && tailRegion == headerRegion) return;

    final boolean selected;
    final IndentGuideDescriptor guide = editor.getIndentsModel().getCaretIndentGuide();
    if (guide != null) {
      final CaretModel caretModel = editor.getCaretModel();
      final int caretOffset = caretModel.getOffset();
      selected =
        caretOffset >= off && caretOffset < endOffset && caretModel.getLogicalPosition().column == indentColumn;
    }
    else {
      selected = false;
    }

    Point start = editor.visualPositionToXY(new VisualPosition(startPosition.line + lineShift, indentColumn));
    final VisualPosition endPosition = editor.offsetToVisualPosition(endOffset);
    Point end = editor.visualPositionToXY(new VisualPosition(endPosition.line, endPosition.column));
    int maxY = end.y;
    if (endPosition.line == editor.offsetToVisualPosition(doc.getTextLength()).line) {
      maxY += editor.getLineHeight();
    }

    Rectangle clip = g.getClipBounds();
    if (clip != null) {
      if (clip.y >= maxY || clip.y + clip.height <= start.y) {
        return;
      }
      maxY = Math.min(maxY, clip.y + clip.height);
    }

    final EditorColorsScheme scheme = editor.getColorsScheme();
    g.setColor(scheme.getColor(selected ? EditorColors.SELECTED_INDENT_GUIDE_COLOR : EditorColors.INDENT_GUIDE_COLOR));

    // There is a possible case that indent line intersects soft wrap-introduced text. Example:
    //     this is a long line <soft-wrap>
    // that| is soft-wrapped
    //     |
    //     | <- vertical indent
    //
    // Also it's possible that no additional intersections are added because of soft wrap:
    //     this is a long line <soft-wrap>
    //     |   that is soft-wrapped
    //     |
    //     | <- vertical indent
    // We want to use the following approach then:
    //     1. Show only active indent if it crosses soft wrap-introduced text;
    //     2. Show indent as is if it doesn't intersect with soft wrap-introduced text;
    if (selected) {
      LinePainter2D.paint((Graphics2D)g, start.x + 2, start.y, start.x + 2, maxY - 1);
    }
    else {
      int y = start.y;
      int newY = start.y;
      SoftWrapModel softWrapModel = editor.getSoftWrapModel();
      int lineHeight = editor.getLineHeight();
      for (int i = Math.max(0, startLine + lineShift); i < endLine && newY < maxY; i++) {
        List<? extends SoftWrap> softWraps = softWrapModel.getSoftWrapsForLine(i);
        int logicalLineHeight = softWraps.size() * lineHeight;
        if (i > startLine + lineShift) {
          logicalLineHeight += lineHeight; // We assume that initial 'y' value points just below the target line.
        }
        if (!softWraps.isEmpty() && softWraps.get(0).getIndentInColumns() < indentColumn) {
          if (y < newY || i > startLine + lineShift) { // There is a possible case that soft wrap is located on indent start line.
            LinePainter2D.paint((Graphics2D)g, start.x + 2, y, start.x + 2, newY + lineHeight - 1);
          }
          newY += logicalLineHeight;
          y = newY;
        }
        else {
          newY += logicalLineHeight;
        }

        FoldRegion foldRegion = foldingModel.getCollapsedRegionAtOffset(doc.getLineEndOffset(i));
        if (foldRegion != null && foldRegion.getEndOffset() < doc.getTextLength()) {
          i = doc.getLineNumber(foldRegion.getEndOffset());
        }
      }

      if (y < maxY) {
        LinePainter2D.paint((Graphics2D)g, start.x + 2, y, start.x + 2, maxY - 1);
      }
    }
  };

  IndentsPass(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    super(project, editor.getDocument(), false);
    myEditor = (EditorEx)editor;
    myFile = file;
  }

  @Override
  public void doCollectInformation(@NotNull ProgressIndicator progress) {
    assert myDocument != null;
    final Long stamp = myEditor.getUserData(LAST_TIME_INDENTS_BUILT);
    if (stamp != null && stamp.longValue() == nowStamp()) return;

    myDescriptors = buildDescriptors();

    ArrayList<TextRange> ranges = new ArrayList<>();
    for (IndentGuideDescriptor descriptor : myDescriptors) {
      ProgressManager.checkCanceled();
      int endOffset =
        descriptor.endLine < myDocument.getLineCount() ? myDocument.getLineStartOffset(descriptor.endLine) : myDocument.getTextLength();
      ranges.add(new TextRange(myDocument.getLineStartOffset(descriptor.startLine), endOffset));
    }

    Collections.sort(ranges, Segment.BY_START_OFFSET_THEN_END_OFFSET);
    myRanges = ranges;
  }

  private long nowStamp() {
    if (!myEditor.getSettings().isIndentGuidesShown()) return -1;
    assert myDocument != null;
    return myDocument.getModificationStamp();
  }

  @Override
  public void doApplyInformationToEditor() {
    final Long stamp = myEditor.getUserData(LAST_TIME_INDENTS_BUILT);
    if (stamp != null && stamp.longValue() == nowStamp()) return;

    List<RangeHighlighter> oldHighlighters = myEditor.getUserData(INDENT_HIGHLIGHTERS_IN_EDITOR_KEY);
    final List<RangeHighlighter> newHighlighters = new ArrayList<>();
    final MarkupModel mm = myEditor.getMarkupModel();

    int curRange = 0;

    if (oldHighlighters != null) {
      // after document change some range highlighters could have become invalid, or the order could have been broken
      oldHighlighters.sort(Comparator.comparing((RangeHighlighter h) -> !h.isValid())
                                     .thenComparing(Segment.BY_START_OFFSET_THEN_END_OFFSET));
      int curHighlight = 0;
      while (curRange < myRanges.size() && curHighlight < oldHighlighters.size()) {
        TextRange range = myRanges.get(curRange);
        RangeHighlighter highlighter = oldHighlighters.get(curHighlight);
        if (!highlighter.isValid()) break;

        int cmp = compare(range, highlighter);
        if (cmp < 0) {
          newHighlighters.add(createHighlighter(mm, range));
          curRange++;
        }
        else if (cmp > 0) {
          highlighter.dispose();
          curHighlight++;
        }
        else {
          newHighlighters.add(highlighter);
          curHighlight++;
          curRange++;
        }
      }

      for (; curHighlight < oldHighlighters.size(); curHighlight++) {
        RangeHighlighter highlighter = oldHighlighters.get(curHighlight);
        if (!highlighter.isValid()) break;
        highlighter.dispose();
      }
    }

    final int startRangeIndex = curRange;
    assert myDocument != null;
    DocumentUtil.executeInBulk(myDocument, myRanges.size() > 10000, () -> {
      for (int i = startRangeIndex; i < myRanges.size(); i++) {
        newHighlighters.add(createHighlighter(mm, myRanges.get(i)));
      }
    });


    myEditor.putUserData(INDENT_HIGHLIGHTERS_IN_EDITOR_KEY, newHighlighters);
    myEditor.putUserData(LAST_TIME_INDENTS_BUILT, nowStamp());
    myEditor.getIndentsModel().assumeIndents(myDescriptors);
  }

  private List<IndentGuideDescriptor> buildDescriptors() {
    if (!myEditor.getSettings().isIndentGuidesShown()) return Collections.emptyList();

    IndentsCalculator calculator = new IndentsCalculator();
    calculator.calculate();
    int[] lineIndents = calculator.lineIndents;

    IntStack lines = new IntStack();
    IntStack indents = new IntStack();

    lines.push(0);
    indents.push(0);
    assert myDocument != null;
    List<IndentGuideDescriptor> descriptors = new ArrayList<>();
    for (int line = 1; line < lineIndents.length; line++) {
      ProgressManager.checkCanceled();
      int curIndent = Math.abs(lineIndents[line]);

      while (!indents.empty() && curIndent <= indents.peek()) {
        ProgressManager.checkCanceled();
        final int level = indents.pop();
        int startLine = lines.pop();
        if (level > 0) {
          for (int i = startLine; i < line; i++) {
            if (level != Math.abs(lineIndents[i])) {
              descriptors.add(createDescriptor(level, startLine, line, lineIndents));
              break;
            }
          }
        }
      }

      int prevLine = line - 1;
      int prevIndent = Math.abs(lineIndents[prevLine]);

      if (curIndent - prevIndent > 1) {
        lines.push(prevLine);
        indents.push(prevIndent);
      }
    }

    while (!indents.empty()) {
      ProgressManager.checkCanceled();
      final int level = indents.pop();
      int startLine = lines.pop();
      if (level > 0) {
        descriptors.add(createDescriptor(level, startLine, myDocument.getLineCount(), lineIndents));
      }
    }
    return descriptors;
  }

  private IndentGuideDescriptor createDescriptor(int level, int startLine, int endLine, int[] lineIndents) {
    while (startLine > 0 && lineIndents[startLine] < 0) startLine--;
    int codeConstructStartLine = findCodeConstructStartLine(startLine);
    return new IndentGuideDescriptor(level, codeConstructStartLine, startLine, endLine);
  }

  private int findCodeConstructStartLine(int startLine) {
    DocumentEx document = myEditor.getDocument();
    CharSequence text = document.getImmutableCharSequence();
    int lineStartOffset = document.getLineStartOffset(startLine);
    int firstNonWsOffset = CharArrayUtil.shiftForward(text, lineStartOffset, " \t");
    FileType type = PsiUtilBase.getPsiFileAtOffset(myFile, firstNonWsOffset).getFileType();
    Language language = PsiUtilCore.getLanguageAtOffset(myFile, firstNonWsOffset);
    BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(type, language);
    HighlighterIterator iterator = myEditor.getHighlighter().createIterator(firstNonWsOffset);
    if (braceMatcher.isLBraceToken(iterator, text, type)) {
      int codeConstructStart = braceMatcher.getCodeConstructStart(myFile, firstNonWsOffset);
      return document.getLineNumber(codeConstructStart);
    }
    else {
      return startLine;
    }
  }

  @NotNull
  private static RangeHighlighter createHighlighter(MarkupModel mm, TextRange range) {
    final RangeHighlighter highlighter =
      mm.addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), 0, null, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setCustomRenderer(RENDERER);
    return highlighter;
  }

  private static int compare(@NotNull TextRange r, @NotNull RangeHighlighter h) {
    int answer = r.getStartOffset() - h.getStartOffset();
    return answer != 0 ? answer : r.getEndOffset() - h.getEndOffset();
  }

  private class IndentsCalculator {
    @NotNull final Map<Language, TokenSet> myComments = ContainerUtilRt.newHashMap();
    @NotNull final int[] lineIndents; // negative value means the line is empty (or contains a comment) and indent
    // (denoted by absolute value) was deduced from enclosing non-empty lines
    @NotNull final CharSequence myChars;

    IndentsCalculator() {
      assert myDocument != null;
      lineIndents = new int[myDocument.getLineCount()];
      myChars = myDocument.getCharsSequence();
    }

    /**
     * Calculates line indents for the {@link #myDocument target document}.
     */
    void calculate() {
      assert myDocument != null;
      final FileType fileType = myFile.getFileType();
      int tabSize = EditorUtil.getTabSize(myEditor);

      for (int line = 0; line < lineIndents.length; line++) {
        ProgressManager.checkCanceled();
        int lineStart = myDocument.getLineStartOffset(line);
        int lineEnd = myDocument.getLineEndOffset(line);
        int offset = lineStart;
        int column = 0;
        outer:
        while(offset < lineEnd) {
          switch (myChars.charAt(offset)) {
            case ' ':
              column++;
              break;
            case '\t':
              column = (column / tabSize + 1) * tabSize;
              break;
            default:
              break outer;
          }
          offset++;
        }
        // treating commented lines in the same way as empty lines
        // Blank line marker
        lineIndents[line] = offset == lineEnd || isComment(offset) ? -1 : column;
      }

      int topIndent = 0;
      for (int line = 0; line < lineIndents.length; line++) {
        ProgressManager.checkCanceled();
        if (lineIndents[line] >= 0) {
          topIndent = lineIndents[line];
        }
        else {
          int startLine = line;
          while (line < lineIndents.length && lineIndents[line] < 0) {
            //noinspection AssignmentToForLoopParameter
            line++;
          }

          int bottomIndent = line < lineIndents.length ? lineIndents[line] : topIndent;

          int indent = Math.min(topIndent, bottomIndent);
          if (bottomIndent < topIndent) {
            int lineStart = myDocument.getLineStartOffset(line);
            int lineEnd = myDocument.getLineEndOffset(line);
            int nonWhitespaceOffset = CharArrayUtil.shiftForward(myChars, lineStart, lineEnd, " \t");
            HighlighterIterator iterator = myEditor.getHighlighter().createIterator(nonWhitespaceOffset);
            if (BraceMatchingUtil.isRBraceToken(iterator, myChars, fileType)) {
              indent = topIndent;
            }
          }

          for (int blankLine = startLine; blankLine < line; blankLine++) {
            assert lineIndents[blankLine] == -1;
            lineIndents[blankLine] = - Math.min(topIndent, indent);
          }

          //noinspection AssignmentToForLoopParameter
          line--; // will be incremented back at the end of the loop;
        }
      }
    }

    private boolean isComment(int offset) {
      final HighlighterIterator it = myEditor.getHighlighter().createIterator(offset);
      IElementType tokenType = it.getTokenType();
      Language language = tokenType.getLanguage();
      TokenSet comments = myComments.get(language);
      if (comments == null) {
        ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(language);
        if (definition != null) {
          comments = definition.getCommentTokens();
        }
        if (comments == null) {
          return false;
        }
        else {
          myComments.put(language, comments);
        }
      }
      return comments.contains(tokenType);
    }
  }
}
