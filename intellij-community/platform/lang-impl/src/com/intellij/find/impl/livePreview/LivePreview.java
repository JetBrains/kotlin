// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl.livePreview;


import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.PositionTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

public class LivePreview implements SearchResults.SearchResultsListener, SelectionListener, DocumentListener, EditorColorsListener {
  private static final Key<RangeHighlighter> IN_SELECTION_KEY = Key.create("LivePreview.IN_SELECTION_KEY");
  private static final String EMPTY_STRING_DISPLAY_TEXT = "<Empty string>";

  private final Disposable myDisposable = Disposer.newDisposable("livePreview");
  private boolean mySuppressedUpdate = false;

  private static final Key<Boolean> MARKER_USED = Key.create("LivePreview.MARKER_USED");
  private static final Key<Boolean> SEARCH_MARKER = Key.create("LivePreview.SEARCH_MARKER");

  public static PrintStream ourTestOutput;
  private String myReplacementPreviewText;
  private static boolean NotFound;

  private final List<RangeHighlighter> myHighlighters = new ArrayList<>();
  private RangeHighlighter myCursorHighlighter;
  private VisibleAreaListener myVisibleAreaListener;
  private Delegate myDelegate;
  private final SearchResults mySearchResults;
  private Balloon myReplacementBalloon;

  @Override
  public void selectionChanged(@NotNull SelectionEvent e) {
    updateInSelectionHighlighters();
  }

  public static void processNotFound() {
    NotFound = true;
  }

  public interface Delegate {
    @Nullable
    String getStringToReplace(@NotNull Editor editor, @Nullable FindResult findResult)
      throws FindManager.MalformedReplacementStringException;
  }

  @Override
  public void searchResultsUpdated(@NotNull SearchResults sr) {
    if (mySuppressedUpdate) {
      mySuppressedUpdate = false;
      return;
    }
    highlightUsages();
    updateCursorHighlighting();
  }

  private void dumpState() {
    if (ApplicationManager.getApplication().isUnitTestMode() && ourTestOutput != null) {
      dumpEditorMarkupAndSelection(ourTestOutput);
    }
  }

  private void dumpEditorMarkupAndSelection(PrintStream dumpStream) {
    dumpStream.println(mySearchResults.getFindModel());
    if (myReplacementPreviewText != null) {
      dumpStream.println("--");
      dumpStream.println("Replacement Preview: " + myReplacementPreviewText);
    }
    dumpStream.println("--");

    Editor editor = mySearchResults.getEditor();

    RangeHighlighter[] highlighters = editor.getMarkupModel().getAllHighlighters();
    Arrays.sort(highlighters, Segment.BY_START_OFFSET_THEN_END_OFFSET);
    List<Pair<Integer, Character>> ranges = new ArrayList<>();
    for (RangeHighlighter highlighter : highlighters) {
      ranges.add(new Pair<>(highlighter.getStartOffset(), '['));
      ranges.add(new Pair<>(highlighter.getEndOffset(), ']'));
    }

    SelectionModel selectionModel = editor.getSelectionModel();

    if (selectionModel.getSelectionStart() != selectionModel.getSelectionEnd()) {
      ranges.add(new Pair<>(selectionModel.getSelectionStart(), '<'));
      ranges.add(new Pair<>(selectionModel.getSelectionEnd(), '>'));
    }
    ranges.add(new Pair<>(-1, '\n'));
    ranges.add(new Pair<>(editor.getDocument().getTextLength() + 1, '\n'));
    ContainerUtil.sort(ranges, (pair, pair2) -> {
      int res = pair.first - pair2.first;
      if (res == 0) {

        Character c1 = pair.second;
        Character c2 = pair2.second;
        if (c1 == '<' && c2 == '[') {
          return 1;
        }
        else if (c1 == '[' && c2 == '<') {
          return -1;
        }
        return c1.compareTo(c2);
      }
      return res;
    });

    Document document = editor.getDocument();
    for (int i = 0; i < ranges.size()-1; ++i) {
      Pair<Integer, Character> pair = ranges.get(i);
      Pair<Integer, Character> pair1 = ranges.get(i + 1);
      dumpStream.print(pair.second + document.getText(TextRange.create(Math.max(pair.first, 0),
                                                                       Math.min(pair1.first, document.getTextLength()))));
    }
    dumpStream.println("\n--");

    if (NotFound) {
      dumpStream.println("Not Found");
      dumpStream.println("--");
      NotFound = false;
    }

    for (RangeHighlighter highlighter : highlighters) {
      dumpStream.println(highlighter + " : " + highlighter.getTextAttributes());
    }
    dumpStream.println("------------");
  }

