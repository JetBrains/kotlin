// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.largeFilesEditor.Utils;
import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.largeFilesEditor.search.actions.*;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearch;
import com.intellij.largeFilesEditor.search.searchTask.CloseSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchManagerImpl implements SearchManager, CloseSearchTask.Callback {
  private static final int CONTEXT_ONE_SIDE_LENGTH = 100;
  private static final long STATUS_TEXT_LIFE_TIME = 3000;

  private static final Logger logger = Logger.getInstance(SearchManagerImpl.class);
  private static final long PROGRESS_STATUS_UPDATE_PERIOD = 150;

  private final LargeFileEditor myLargeFileEditor;
  private final FileDataProviderForSearch fileDataProviderForSearch;
  private final RangeSearchCreator myRangeSearchCreator;

  // TODO: 2019-05-21 need to implement using this for optimization of "close" searching
  private final JBList<SearchResult> closeSearchResultsList;

  private CloseSearchTask lastExecutedCloseSearchTask;
  private boolean notFoundState;
  private long lastProgressStatusUpdateTime = System.currentTimeMillis();

  private SearchReplaceComponent searchManageGUI;
  private FindAllAction findAllAction;
  private FindForwardBackwardAction findForwardAction;
  private FindForwardBackwardAction findBackwardAction;
  private PrevNextOccurrenceAction nextOccurrenceAction;
  private PrevNextOccurrenceAction prevOccurrenceAction;
  private ToggleAction toggleCaseSensitiveAction;
  private ToggleAction toggleWholeWordsAction;
  private StatusTextAction statusTextAction;

  private String statusText;
  private boolean isStatusTextHidden;
  private long lastTimeStatusTextWasChanged;

  public SearchManagerImpl(@NotNull LargeFileEditor largeFileEditor,
                           FileDataProviderForSearch fileDataProviderForSearch,
                           @NotNull RangeSearchCreator rangeSearchCreator) {
    this.myLargeFileEditor = largeFileEditor;
    this.fileDataProviderForSearch = fileDataProviderForSearch;
    this.myRangeSearchCreator = rangeSearchCreator;

    createActions();
    createSearchManageGUI();
    attachListenersToSearchManageGUI();

    closeSearchResultsList = createCloseSearchResultsList();

    lastExecutedCloseSearchTask = null;
    notFoundState = false;

    statusText = "";
    isStatusTextHidden = true;
    lastTimeStatusTextWasChanged = System.currentTimeMillis();
  }

  @Override
  public SearchReplaceComponent getSearchManageGUI() {
    return searchManageGUI;
  }

  @Override
  public CloseSearchTask getLastExecutedCloseSearchTask() {
    return lastExecutedCloseSearchTask;
  }

  @Override
  public void onSearchActionHandlerExecuted() {
    myLargeFileEditor.getEditor().setHeaderComponent(searchManageGUI);
    searchManageGUI.requestFocusInTheSearchFieldAndSelectContent(myLargeFileEditor.getProject());
    searchManageGUI.getSearchTextComponent().selectAll();
  }

  @NotNull
  @Override
  public LargeFileEditor getLargeFileEditor() {
    return myLargeFileEditor;
  }

  @Override
  public void launchNewRangeSearch(long fromPageNumber, long toPageNumber, boolean forwardDirection) {
    SearchTaskOptions options = new SearchTaskOptions()
      .setStringToFind(searchManageGUI.getSearchTextComponent().getText())
      .setSearchDirectionForward(forwardDirection)
      .setSearchBounds(fromPageNumber, SearchTaskOptions.NO_LIMIT,
                       toPageNumber, SearchTaskOptions.NO_LIMIT)
      .setCaseSensetive(toggleCaseSensitiveAction.isSelected(null))
      .setWholeWords(toggleWholeWordsAction.isSelected(null))
      .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

    launchNewRangeSearch(options);
  }

  private void launchNewRangeSearch(SearchTaskOptions searchTaskOptions) {
    RangeSearch rangeSearch = myRangeSearchCreator.createContent(
      myLargeFileEditor.getProject(), myLargeFileEditor.getVirtualFile(),
      myLargeFileEditor.getVirtualFile().getName());
    rangeSearch.runNewSearch(searchTaskOptions, fileDataProviderForSearch);
  }

  @Override
  public void gotoNextOccurrence(boolean directionForward) {
    int gotoSearchResultIndex = getNextOccurrenceIndexIfCan(directionForward,
                                                            myLargeFileEditor.getCaretPageNumber(),
                                                            myLargeFileEditor.getCaretPageOffset(),
                                                            closeSearchResultsList);

    if (gotoSearchResultIndex == -1) {

      boolean launchedLoopedCloseSearch = false;

      SearchTaskOptions normalCloseSearchOptions = generateOptionsForNormalCloseSearch(directionForward);

      if (notFoundState) {
        notFoundState = false;
        launchedLoopedCloseSearch = launchLoopedCloseSearchTaskIfNeeded(normalCloseSearchOptions);
      }

      if (!launchedLoopedCloseSearch) {
        launchCloseSearch(normalCloseSearchOptions);
      }
    }
    else {
      closeSearchResultsList.setSelectedIndex(gotoSearchResultIndex);
      setNewStatusText("");
    }
  }

  @CalledInAwt
  private void launchCloseSearch(SearchTaskOptions options) {
    if (StringUtil.isEmpty(options.stringToFind)) {
      return;
    }

    stopSearchTaskIfItExists();
    lastExecutedCloseSearchTask = new CloseSearchTask(
      options, myLargeFileEditor.getProject(), fileDataProviderForSearch, this);
    ApplicationManager.getApplication().executeOnPooledThread(lastExecutedCloseSearchTask);
  }

  private boolean launchLoopedCloseSearchTaskIfNeeded(SearchTaskOptions normalCloseSearchOptions) {
    if (lastExecutedCloseSearchTask == null || !lastExecutedCloseSearchTask.isFinished()) {
      return false;
    }

    SearchTaskOptions oldOptions = lastExecutedCloseSearchTask.getOptions();
    if (oldOptions.loopedPhase) {
      return false;
    }
    if (!normalCloseSearchOptions.stringToFind.equals(oldOptions.stringToFind)
        || normalCloseSearchOptions.wholeWords != oldOptions.wholeWords
        || normalCloseSearchOptions.caseSensitive != oldOptions.caseSensitive
        || normalCloseSearchOptions.searchForwardDirection != oldOptions.searchForwardDirection
        || normalCloseSearchOptions.leftBoundPageNumber != oldOptions.leftBoundPageNumber
        || normalCloseSearchOptions.leftBoundCaretPageOffset != oldOptions.leftBoundCaretPageOffset
        || normalCloseSearchOptions.rightBoundPageNumber != oldOptions.rightBoundPageNumber
        || normalCloseSearchOptions.rightBoundCaretPageOffset != oldOptions.rightBoundCaretPageOffset) {
      return false;
    }

    SearchTaskOptions loopedOptions;
    try {
      loopedOptions = normalCloseSearchOptions.clone();
    }
    catch (CloneNotSupportedException e) {
      logger.warn(e);
      Messages.showWarningDialog("Error while searching.", "Search Error");
      return false;
    }
    loopedOptions.loopedPhase = true;
    if (loopedOptions.searchForwardDirection) {
      loopedOptions.setSearchBounds(
        0, SearchTaskOptions.NO_LIMIT,
        normalCloseSearchOptions.leftBoundPageNumber, normalCloseSearchOptions.leftBoundCaretPageOffset);
    }
    else {
      loopedOptions.setSearchBounds(
        normalCloseSearchOptions.rightBoundPageNumber, normalCloseSearchOptions.rightBoundCaretPageOffset,
        SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
    }

    launchCloseSearch(loopedOptions);

    return true;
  }

  private SearchTaskOptions generateOptionsForNormalCloseSearch(boolean directionForward) {
    SearchTaskOptions options = new SearchTaskOptions()
      .setSearchDirectionForward(directionForward)
      .setStringToFind(searchManageGUI.getSearchTextComponent().getText())
      .setCaseSensetive(toggleCaseSensitiveAction.isSelected(null))
      .setWholeWords(toggleWholeWordsAction.isSelected(null))
      .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

    if (!closeSearchResultsList.isEmpty() && closeSearchResultsList.getSelectedIndex() != -1) {
      Position position = closeSearchResultsList.getSelectedValue().startPosition;
      if (directionForward) {
        options.setSearchBounds(
          //position.pageNumber, position.symbolOffsetInPage + 1,
          position.pageNumber + 1, 0,
          SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
      }
      else {
        options.setSearchBounds(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT,
                                position.pageNumber, position.symbolOffsetInPage);
      }
    }
    else {
      long caretPageNumber = myLargeFileEditor.getCaretPageNumber();
      int caretPageOffset = myLargeFileEditor.getCaretPageOffset();
      if (directionForward) {
        options.setSearchBounds(caretPageNumber, caretPageOffset,
                                SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
      }
      else {
        options.setSearchBounds(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT,
                                caretPageNumber, caretPageOffset);
      }
    }

    return options;
  }

  @Override
  public void tellSearchProgress(CloseSearchTask caller, long curPageNumber, long pagesAmount) {
    long time = System.currentTimeMillis();
    if (time - lastProgressStatusUpdateTime > PROGRESS_STATUS_UPDATE_PERIOD
        || curPageNumber == 0
        || curPageNumber == pagesAmount - 1) {
      lastProgressStatusUpdateTime = time;
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!caller.isShouldStop()) {
          setNewStatusText("Searching at " + Utils.calculatePagePositionPercent(curPageNumber, pagesAmount) + "% of file ...");
        }
      });
    }
  }

  @Override
  public void tellClosestResultFound(CloseSearchTask caller, ArrayList<SearchResult> allMatchesAtFrame,
                                     int indexOfClosestResult) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!caller.isShouldStop()) {
        setNewStatusText("");
        SearchResult closestResult = allMatchesAtFrame.get(indexOfClosestResult);
        myLargeFileEditor.getEditorModel().showSearchResult(closestResult);
        myLargeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(true);
      }
    });
  }

  @Override
  public void tellSearchIsFinished(CloseSearchTask caller, long lastScannedPageNumber) {
    ApplicationManager.getApplication().invokeLater(() -> {

      SearchTaskOptions options = caller.getOptions();
      if (!caller.isShouldStop()) {
        if (options.loopedPhase) {
          setNewStatusText("Search complete. No more matches.");
          searchManageGUI.setNotFoundBackground();
          if (!(myLargeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent)) {
            String message = "\"" + options.stringToFind + "\" not found";
            showSimpleHintInEditor(message, myLargeFileEditor.getEditor());
          }
        }
        else {
          notFoundState = true;
          AnAction action = ActionManager.getInstance().getAction(
            options.searchForwardDirection ? IdeActions.ACTION_FIND_NEXT : IdeActions.ACTION_FIND_PREVIOUS);
          String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
          String findAgainFromText = options.searchForwardDirection ? "start" : "end";
          String message;
          setNewStatusText("");
          if (!shortcutsText.isEmpty()) {
            message = String.format("\"%s\" not found, press %s to search from the %s",
                                    options.stringToFind, shortcutsText, findAgainFromText);
          }
          else {
            message = String.format("\"%s\" not found, perform \"%s\" action again to search from the %s",
                                    options.stringToFind, action.getTemplatePresentation().getText(), findAgainFromText);
          }
          showSimpleHintInEditor(message, myLargeFileEditor.getEditor());
        }
      }
    });
  }

  private static void showSimpleHintInEditor(String message, Editor editor) {
    JComponent hintComponent = HintUtil.createInformationLabel(message);
    final LightweightHint hint = new LightweightHint(hintComponent);
    HintManagerImpl.getInstanceImpl().showEditorHint(hint,
                                                     editor,
                                                     HintManager.UNDER,
                                                     HintManager.HIDE_BY_ANY_KEY |
                                                     HintManager.HIDE_BY_TEXT_CHANGE |
                                                     HintManager.HIDE_BY_SCROLLING,
                                                     0, false);
  }

  @Override
  public void tellSearchWasStopped(CloseSearchTask caller, long curPageNumber) {
  }

  @Override
  public void tellSearchWasCatchedException(CloseSearchTask caller, IOException e) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!caller.isShouldStop()) {
        setNewStatusText("Search stopped because something went wrong.");
      }
    });
  }

  @Override
  public void onEscapePressed() {
    if (lastExecutedCloseSearchTask != null
        && !lastExecutedCloseSearchTask.isShouldStop()
        && !lastExecutedCloseSearchTask.isFinished()) {
      stopSearchTaskIfItExists();
      if (lastExecutedCloseSearchTask != null) {
        setNewStatusText("Stopped by user.");
      }
    }
    else {
      stopSearchTaskIfItExists();
      IdeFocusManager
        .getInstance(myLargeFileEditor.getProject())
        .requestFocus(myLargeFileEditor.getEditor().getContentComponent(), false);
      myLargeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);
      if (myLargeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent) {
        myLargeFileEditor.getEditor().setHeaderComponent(null);
      }
    }
  }

  @Override
  public String getStatusText() {
    return statusText;
  }

  @Override
  public void updateStatusText() {
    if (isStatusTextHidden) {
      return;
    }

    if (System.currentTimeMillis() - lastTimeStatusTextWasChanged > STATUS_TEXT_LIFE_TIME) {
      statusText = "";
      isStatusTextHidden = true;
    }
  }

  @Override
  public void updateSearchManageGUIActions() {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      searchManageGUI.updateActions();
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> searchManageGUI.updateActions());
    }
  }


  @CalledInAwt
  @Override
  public void onSearchParametersChanged() {
    if (lastExecutedCloseSearchTask != null) {
      lastExecutedCloseSearchTask.shouldStop();
      setNewStatusText("");
    }
    searchManageGUI.setRegularBackground();
    myLargeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);

    String stringToFind = searchManageGUI.getSearchTextComponent().getText();
    boolean isMultiline = stringToFind.contains("\n");
    searchManageGUI.update(stringToFind, "", false, isMultiline);
  }

  @Override
  public void onCaretPositionChanged(CaretEvent e) {
    if (closeSearchResultsList.getSelectedIndex() != -1
        && e.getEditor().getCaretModel().getOffset() != closeSearchResultsList.getSelectedValue().startPosition.symbolOffsetInPage
        && e.getEditor().getCaretModel().getOffset() != 0) {
      closeSearchResultsList.clearSelection();
    }
  }

  @Override
  public void dispose() {
    stopSearchTaskIfItExists();
  }

  @Override
  public List<TextRange> getAllSearchResultsInDocument(Document document) {
    SearchTaskOptions options = generateOptionsForNormalCloseSearch(true); // these parameters will be ignored
    if (StringUtil.isEmpty(options.stringToFind)) {
      return null;
    }

    FindModel findModel = options.generateFindModel();
    String documentText = document.getText();

    int offset = 0;
    ArrayList<TextRange> resultsList = new ArrayList<>();

    while (true) {
      FindResult findResult = FindManager.getInstance(myLargeFileEditor.getProject()).findString(documentText, offset, findModel);
      if (findResult.isStringFound()) {
        resultsList.add(findResult);
        offset = findResult.getEndOffset();
      }
      else {
        return resultsList;
      }
    }
  }

  @Override
  public boolean isSearchWorkingNow() {
    return (lastExecutedCloseSearchTask != null && !lastExecutedCloseSearchTask.isFinished());
  }

  private void createActions() {
    nextOccurrenceAction = new PrevNextOccurrenceAction(this, true);
    prevOccurrenceAction = new PrevNextOccurrenceAction(this, false);
    findAllAction = new FindAllAction(this);
    findForwardAction = new FindForwardBackwardAction(true, this);
    findBackwardAction = new FindForwardBackwardAction(false, this);
    toggleCaseSensitiveAction = new ToggleAction(this, "Match &Case");
    toggleWholeWordsAction = new ToggleAction(this, "W&ords");
    statusTextAction = new StatusTextAction(this);
  }

  private void createSearchManageGUI() {
    searchManageGUI = SearchReplaceComponent
      .buildFor(myLargeFileEditor.getProject(),
                myLargeFileEditor.getEditor().getContentComponent())
      .addPrimarySearchActions(prevOccurrenceAction,
                               nextOccurrenceAction,
                               new Separator(),
                               findAllAction,
                               findBackwardAction,
                               findForwardAction)
      .addExtraSearchActions(toggleCaseSensitiveAction,
                             toggleWholeWordsAction,
                             statusTextAction)
      //.addSearchFieldActions(new RestorePreviousSettingsAction())
      .withCloseAction(this::onEscapePressed)
      .build();
  }

  private void attachListenersToSearchManageGUI() {
    searchManageGUI.addListener(new SearchReplaceComponent.Listener() {
      @Override
      public void searchFieldDocumentChanged() {
        onSearchParametersChanged();
      }

      @Override
      public void replaceFieldDocumentChanged() {
      }

      @Override
      public void multilineStateChanged() {
      }
    });
  }

  private JBList<SearchResult> createCloseSearchResultsList() {
    CollectionListModel<SearchResult> model = new CollectionListModel<>();
    JBList<SearchResult> list = new JBList<>(model);
    list.addListSelectionListener(new CloseSearchResultsListSelectionListener(list));
    return list;
  }

  private void stopSearchTaskIfItExists() {
    if (lastExecutedCloseSearchTask != null) {
      lastExecutedCloseSearchTask.shouldStop();
    }
  }

  private void setNewStatusText(String newStatusText) {
    statusText = newStatusText;
    lastTimeStatusTextWasChanged = System.currentTimeMillis();

    isStatusTextHidden = StringUtil.isEmpty(newStatusText);

    updateSearchManageGUIActions();
  }

  private static int getNextOccurrenceIndexIfCan(boolean directionForward,
                                                 long currentPageNumber,
                                                 int caretPageOffset,
                                                 JBList<SearchResult> listResult) {
    ListModel<SearchResult> model = listResult.getModel();
    int index;
    SearchResult searchResult;

    if (model.getSize() == -1) {
      return -1;
    }

    if (listResult.getSelectedIndex() != -1) {

      index = listResult.getSelectedIndex();
      if (directionForward) {
        index++;
      }
      else {
        index--;
      }
    }
    else {

      index = 0;
      while (true) {
        if (index >= model.getSize()) {
          if (directionForward) {
            return -1;
          }
          else {
            return model.getSize() - 1;
          }
        }
        else {
          searchResult = model.getElementAt(index);
          if (currentPageNumber > searchResult.startPosition.pageNumber
              || currentPageNumber == searchResult.startPosition.pageNumber
                 && caretPageOffset >= searchResult.startPosition.symbolOffsetInPage) {
            index++;
          }
          else {
            break;
          }
        }
      }

      if (!directionForward) {
        index--;
      }
    }

    if (index < 0 || index >= model.getSize()) {
      return -1;
    }
    else {
      return index;
    }
  }


  private class CloseSearchResultsListSelectionListener implements ListSelectionListener {
    private final JBList<SearchResult> list;

    CloseSearchResultsListSelectionListener(JBList<SearchResult> list) {
      this.list = list;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
      if (!e.getValueIsAdjusting()) { // it happens when the selecting process is over and the selected position is set finaly
        SearchResult selectedSearchResult = list.getSelectedValue();
        if (selectedSearchResult != null) {
          myLargeFileEditor.showSearchResult(selectedSearchResult);
        }
      }
    }
  }
}
