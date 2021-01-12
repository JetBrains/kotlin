// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.google.common.collect.EvictingQueue;
import com.intellij.CommonBundle;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.largeFilesEditor.file.ReadingPageResultHandler;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.impl.status.PositionPanel;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorModel {
  private static final Logger LOG = Logger.getInstance(EditorModel.class);

  private static final int EDITOR_LINE_BEGINNING_INDENT = 5;
  private static final int PAGES_CASH_CAPACITY = 10;
  private static final int MAX_AVAILABLE_LINE_LENGTH = 1000;

  private final DataProvider dataProvider;

  private final Editor editor;
  private final DocumentOfPagesModel documentOfPagesModel;
  private final Collection<RangeHighlighter> pageRangeHighlighters;

  private final EvictingQueue<Page> pagesCash = EvictingQueue.create(PAGES_CASH_CAPACITY);
  private final List<Long> numbersOfRequestedForReadingPages = new LinkedList<>();
  private final AtomicBoolean isUpdateRequested = new AtomicBoolean(false);
  private boolean isBrokenMode = false;

  private final AbsoluteEditorPosition targetVisiblePosition = new AbsoluteEditorPosition(0, 0);
  private boolean isLocalScrollBarStabilized = false;

  private final AbsoluteSymbolPosition targetCaretPosition = new AbsoluteSymbolPosition(0, 0);
  private boolean isRealCaretAndSelectionCanAffectOnTarget = true;
  private boolean isNeedToShowCaret = false;
  private final SelectionState targetSelectionState = new SelectionState();

  private boolean wasViewPortInTheEndOfFileLastTime = false;
  private boolean isAllowedToFollowTheEndOfFile = false;

  private boolean isNeedToHighlightCloseSearchResults = false;
  private boolean isHighlightedSearchResultsAreStabilized = false;

  private JComponent myRootComponent;
  private final ExecutorService myPageReaderExecutor =
    AppExecutorUtil.createBoundedApplicationPoolExecutor("Large File Editor Page Reader Executor", 1);
  private final GlobalScrollBar myGlobalScrollBar;
  private final LocalInvisibleScrollBar myLocalInvisibleScrollBar;


  EditorModel(Document document, Project project, DataProvider dataProvider) {
    this.dataProvider = dataProvider;
    pageRangeHighlighters = new ArrayList<>();
    documentOfPagesModel = new DocumentOfPagesModel(document);

    documentOfPagesModel.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        fireHighlightedSearchResultsAreOutdated();
      }
    });

    editor = createSpecialEditor(document, project);

    editor.getCaretModel().addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@NotNull CaretEvent event) {
        fireRealCaretPositionChanged(event);
      }
    });

    editor.getSelectionModel().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(@NotNull SelectionEvent e) {
        fireRealSelectionChanged(e);
      }
    });

    myGlobalScrollBar = new GlobalScrollBar(this);

    myLocalInvisibleScrollBar = new LocalInvisibleScrollBar(this);
    JScrollPane scrollPaneInEditor = ((EditorEx)editor).getScrollPane();
    scrollPaneInEditor.setVerticalScrollBar(myLocalInvisibleScrollBar);
    scrollPaneInEditor.getVerticalScrollBar().setOpaque(true);

    myRootComponent = editor.getComponent();
    insertGlobalScrollBarIntoEditorComponent();
  }

  private void insertGlobalScrollBarIntoEditorComponent() {
    JComponent mainPanelInEditor = editor.getComponent();
    LayoutManager layout = mainPanelInEditor.getLayout();
    if (layout instanceof BorderLayout) {
      BorderLayout borderLayout = (BorderLayout)layout;
      Component originalCentralComponent = borderLayout.getLayoutComponent(BorderLayout.CENTER);

      JBLayeredPane intermediatePane = new JBLayeredPane() {
        @Override
        public void doLayout() {
          final Component[] components = getComponents();
          final Rectangle bounds = getBounds();
          for (Component component : components) {
            if (component == myGlobalScrollBar) {
              int scrollBarWidth = myGlobalScrollBar.getPreferredSize().width;
              component.setBounds(bounds.width - scrollBarWidth, 0,
                                  scrollBarWidth, bounds.height);
            }
            else {
              component.setBounds(0, 0, bounds.width, bounds.height);
            }
          }
        }

        @Override
        public Dimension getPreferredSize() {
          return originalCentralComponent.getPreferredSize();
        }
      };

      intermediatePane.add(originalCentralComponent, JLayeredPane.DEFAULT_LAYER);
      intermediatePane.add(myGlobalScrollBar, JLayeredPane.PALETTE_LAYER);

      mainPanelInEditor.add(intermediatePane, BorderLayout.CENTER);
    }
    else {  // layout instanceof BorderLayout == false
      LOG.info("[Large File Editor Subsystem] EditorModel.insertGlobalScrollBarIntoEditorComponent():" +
               " can't insert GlobalScrollBar in normal way.");
      myRootComponent = new JPanel();
      myRootComponent.setLayout(new BorderLayout());
      myRootComponent.add(editor.getComponent(), BorderLayout.CENTER);
      myRootComponent.add(myGlobalScrollBar, BorderLayout.EAST);
    }
  }

  long getCaretPageNumber() {
    return targetCaretPosition.pageNumber;
  }

  int getCaretPageOffset() {
    return targetCaretPosition.symbolOffsetInPage;
  }

  Editor getEditor() {
    return editor;
  }

  JComponent getComponent() {
    return myRootComponent;
  }

  void setBrokenMode() {
    isBrokenMode = true;
    requestUpdate();
  }

  void addCaretListener(CaretListener caretListener) {
    editor.getCaretModel().addCaretListener(caretListener);
  }

  <T> void putUserDataToEditor(@NotNull Key<T> key, T value) {
    editor.putUserData(key, value);
  }

  private void fireRealCaretPositionChanged(@NotNull CaretEvent event) {
    if (isRealCaretAndSelectionCanAffectOnTarget) {
      reflectRealToTargetCaretPosition();
      isNeedToShowCaret = true;

      if (editor.getSelectionModel().getSelectionEnd() == editor.getSelectionModel().getSelectionStart()) {
        targetSelectionState.isExists = false;
      }

      if (event.getOldPosition().line != event.getNewPosition().line) {
        IdeDocumentHistory docHistory = IdeDocumentHistory.getInstance(dataProvider.getProject());
        if (docHistory != null) {
          docHistory.includeCurrentCommandAsNavigation();
          docHistory.setCurrentCommandHasMoves();
        }
      }

      requestUpdate();
    }
  }

  private void reflectRealToTargetCaretPosition() {
    int caretOffset = editor.getCaretModel().getPrimaryCaret().getOffset();
    AbsoluteSymbolPosition absoluteSymbolPosition = documentOfPagesModel.offsetToAbsoluteSymbolPosition(caretOffset);
    targetCaretPosition.set(absoluteSymbolPosition);
  }

  private void fireRealSelectionChanged(SelectionEvent selectionEvent) {
    if (isRealCaretAndSelectionCanAffectOnTarget) {
      reflectRealToTargetSelection(selectionEvent);
    }
  }

  private void reflectRealToTargetSelection(SelectionEvent selectionEvent) {
    TextRange newSelectionRange = selectionEvent.getNewRange();
    if (newSelectionRange == null || newSelectionRange.isEmpty()) {
      targetSelectionState.isExists = false;
    }
    else {
      int startOffset = newSelectionRange.getStartOffset();
      int endOffset = newSelectionRange.getEndOffset();
      AbsoluteSymbolPosition startAbsolutePosition = documentOfPagesModel.offsetToAbsoluteSymbolPosition(startOffset);
      AbsoluteSymbolPosition endAbsolutePosition = documentOfPagesModel.offsetToAbsoluteSymbolPosition(endOffset);
      targetSelectionState.set(startAbsolutePosition, endAbsolutePosition);
      targetSelectionState.isExists = true;
    }
  }

  private void fireHighlightedSearchResultsAreOutdated() {
    isHighlightedSearchResultsAreStabilized = false;
    requestUpdate();
  }

  public void fireGlobalScrollBarValueChangedFromOutside(long pageNumber) {
    long pagesAmount;
    try {
      pagesAmount = dataProvider.getPagesAmount();
    }
    catch (IOException e) {
      LOG.info(e);
      return;
    }

    if (pageNumber < 0 || pageNumber > pagesAmount) {
      LOG.warn("[Large File Editor Subsystem] EditorModel.fireGlobalScrollBarValueChangedFromOutside(pageNumber):" +
               " Illegal argument pageNumber. Expected: 0 < pageNumber <= pagesAmount." +
               " Actual: pageNumber=" + pageNumber + " pagesAmount=" + pagesAmount);
      return;
    }
    targetVisiblePosition.set(pageNumber, 0);
    update();
  }

  private void requestUpdate() {
    // elimination of duplicates of update() tasks in EDT queue
    if (isUpdateRequested.compareAndSet(false, true)) {
      ApplicationManager.getApplication().invokeLater(() -> {
        isUpdateRequested.set(false);
        update();
      });
    }
  }

  @CalledInAwt
  private void update() {
    if (isBrokenMode) {
      documentOfPagesModel.removeAllPages(dataProvider.getProject());
      return;
    }

    long pagesAmountInFile;
    try {
      pagesAmountInFile = dataProvider.getPagesAmount();
    }
    catch (IOException e) {
      LOG.info(e);
      Messages.showErrorDialog(EditorBundle.message("large.file.editor.message.error.while.working.with.file.try.to.reopen.it"),
                               CommonBundle.getErrorTitle());
      return;
    }

    if (isNeedToShowCaret) {
      ensureTargetCaretPositionIsInFileBounds(pagesAmountInFile);
      targetVisiblePosition.set(targetCaretPosition.pageNumber,
                                targetVisiblePosition.verticalScrollOffset);  // we can't count the necessary offset at this stage
    }

    normalizePagesInDocumentListBeginning();

    if (pagesAmountInFile == 0) {
      return;
    }

    if (documentOfPagesModel.getPagesAmount() == 0) {
      long pageNumber = targetVisiblePosition.pageNumber == 0
                        ? 0
                        : targetVisiblePosition.pageNumber == pagesAmountInFile  // to avoid redundant document rebuilding
                          ? targetVisiblePosition.pageNumber - 2
                          : targetVisiblePosition.pageNumber - 1;
      Page page = tryGetPageFromCash(pageNumber);
      if (page != null) {
        setNextPageIntoDocument(page);
      }
      else {
        requestReadPage(pageNumber);
        return;
      }
    }

    if (isNeedToTurnOnSoftWrapping()) {
      // TODO: 2019-05-21 need to notify user someway
      editor.getSettings().setUseSoftWraps(true);
    }

    normalizePagesInDocumentListEnding();

    ensureDocumentLastPageIsValid(pagesAmountInFile);

    if (wasViewPortInTheEndOfFileLastTime && isAllowedToFollowTheEndOfFile) {
      targetVisiblePosition.set(Long.MAX_VALUE, 0);
      isNeedToShowCaret = false;
      isAllowedToFollowTheEndOfFile = false;
    }

    tryReflectTargetCaretPositionToReal();

    tryReflectTargetSelectionToReal();

    tryNormalizeTargetVisiblePosition();

    if (!isLocalScrollBarStabilized) {
      tryScrollToTargetVisiblePosition();
    }

    updateGlobalStrollBarView();

    long nextPageNumberToAdd = tryGetNextPageNumberToAdd(pagesAmountInFile);
    if (nextPageNumberToAdd != -1) {
      Page nextPageToAdd = tryGetPageFromCash(nextPageNumberToAdd);
      if (nextPageToAdd != null) {
        setNextPageIntoDocument(nextPageToAdd);
        requestUpdate();
      }
      else {
        requestReadPage(nextPageNumberToAdd);
      }
      return;
    }

    pagesCash.clear();

    if (isNeedToShowCaret) {
      if (isLocalScrollBarStabilized) {
        showCaretThatIsNeededToShow();
      }
      else {
        requestUpdate();
        return;
      }
    }
    else {
      if (!isRealCaretMatchesTarget()) {
        setCaretToConvenientPosition();
      }
    }

    wasViewPortInTheEndOfFileLastTime = false;
    if (documentOfPagesModel.getLastPage().getPageNumber() == pagesAmountInFile - 1 &&
        myLocalInvisibleScrollBar.getValue() + myLocalInvisibleScrollBar.getHeight() == myLocalInvisibleScrollBar.getMaximum()) {
      wasViewPortInTheEndOfFileLastTime = true;
    }

    tryHighlightSearchResultsIfNeed();
  }

  // IDEA-232158
  private void ensureDocumentLastPageIsValid(long pagesAmountInFile) {
    Page lastPageInDocument = documentOfPagesModel.getLastPage();
    if (lastPageInDocument.isLastInFile() != (lastPageInDocument.getPageNumber() == pagesAmountInFile - 1)) {
      removeLastPageFromDocument();
      pagesCash.removeIf(page -> lastPageInDocument.getPageNumber() == page.getPageNumber());
    }
  }

  private boolean isNeedToTurnOnSoftWrapping() {
    return !editor.getSettings().isUseSoftWraps() && isExistLineWithTooLargeLength();
  }

  // TODO: 2019-05-13 can be optimized by checking lines only for last added page
  private boolean isExistLineWithTooLargeLength() {
    Document document = documentOfPagesModel.getDocument();
    int lineCount = document.getLineCount();
    for (int i = 0; i < lineCount; i++) {
      int lineWidth = document.getLineEndOffset(i) - document.getLineStartOffset(i);
      if (lineWidth > MAX_AVAILABLE_LINE_LENGTH) {
        return true;
      }
    }
    return false;
  }

  private void tryHighlightSearchResultsIfNeed() {
    if (isNeedToHighlightCloseSearchResults) {
      if (!isHighlightedSearchResultsAreStabilized) {
        updateSearchResultsHighlighting();
        isHighlightedSearchResultsAreStabilized = true;
      }
    }
    else {
      clearHighlightedSearchResults();
    }
  }

  private void updateSearchResultsHighlighting() {
    clearHighlightedSearchResults();

    List<TextRange> highlightRanges = getAllSearchResultsRangesInDocument();

    if (!highlightRanges.isEmpty()) {
      HighlightManager highlightManager = HighlightManager.getInstance(dataProvider.getProject());

      for (TextRange range : highlightRanges) {
        highlightManager.addRangeHighlight(
          editor,
          range.getStartOffset(),
          range.getEndOffset(),
          EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES, true, pageRangeHighlighters);
      }
    }
  }

  private List<TextRange> getAllSearchResultsRangesInDocument() {
    List<TextRange> searchResultsRanges = new ArrayList<>();
    for (int i = 0; i < documentOfPagesModel.getPagesList().size(); i++) {
      Page page = documentOfPagesModel.getPagesList().get(i);
      List<SearchResult> searchResults = dataProvider.getSearchResultsInPage(page);
      if (searchResults != null) {
        for (SearchResult result : searchResults) {
          int from = documentOfPagesModel.getSymbolOffsetToStartOfPage(
            documentOfPagesModel.getIndexOfPageByPageNumber(result.startPosition.pageNumber))
                     + result.startPosition.symbolOffsetInPage;
          int to = documentOfPagesModel.getSymbolOffsetToStartOfPage(
            documentOfPagesModel.getIndexOfPageByPageNumber(result.endPostion.pageNumber))
                   + result.endPostion.symbolOffsetInPage;
          if (from < 0) from = 0;
          if (to > documentOfPagesModel.getDocument().getTextLength()) to = documentOfPagesModel.getDocument().getTextLength();
          if (from < to) {
            searchResultsRanges.add(new TextRange(from, to));
          }
        }
      }
    }
    return searchResultsRanges;
  }

  private void clearHighlightedSearchResults() {
    final HighlightManager highlightManager = HighlightManager.getInstance(dataProvider.getProject());
    for (RangeHighlighter pageRangeHighlighter : pageRangeHighlighters) {
      highlightManager.removeSegmentHighlighter(editor, pageRangeHighlighter);
    }
    pageRangeHighlighters.clear();
  }

  private void showCaretThatIsNeededToShow() {
    int targetCaretOffsetInDocument = tryGetTargetCaretOffsetInDocumentWithMargin();
    if (targetCaretOffsetInDocument != -1) {
      if (!isRealCaretInsideTargetVisibleArea()) {
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      }
    }
    else {
      LOG.warn("[Large File Editor Subsystem] EditorMode.update(): can't show caret. "
               + " targetCaretPosition.pageNumber=" + targetCaretPosition.pageNumber
               + " targetCaretPosition.symbolOffsetInPage=" + targetCaretPosition.symbolOffsetInPage
               + " targetVisiblePosition.pageNumber=" + targetVisiblePosition.pageNumber
               + " targetVisiblePosition.verticalScrollOffset=" + targetVisiblePosition.verticalScrollOffset);
    }
    isNeedToShowCaret = false;
  }

  private boolean isRealCaretInsideTargetVisibleArea() {
    Point pointOfCaret = editor.offsetToXY(editor.getCaretModel().getOffset());
    Rectangle area = editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
    return area.contains(pointOfCaret);
  }

  private void setCaretToConvenientPosition() {
    int offset = tryGetConvenientOffsetForCaret();
    if (offset != -1) {
      runCaretAndSelectionListeningTransparentCommand(
        () -> editor.getCaretModel().moveToOffset(offset));
      reflectRealToTargetCaretPosition();
    }
    else {
      LOG.info("[Large File Editor Subsystem] EditorModel.setCaretToConvenientPosition(): " +
               "Can't set caret to convenient position.");
    }
  }

  private boolean isRealCaretMatchesTarget() {
    int targetCaretOffsetInDocument = tryGetTargetCaretOffsetInDocumentWithMargin();
    return targetCaretOffsetInDocument == editor.getCaretModel().getOffset();
  }

  private void tryReflectTargetCaretPositionToReal() {
    int targetCaretOffsetInDocument = tryGetTargetCaretOffsetInDocumentWithMargin();
    if (targetCaretOffsetInDocument != -1) {
      runCaretAndSelectionListeningTransparentCommand(
        () -> editor.getCaretModel().moveToOffset(targetCaretOffsetInDocument));
    }
  }

  private void tryReflectTargetSelectionToReal() {
    if (targetSelectionState.isExists &&
        documentOfPagesModel.getPagesAmount() != 0) {
      int docLength = documentOfPagesModel.getDocument().getTextLength();

      int startOffset = Math.min(documentOfPagesModel.absoluteSymbolPositionToOffset(targetSelectionState.start),
                                 docLength);
      int endOffset = Math.min(documentOfPagesModel.absoluteSymbolPositionToOffset(targetSelectionState.end),
                               docLength);

      runCaretAndSelectionListeningTransparentCommand(() -> {
        if (startOffset == endOffset) {
          editor.getSelectionModel().removeSelection();
        }
        else {
          editor.getSelectionModel().setSelection(startOffset, endOffset);
        }
      });
    }
  }

  private int tryGetConvenientOffsetForCaret() {
    Rectangle visibleArea = editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
    return editor.logicalPositionToOffset(editor.xyToLogicalPosition(new Point(
      0, visibleArea.y + editor.getLineHeight())));
  }

  private int tryGetTargetCaretOffsetInDocumentWithMargin() {
    if (documentOfPagesModel.getPagesAmount() == 0) {
      return 0;
    }

    int offset = tryGetTargetCaretOffsetInDocument();
    if (offset == -1) return -1;

    int startOfAllowedRange = tryGetStartMarginForTargetCaretInDocument();

    int endOfAllowedRange = getSymbolOffsetToStartOfPage(documentOfPagesModel.getPagesAmount());
    try {
      if (documentOfPagesModel.getLastPage().getPageNumber() != dataProvider.getPagesAmount() - 1) {
        endOfAllowedRange -= documentOfPagesModel.getLastPage().getText().length() / 2;
      }
    }
    catch (IOException e) {
      LOG.warn(e);
    }

    if (offset >= startOfAllowedRange && offset <= endOfAllowedRange) {
      return offset;
    }
    else {
      return -1;
    }
  }

  private int tryGetStartMarginForTargetCaretInDocument() {
    if (documentOfPagesModel.getPagesAmount() > 0) {
      if (documentOfPagesModel.getFirstPage().getPageNumber() == 0) {
        return 0;
      }
      else {
        return documentOfPagesModel.getFirstPage().getText().length() / 2;
      }
    }
    else {
      return 0;
    }
  }

  private int tryGetTargetCaretOffsetInDocument() {
    int indexOfPage = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetCaretPosition.pageNumber);

    int targetCaretOffsetInDocument;

    if (indexOfPage != -1) {
      targetCaretOffsetInDocument = getSymbolOffsetToStartOfPage(indexOfPage)
                                    + targetCaretPosition.symbolOffsetInPage;
    }
    else if (targetCaretPosition.symbolOffsetInPage == 0 &&
             targetCaretPosition.pageNumber == documentOfPagesModel.getLastPage().getPageNumber() + 1) {
      targetCaretOffsetInDocument = getSymbolOffsetToStartOfPage(documentOfPagesModel.getPagesAmount());
    }
    else {
      return -1;
    }

    int docLength = documentOfPagesModel.getDocument().getTextLength();
    if (targetCaretOffsetInDocument >= 0) {
      return Math.min(targetCaretOffsetInDocument, docLength);
    }
    else {
      LOG.warn("[Large File Editor Subsystem] EditorModel.tryGetTargetCaretOffsetInDocument():"
               + " Invalid targetCaretPosition state."
               + " targetCaretPosition.pageNumber=" + targetCaretPosition.pageNumber
               + " targetCaretPosition.symbolOffsetInPage=" + targetCaretPosition.symbolOffsetInPage
               + " targetCaretOffsetInDocument=" + targetCaretOffsetInDocument
               + " document.getTextLength()=" + docLength);
    }
    return -1;
  }

  // TODO: 2019-04-10 need to handle possible 'long' values
  private void updateGlobalStrollBarView() {
    long pagesAmount;
    try {
      pagesAmount = dataProvider.getPagesAmount();
    }
    catch (IOException e) {
      LOG.warn(e);
      return;
    }

    if (pagesAmount >= Integer.MAX_VALUE) {
      LOG.warn("[Large File Editor Subsystem] EditorModel.updateGlobalStrollBarView():" +
               "pagesAmount > Integer.MAX_VALUE. pagesAmount=" + pagesAmount);
    }

    int extent = 1; // to make thumb of minimum size
    myGlobalScrollBar.setValues((int)targetVisiblePosition.pageNumber, extent, 0, (int)pagesAmount + 1);
  }

  private long tryGetNextPageNumberToAdd(long pagesAmountInFile) {
    if (documentOfPagesModel.getPagesAmount() == 0) {
      return targetVisiblePosition.pageNumber == 0
             ? targetVisiblePosition.pageNumber
             : targetVisiblePosition.pageNumber - 1;
    }

    int visibleTargetPageIndex = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetVisiblePosition.pageNumber);
    if (visibleTargetPageIndex == -1) {
      // some pages before visible one exist and are located in list => just need to get next to last in list
      return tryGetNumberOfNextToDocumentPage(pagesAmountInFile);
    }

    // check if we really need some extra pages or already not
    int offsetToFirstVisibleSymbolOfTargetVisiblePage = getSymbolOffsetToStartOfPage(visibleTargetPageIndex);
    int topOfTargetVisiblePage = offsetToY(offsetToFirstVisibleSymbolOfTargetVisiblePage);
    int topOfTargetVisibleArea = topOfTargetVisiblePage + targetVisiblePosition.verticalScrollOffset;
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    int bottomOfTargetVisibleArea = topOfTargetVisibleArea + visibleArea.height;

    int lastVisiblePageIndex = visibleTargetPageIndex;
    int offsetToEndOfLastVisiblePage = getSymbolOffsetToStartOfPage(lastVisiblePageIndex + 1);
    while (lastVisiblePageIndex + 1 < documentOfPagesModel.getPagesAmount()
           && offsetToY(offsetToEndOfLastVisiblePage) <= bottomOfTargetVisibleArea) {
      lastVisiblePageIndex++;
      offsetToEndOfLastVisiblePage = getSymbolOffsetToStartOfPage(lastVisiblePageIndex + 1);
    }

    if (documentOfPagesModel.getPagesAmount() - 1 == lastVisiblePageIndex) {
      // we need at least 1 invisible page after visible ones, if it's not the end
      return tryGetNumberOfNextToDocumentPage(pagesAmountInFile);
    }
    return -1;
  }

  private long tryGetNumberOfNextToDocumentPage(long pagesAmountInFile) {
    if (documentOfPagesModel.getPagesAmount() > 0) {
      long nextPageNumber = documentOfPagesModel.getLastPage().getPageNumber() + 1;
      return nextPageNumber < pagesAmountInFile ? nextPageNumber : -1;
    }
    else {
      return -1;
    }
  }

  private void tryNormalizeTargetVisiblePosition() {
    boolean needToRepeat;
    try {
      do {
        needToRepeat = tryNormalizeTargetEditorViewPosition_iteration();
      }
      while (needToRepeat);
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  private boolean tryNormalizeTargetEditorViewPosition_iteration() throws IOException {
    long pagesAmountInFile = dataProvider.getPagesAmount();

    if (pagesAmountInFile == 0) {
      targetVisiblePosition.set(0, 0);
      return false;
    }

    if (targetVisiblePosition.pageNumber >= pagesAmountInFile) {
      targetVisiblePosition.set(pagesAmountInFile, -1);
    }

    if (targetVisiblePosition.verticalScrollOffset < 0) {
      if (targetVisiblePosition.pageNumber == 0) {
        targetVisiblePosition.set(0, 0);
        return false;
      }

      int prevPageIndex = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetVisiblePosition.pageNumber - 1);
      if (prevPageIndex == -1) {
        return false;
      }

      int symbolOffsetToBeginningOfPrevPage = getSymbolOffsetToStartOfPage(prevPageIndex);
      int symbolOffsetToBeginningOfTargetPage = getSymbolOffsetToStartOfPage(prevPageIndex + 1);
      int verticalOffsetToBeginningOfPrevPage = offsetToY(symbolOffsetToBeginningOfPrevPage);
      int verticalOffsetToBeginningOfTargetPage = offsetToY(symbolOffsetToBeginningOfTargetPage);

      targetVisiblePosition.set(targetVisiblePosition.pageNumber - 1,
                                targetVisiblePosition.verticalScrollOffset
                                + verticalOffsetToBeginningOfTargetPage - verticalOffsetToBeginningOfPrevPage);
      return true;
    }

    // here targetVisiblePosition.pageNumber < pagesAmountInFile
    //   && targetVisiblePosition.verticalScrollOffset >= 0

    int visibleTargetPageIndex = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetVisiblePosition.pageNumber);
    if (visibleTargetPageIndex == -1) {
      return false;
    }

    int symbolOffsetToBeginningOfTargetVisiblePage = getSymbolOffsetToStartOfPage(visibleTargetPageIndex);
    int symbolOffsetToEndOfTargetVisiblePage = getSymbolOffsetToStartOfPage(visibleTargetPageIndex + 1);
    int topOfTargetVisiblePage = offsetToY(symbolOffsetToBeginningOfTargetVisiblePage);

    int bottomOfExpectedVisibleArea =
      topOfTargetVisiblePage + targetVisiblePosition.verticalScrollOffset + editor.getScrollingModel().getVisibleArea().height;
    if (bottomOfExpectedVisibleArea > editor.getContentComponent().getHeight()) {
      int indexOfLastLastPage = documentOfPagesModel.tryGetIndexOfNeededPageInList(pagesAmountInFile - 1);
      if (indexOfLastLastPage == -1) {
        return false;
      }
      int extraDifference = bottomOfExpectedVisibleArea - editor.getContentComponent().getHeight();
      targetVisiblePosition.set(targetVisiblePosition.pageNumber,
                                targetVisiblePosition.verticalScrollOffset - extraDifference);
      return true;
    }

    // here targetVisiblePosition.pageNumber < pagesAmountInFile
    //   && targetVisiblePosition.verticalScrollOffset >= 0
    //   && bottomOfExpectedVisibleArea <= editor.getContentComponent().getHeight()

    int bottomOfTargetVisiblePage = offsetToY(symbolOffsetToEndOfTargetVisiblePage);
    if (topOfTargetVisiblePage + targetVisiblePosition.verticalScrollOffset >= bottomOfTargetVisiblePage) {
      targetVisiblePosition.set(targetVisiblePosition.pageNumber + 1,
                                targetVisiblePosition.verticalScrollOffset
                                - bottomOfTargetVisiblePage + topOfTargetVisiblePage);
      return true;
    }
    return false;
  }

  private void ensureTargetCaretPositionIsInFileBounds(long pagesAmount) {
    if (targetCaretPosition.pageNumber < 0) {
      targetCaretPosition.set(0, 0);
    }
    else if (targetCaretPosition.pageNumber >= pagesAmount) {
      targetCaretPosition.set(pagesAmount, 0);
    }
  }

  private void normalizePagesInDocumentListEnding() {
    int visibleTargetPageIndex = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetVisiblePosition.pageNumber);
    if (visibleTargetPageIndex == -1) {
      return;
    }

    int offsetToFirstVisibleSymbolOfTargetVisiblePage = getSymbolOffsetToStartOfPage(visibleTargetPageIndex);
    int topOfTargetVisiblePage = offsetToY(offsetToFirstVisibleSymbolOfTargetVisiblePage);
    int topOfTargetVisibleArea = topOfTargetVisiblePage + targetVisiblePosition.verticalScrollOffset;
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    int bottomOfTargetVisibleArea = topOfTargetVisibleArea + visibleArea.height;

    int lastVisiblePageIndex = visibleTargetPageIndex;
    int offsetToEndOfLastVisiblePage = getSymbolOffsetToStartOfPage(lastVisiblePageIndex + 1);
    while (lastVisiblePageIndex + 1 < documentOfPagesModel.getPagesAmount()
           && offsetToY(offsetToEndOfLastVisiblePage) <= bottomOfTargetVisibleArea) {
      lastVisiblePageIndex++;
      offsetToEndOfLastVisiblePage = getSymbolOffsetToStartOfPage(lastVisiblePageIndex + 1);
    }

    int maxAllowedAmountOfPagesInDocumentHere = lastVisiblePageIndex + 2 + 1;  // max 2 invisible pages in the end

    while (documentOfPagesModel.getPagesAmount() > maxAllowedAmountOfPagesInDocumentHere) {
      removeLastPageFromDocument();
    }
  }

  private void tryScrollToTargetVisiblePosition() {
    int visibleTargetPageIndex = documentOfPagesModel.tryGetIndexOfNeededPageInList(targetVisiblePosition.pageNumber);
    if (visibleTargetPageIndex == -1) {
      return;
    }

    int symbolOffsetToBeginningOfTargetVisiblePage = getSymbolOffsetToStartOfPage(visibleTargetPageIndex);
    int topOfTargetVisiblePage = offsetToY(symbolOffsetToBeginningOfTargetVisiblePage);
    int targetTopOfVisibleArea = topOfTargetVisiblePage + targetVisiblePosition.verticalScrollOffset;

    if (editor.getScrollingModel().getVisibleAreaOnScrollingFinished().y != targetTopOfVisibleArea) {
      editor.getScrollingModel().disableAnimation();
      editor.getScrollingModel().scrollVertically(targetTopOfVisibleArea);
      editor.getScrollingModel().enableAnimation();
    }

    if (editor.getScrollingModel().getVisibleAreaOnScrollingFinished().y == targetTopOfVisibleArea) {
      isLocalScrollBarStabilized = true;
    }
  }

  public void setCaretToFileEndAndShow() {
    long pagesAmount;
    try {
      pagesAmount = dataProvider.getPagesAmount();
    }
    catch (IOException e) {
      LOG.info(e);
      return;
    }
    setCaretAndShow(pagesAmount, 0);
  }

  public void setCaretToFileStartAndShow() {
    setCaretAndShow(0, 0);
  }

  public void setCaretAndShow(long pageNumber, int symbolOffsetInPage) {
    targetCaretPosition.set(pageNumber, symbolOffsetInPage);
    isNeedToShowCaret = true;
    requestUpdate();
  }

  private int offsetToY(int offset) {
    return editor.offsetToXY(offset).y;
  }

  private int getSymbolOffsetToStartOfPage(int indexOfPage) {
    return documentOfPagesModel.getSymbolOffsetToStartOfPage(indexOfPage);
  }

  private void requestReadPage(long pageNumber) {
    if (!numbersOfRequestedForReadingPages.contains(pageNumber)) {
      dataProvider.requestReadPage(pageNumber, page -> tellPageWasRead(pageNumber, page));
      numbersOfRequestedForReadingPages.add(pageNumber);
    }
  }

  private void tellPageWasRead(long pageNumber, Page page) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (page == null) {
        LOG.warn("page with number " + pageNumber + " is null.");
        return;
      }

      pagesCash.add(page);
      numbersOfRequestedForReadingPages.remove(pageNumber);
      requestUpdate();
    });
  }

  private void setNextPageIntoDocument(Page page) {
    runCaretAndSelectionListeningTransparentCommand(
      () -> documentOfPagesModel.addPageIntoEnd(page, dataProvider.getProject()));
  }

  private void removeLastPageFromDocument() {
    if (documentOfPagesModel.getPagesAmount() > 0) {
      pagesCash.add(documentOfPagesModel.getLastPage());
      runCaretAndSelectionListeningTransparentCommand(
        () -> documentOfPagesModel.removeLastPage(dataProvider.getProject()));
    }
  }

  private void deleteAllPagesFromDocument() {
    pagesCash.addAll(documentOfPagesModel.getPagesList());
    runCaretAndSelectionListeningTransparentCommand(
      () -> documentOfPagesModel.removeAllPages(dataProvider.getProject()));
  }

  private void runCaretAndSelectionListeningTransparentCommand(Runnable command) {
    isRealCaretAndSelectionCanAffectOnTarget = false;
    command.run();
    isRealCaretAndSelectionCanAffectOnTarget = true;
  }

  private Page tryGetPageFromCash(long pageNumber) {
    for (Page page : pagesCash) {
      if (page.getPageNumber() == pageNumber) {
        return page;
      }
    }
    return null;
  }

  private void normalizePagesInDocumentListBeginning() {
    if (!isOkBeginningOfPagesInDocumentList()) {
      isLocalScrollBarStabilized = false;
      deleteAllPagesFromDocument();
    }
  }

  private boolean isOkBeginningOfPagesInDocumentList() {
    int listSize = documentOfPagesModel.getPagesAmount();

    if (listSize < 1) {
      return true;
    }

    long numberOfPage0 = documentOfPagesModel.getPageByIndex(0).getPageNumber();
    if (numberOfPage0 != targetVisiblePosition.pageNumber - 2
        && numberOfPage0 != targetVisiblePosition.pageNumber - 1) {
      // we can have no any extra pages before visible one only in case, when target visible page is in the beginning of the file
      return numberOfPage0 == targetVisiblePosition.pageNumber
             && targetVisiblePosition.pageNumber == 0;
    }

    if (listSize < 2) {
      return true;
    }

    long numberOfPage1 = documentOfPagesModel.getPageByIndex(1).getPageNumber();
    if (numberOfPage1 == targetVisiblePosition.pageNumber) {
      return true;
    }
    if (numberOfPage1 != targetVisiblePosition.pageNumber - 1) {
      return false;
    }

    if (listSize < 3) {
      return true;
    }

    long numberOfPage2 = documentOfPagesModel.getPageByIndex(2).getPageNumber();
    return numberOfPage2 == targetVisiblePosition.pageNumber;
  }

  void dispose() {
    if (editor != null) {
      EditorFactory.getInstance().releaseEditor(editor);
    }
    myPageReaderExecutor.shutdown();
  }

  private static Editor createSpecialEditor(Document document, Project project) {
    // TODO: 2019-05-21 use line below to activate editor, when editing opportunity will be available
    //Editor editor = EditorFactory.getInstance().createEditor(document, project, EditorKind.MAIN_EDITOR);
    Editor editor = EditorFactory.getInstance().createViewer(document, project, EditorKind.MAIN_EDITOR);

    editor.getSettings().setLineMarkerAreaShown(false);
    editor.getSettings().setLineNumbersShown(false);
    editor.getSettings().setFoldingOutlineShown(false);
    editor.getContentComponent().setBorder(JBUI.Borders.emptyLeft(EDITOR_LINE_BEGINNING_INDENT));

    if (editor instanceof EditorEx) {
      ((EditorEx)editor).setContextMenuGroupId(IdeActions.GROUP_EDITOR_POPUP);
    }
    else {
      LOG.warn("[Large File Editor Subsystem] com.intellij.largeFilesEditor.editor.EditorModel.createSpecialEditor:"
               + " 'editor' is not instance of EditorEx. Can't set proper context menu group id.");
    }

    // don't show PositionPanel for this editor, because it still can't work properly with the editor
    editor.putUserData(PositionPanel.DISABLE_FOR_EDITOR, new Object());

    return editor;
  }

  public void fireLocalScrollBarValueChanged() {
    if (editor.isDisposed()) return;  // to avoid DisposalException: Editor is already disposed

    if (isLocalScrollBarStabilized) {
      reflectLocalScrollBarStateToTargetPosition();
    }
    requestUpdate();
  }

  private void reflectLocalScrollBarStateToTargetPosition() {
    if (documentOfPagesModel.getPagesAmount() == 0) return;

    int localScrollBarValue = myLocalInvisibleScrollBar.getValue();

    int indexOfPage = 0;
    int topOfPage = 0;
    int bottomOfPage = 0;
    while (indexOfPage < documentOfPagesModel.getPagesAmount()) {
      topOfPage = bottomOfPage;
      bottomOfPage = offsetToY(getSymbolOffsetToStartOfPage(indexOfPage + 1));

      if (localScrollBarValue < bottomOfPage) {
        targetVisiblePosition.set(documentOfPagesModel.getPageByIndex(indexOfPage).getPageNumber(),
                                  localScrollBarValue - topOfPage);
        return;
      }

      indexOfPage++;
    }

    if (indexOfPage >= 1 && localScrollBarValue == bottomOfPage) {
      targetVisiblePosition.set(documentOfPagesModel.getPageByIndex(indexOfPage - 1).getPageNumber(),
                                localScrollBarValue - topOfPage);
      return;
    }

    LOG.warn("[Large File Editor Subsystem] EditorModel.reflectLocalScrollBarStateToTargetPosition():" +
             " can't reflect state." +
             " indexOfPage=" + indexOfPage + " localScrollBarValue=" + localScrollBarValue + " topOfPage=" + topOfPage
             + " bottomOfPage=" + bottomOfPage + " pagesInDocument.size()=" + documentOfPagesModel.getPagesAmount());
  }

  @CalledInAwt
  public void showSearchResult(SearchResult searchResult) {
    targetSelectionState.set(searchResult.startPosition.pageNumber, searchResult.startPosition.symbolOffsetInPage,
                             searchResult.endPostion.pageNumber, searchResult.endPostion.symbolOffsetInPage);
    targetSelectionState.isExists = true;

    targetCaretPosition.pageNumber = searchResult.endPostion.pageNumber;
    targetCaretPosition.symbolOffsetInPage = searchResult.endPostion.symbolOffsetInPage;
    isNeedToShowCaret = true;

    requestUpdate();
  }

  @CalledInAwt
  public void setHighlightingCloseSearchResultsEnabled(boolean enabled) {
    if (isNeedToHighlightCloseSearchResults != enabled) {
      isNeedToHighlightCloseSearchResults = enabled;
      fireHighlightedSearchResultsAreOutdated();
      requestUpdate();
    }
  }

  @CalledInAwt
  public void onFileChanged(Page lastPage, boolean isLengthIncreased) {
    isLocalScrollBarStabilized = false;
    pagesCash.clear();

    if (isLengthIncreased) {
      runCaretAndSelectionListeningTransparentCommand(() -> {
        documentOfPagesModel.removeLastPage(dataProvider.getProject());
      });
      isAllowedToFollowTheEndOfFile = true;
    }
    else {
      runCaretAndSelectionListeningTransparentCommand(() -> {
        documentOfPagesModel.removeAllPages(dataProvider.getProject());
      });
    }

    if (lastPage != null) {
      pagesCash.add(lastPage);
    }
    update();
  }

  @CalledInAwt
  public void onEncodingChanged() {
    isLocalScrollBarStabilized = false;
    pagesCash.clear();
    runCaretAndSelectionListeningTransparentCommand(() -> {
      documentOfPagesModel.removeAllPages(dataProvider.getProject());
    });
    requestUpdate();
  }


  interface DataProvider {

    Page getPage(long pageNumber) throws IOException;

    long getPagesAmount() throws IOException;

    Project getProject();

    void requestReadPage(long pageNumber, ReadingPageResultHandler readingPageResultHandler);

    List<SearchResult> getSearchResultsInPage(Page page);
  }
}