  private void clearUnusedHighlighters() {
    myHighlighters.removeIf(h -> {
      if (h.getUserData(MARKER_USED) == null) {
        removeHighlighterWithDependent(h);
        return true;
      }
      else {
        h.putUserData(MARKER_USED, null);
        return false;
      }
    });
  }

  private void removeHighlighterWithDependent(@NotNull RangeHighlighter highlighter) {
    removeHighlighter(highlighter);
    RangeHighlighter additionalHighlighter = highlighter.getUserData(IN_SELECTION_KEY);
    if (additionalHighlighter != null) {
      removeHighlighter(additionalHighlighter);
    }
  }

  @Override
  public void cursorMoved() {
    updateInSelectionHighlighters();
    updateCursorHighlighting();
  }

  @Override
  public void updateFinished() {
    dumpState();
  }

  private void updateCursorHighlighting() {
    hideBalloon();

    if (myCursorHighlighter != null) {
      removeHighlighter(myCursorHighlighter);
      myCursorHighlighter = null;
    }

    final FindResult cursor = mySearchResults.getCursor();
    Editor editor = mySearchResults.getEditor();
    if (cursor != null && cursor.getEndOffset() <= editor.getDocument().getTextLength()) {
      Color color = editor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
      myCursorHighlighter = addHighlighter(cursor.getStartOffset(), cursor.getEndOffset(),
                                           new TextAttributes(null, null, color, EffectType.ROUNDED_BOX, Font.PLAIN));

      editor.getScrollingModel().runActionOnScrollingFinished(() -> showReplacementPreview());
    }
  }

  public LivePreview(@NotNull SearchResults searchResults) {
    mySearchResults = searchResults;
    searchResultsUpdated(searchResults);
    searchResults.addListener(this);
    EditorUtil.addBulkSelectionListener(mySearchResults.getEditor(), this, myDisposable);
    ApplicationManager.getApplication().getMessageBus().connect(myDisposable).subscribe(EditorColorsManager.TOPIC, this);
  }

  public Delegate getDelegate() {
    return myDelegate;
  }

  public void setDelegate(Delegate delegate) {
    myDelegate = delegate;
  }

  @Override
  public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
    highlightUsages();
    updateCursorHighlighting();
  }

  public void dispose() {
    hideBalloon();

    for (RangeHighlighter h : myHighlighters) {
      removeHighlighterWithDependent(h);
    }
    myHighlighters.clear();

    if (myCursorHighlighter != null) {
      removeHighlighter(myCursorHighlighter);
    }
    myCursorHighlighter = null;

    Disposer.dispose(myDisposable);

    mySearchResults.removeListener(this);
  }

  private void highlightUsages() {
    List<RangeHighlighter> newHighlighters = mySearchResults.getMatchesCount() < mySearchResults.getMatchesLimit()
                                             ? addNewHighlighters() : Collections.emptyList();
    clearUnusedHighlighters();
    myHighlighters.addAll(newHighlighters);
    updateInSelectionHighlighters();
  }

  private List<RangeHighlighter> addNewHighlighters() {
    List<FindResult> occurrences = mySearchResults.getOccurrences();
    List<RangeHighlighter> newHighlighters = new ArrayList<>(occurrences.size());
    for (FindResult range : occurrences) {
      if (range.getEndOffset() > mySearchResults.getEditor().getDocument().getTextLength()) continue;
      TextAttributes attributes = createAttributes(range);
      RangeHighlighter existingHighlighter = findExistingHighlighter(range.getStartOffset(), range.getEndOffset(), attributes);
      if (existingHighlighter == null) {
        RangeHighlighter highlighter = addHighlighter(range.getStartOffset(), range.getEndOffset(), attributes);
        if (highlighter != null) {
          highlighter.putUserData(SEARCH_MARKER, Boolean.TRUE);
          newHighlighters.add(highlighter);
        }
      }
      else {
        existingHighlighter.putUserData(MARKER_USED, Boolean.TRUE);
      }
    }
    return newHighlighters;
  }

  private TextAttributes createAttributes(FindResult range) {
    EditorColorsScheme colorsScheme = mySearchResults.getEditor().getColorsScheme();
    if (mySearchResults.isExcluded(range)) {
      return new TextAttributes(null, null, colorsScheme.getDefaultForeground(), EffectType.STRIKEOUT, Font.PLAIN);
    }
    TextAttributes attributes = colorsScheme.getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    if (range.getLength() == 0) {
      attributes = attributes.clone();
      attributes.setEffectType(EffectType.BOXED);
      attributes.setEffectColor(attributes.getBackgroundColor());
    }
    return attributes;
  }

  private RangeHighlighter findExistingHighlighter(int startOffset, int endOffset, TextAttributes attributes) {
    MarkupModelEx markupModel = (MarkupModelEx)mySearchResults.getEditor().getMarkupModel();
    RangeHighlighter[] existing = new RangeHighlighter[1];
    markupModel.processRangeHighlightersOverlappingWith(startOffset, startOffset, highlighter -> {
      if (highlighter.getUserData(SEARCH_MARKER) != null &&
          highlighter.getStartOffset() == startOffset && highlighter.getEndOffset() == endOffset &&
          Objects.equals(highlighter.getTextAttributes(), attributes)) {
        existing[0] = highlighter;
        return false;
      }
      return true;
    });
    return existing[0];
  }

  private void updateInSelectionHighlighters() {
    final SelectionModel selectionModel = mySearchResults.getEditor().getSelectionModel();
    int[] starts = selectionModel.getBlockSelectionStarts();
    int[] ends = selectionModel.getBlockSelectionEnds();

    for (RangeHighlighter highlighter : myHighlighters) {
      if (!highlighter.isValid()) continue;
      boolean needsAdditionalHighlighting = false;
      TextRange cursor = mySearchResults.getCursor();
      if (cursor == null ||
          highlighter.getStartOffset() != cursor.getStartOffset() || highlighter.getEndOffset() != cursor.getEndOffset()) {
        for (int i = 0; i < starts.length; ++i) {
          TextRange selectionRange = new TextRange(starts[i], ends[i]);
          needsAdditionalHighlighting = selectionRange.intersects(highlighter.getStartOffset(), highlighter.getEndOffset()) &&
                           selectionRange.getEndOffset() != highlighter.getStartOffset() &&
                           highlighter.getEndOffset() != selectionRange.getStartOffset();
          if (needsAdditionalHighlighting) break;
        }
      }

      RangeHighlighter inSelectionHighlighter = highlighter.getUserData(IN_SELECTION_KEY);
      if (inSelectionHighlighter != null) {
        if (!needsAdditionalHighlighting) {
          removeHighlighter(inSelectionHighlighter);
        }
      } else if (needsAdditionalHighlighting) {
        RangeHighlighter additionalHighlighter = addHighlighter(highlighter.getStartOffset(), highlighter.getEndOffset(),
                                                                new TextAttributes(null, null,
                                                                                   Color.WHITE, EffectType.ROUNDED_BOX, Font.PLAIN));
        highlighter.putUserData(IN_SELECTION_KEY, additionalHighlighter);
      }
    }
  }

  private void showReplacementPreview() {
    hideBalloon();
    if (!mySearchResults.isUpToDate()) return;
    final FindResult cursor = mySearchResults.getCursor();
    final Editor editor = mySearchResults.getEditor();
    final FindModel findModel = mySearchResults.getFindModel();
    if (myDelegate != null && cursor != null && findModel.isReplaceState() && findModel.isRegularExpressions()) {
      String replacementPreviewText;
      try {
        replacementPreviewText = myDelegate.getStringToReplace(editor, cursor);
      }
      catch (FindManager.MalformedReplacementStringException e) {
        return;
      }
      if (replacementPreviewText == null) {
        return;//malformed replacement string
      }
      if (Registry.is("ide.find.show.replacement.hint.for.simple.regexp")) {
        showBalloon(editor, replacementPreviewText.isEmpty() ? EMPTY_STRING_DISPLAY_TEXT : replacementPreviewText);
      }
      else if (!replacementPreviewText.equals(findModel.getStringToReplace())) {
        showBalloon(editor, replacementPreviewText);
      }
    }
  }

  private void showBalloon(Editor editor, String replacementPreviewText) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      myReplacementPreviewText = replacementPreviewText;
      return;
    }

    ReplacementView replacementView = new ReplacementView(replacementPreviewText);

    BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(replacementView);
    balloonBuilder.setFadeoutTime(0);
    balloonBuilder.setFillColor(IdeTooltipManager.GRAPHITE_COLOR);
    balloonBuilder.setAnimationCycle(0);
    balloonBuilder.setHideOnClickOutside(false);
    balloonBuilder.setHideOnKeyOutside(false);
    balloonBuilder.setHideOnAction(false);
    balloonBuilder.setCloseButtonEnabled(true);
    myReplacementBalloon = balloonBuilder.createBalloon();
    EditorUtil.disposeWithEditor(editor, myReplacementBalloon);
    myReplacementBalloon.show(new ReplacementBalloonPositionTracker(editor), Balloon.Position.below);
  }

  private void hideBalloon() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      myReplacementPreviewText = null;
      return;
    }

    if (myReplacementBalloon != null) {
      myReplacementBalloon.hide();
      myReplacementBalloon = null;
    }

    removeVisibleAreaListener();
  }

  private void removeVisibleAreaListener() {
    if (myVisibleAreaListener != null) {
      mySearchResults.getEditor().getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
      myVisibleAreaListener = null;
    }
  }

  private RangeHighlighter addHighlighter(int startOffset, int endOffset, @NotNull TextAttributes attributes) {
    Project project = mySearchResults.getProject();
    if (project == null || project.isDisposed()) return null;
    List<RangeHighlighter> sink = new ArrayList<>();
    HighlightManager.getInstance(project).addRangeHighlight(mySearchResults.getEditor(), startOffset, endOffset, attributes, false, sink);
    RangeHighlighter result = ContainerUtil.getFirstItem(sink);
    if (result instanceof RangeHighlighterEx) ((RangeHighlighterEx)result).setVisibleIfFolded(true);
    return result;
  }

  private void removeHighlighter(@NotNull RangeHighlighter highlighter) {
    Project project = mySearchResults.getProject();
    if (project == null || project.isDisposed()) return;
    HighlightManager.getInstance(project).removeSegmentHighlighter(mySearchResults.getEditor(), highlighter);
  }

  private class ReplacementBalloonPositionTracker extends PositionTracker<Balloon> {
    private final Editor myEditor;

    ReplacementBalloonPositionTracker(Editor editor) {
      super(editor.getContentComponent());
      myEditor = editor;

    }

    @Override
    public RelativePoint recalculateLocation(final Balloon object) {
      FindResult cursor = mySearchResults.getCursor();
      if (cursor == null) return null;
      final TextRange cur = cursor;
      int startOffset = cur.getStartOffset();
      int endOffset = cur.getEndOffset();

      if (endOffset > myEditor.getDocument().getTextLength()) {
        if (!object.isDisposed()) {
          requestBalloonHiding(object);
        }
        return null;
      }
      if (!SearchResults.insideVisibleArea(myEditor, cur)) {
        requestBalloonHiding(object);

        removeVisibleAreaListener();
        myVisibleAreaListener = new VisibleAreaListener() {
          @Override
          public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
            if (SearchResults.insideVisibleArea(myEditor, cur)) {
              showReplacementPreview();
            }
          }
        };
        myEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
      }

      Point startPoint = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(startOffset));
      Point endPoint = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(endOffset));
      Point point = new Point((startPoint.x + endPoint.x)/2, startPoint.y + myEditor.getLineHeight());

      return new RelativePoint(myEditor.getContentComponent(), point);
    }
  }

  private static void requestBalloonHiding(final Balloon object) {
    ApplicationManager.getApplication().invokeLater(() -> object.hide());
  }
}
