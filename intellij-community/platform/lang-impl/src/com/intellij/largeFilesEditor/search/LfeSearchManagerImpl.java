// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.impl.RegExHelpPopup;
import com.intellij.largeFilesEditor.Utils;
import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.largeFilesEditor.editor.Page;
import com.intellij.largeFilesEditor.search.actions.ToggleAction;
import com.intellij.largeFilesEditor.search.actions.*;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearch;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearchCallback;
import com.intellij.largeFilesEditor.search.searchTask.CloseSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.DefaultCustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LfeSearchManagerImpl implements LfeSearchManager, CloseSearchTask.Callback {
  private static final int CONTEXT_ONE_SIDE_LENGTH = 100;
  private static final long STATUS_TEXT_LIFE_TIME = 3000;

  private static final Logger LOG = Logger.getInstance(LfeSearchManagerImpl.class);
  private static final long PROGRESS_STATUS_UPDATE_PERIOD = 150;

  private final LargeFileEditor largeFileEditor;
  private final FileDataProviderForSearch fileDataProviderForSearch;
  private final RangeSearchCreator rangeSearchCreator;

  // TODO: 2019-05-21 need to implement using this for optimization of "close" searching
  private final JBList<SearchResult> myCloseSearchResultsList;

  private CloseSearchTask lastExecutedCloseSearchTask;
  private boolean notFoundState;
  private long lastProgressStatusUpdateTime = System.currentTimeMillis();

  private SearchReplaceComponent mySearchReplaceComponent;
  private FindAllAction myFindAllAction;
  private FindForwardBackwardAction myFindForwardAction;
  private FindForwardBackwardAction myFindBackwardAction;
  private PrevNextOccurrenceAction myNextOccurrenceAction;
  private PrevNextOccurrenceAction myPrevOccurrenceAction;
  private ToggleAction myToggleCaseSensitiveAction;
  private ToggleAction myToggleWholeWordsAction;
  private ToggleAction myToggleRegularExpression;
  private StatusTextAction myStatusTextAction;

  private String myStatusText;
  private boolean myIsStatusTextHidden;
  private long myLastTimeStatusTextWasChanged;

  public LfeSearchManagerImpl(@NotNull LargeFileEditor largeFileEditor,
                              FileDataProviderForSearch fileDataProviderForSearch,
                              @NotNull RangeSearchCreator rangeSearchCreator) {
    this.largeFileEditor = largeFileEditor;
    this.fileDataProviderForSearch = fileDataProviderForSearch;
    this.rangeSearchCreator = rangeSearchCreator;

    createActions();
    createSearchReplaceComponent();
    attachListenersToSearchReplaceComponent();

    myCloseSearchResultsList = createCloseSearchResultsList();

    lastExecutedCloseSearchTask = null;
    notFoundState = false;

    myStatusText = "";
    myIsStatusTextHidden = true;
    myLastTimeStatusTextWasChanged = System.currentTimeMillis();
  }

  @Override
  public SearchReplaceComponent getSearchReplaceComponent() {
    return mySearchReplaceComponent;
  }

  @Override
  public CloseSearchTask getLastExecutedCloseSearchTask() {
    return lastExecutedCloseSearchTask;
  }

  @Override
  public void onSearchActionHandlerExecuted() {
    largeFileEditor.getEditor().setHeaderComponent(mySearchReplaceComponent);
    mySearchReplaceComponent.requestFocusInTheSearchFieldAndSelectContent(largeFileEditor.getProject());
    mySearchReplaceComponent.getSearchTextComponent().selectAll();
  }

  @NotNull
  @Override
  public LargeFileEditor getLargeFileEditor() {
    return largeFileEditor;
  }

  @Override
  public void launchNewRangeSearch(long fromPageNumber, long toPageNumber, boolean forwardDirection) {
    SearchTaskOptions options = new SearchTaskOptions()
      .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
      .setSearchDirectionForward(forwardDirection)
      .setSearchBounds(fromPageNumber, SearchTaskOptions.NO_LIMIT,
                       toPageNumber, SearchTaskOptions.NO_LIMIT)
      .setCaseSensetive(myToggleCaseSensitiveAction.isSelected(null))
      .setWholeWords(myToggleWholeWordsAction.isSelected(null))
      .setRegularExpression(myToggleRegularExpression.isSelected(null))
      .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

    launchNewRangeSearch(options);
  }

  private void launchNewRangeSearch(SearchTaskOptions searchTaskOptions) {
    showRegexSearchWarningIfNeed();

    RangeSearch rangeSearch = rangeSearchCreator.createContent(
      largeFileEditor.getProject(), largeFileEditor.getFile(),
      largeFileEditor.getFile().getName());
    rangeSearch.runNewSearch(searchTaskOptions, fileDataProviderForSearch);
  }

  @Override
  public void gotoNextOccurrence(boolean directionForward) {
    int gotoSearchResultIndex = getNextOccurrenceIndexIfCan(directionForward,
                                                            largeFileEditor.getCaretPageNumber(),
                                                            largeFileEditor.getCaretPageOffset(),
                                                            myCloseSearchResultsList);

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
      myCloseSearchResultsList.setSelectedIndex(gotoSearchResultIndex);
      setNewStatusText("");
    }
  }

  @CalledInAwt
  private void launchCloseSearch(SearchTaskOptions options) {
    if (StringUtil.isEmpty(options.stringToFind)) {
      return;
    }

    stopSearchTaskIfItExists();

    showRegexSearchWarningIfNeed();

    lastExecutedCloseSearchTask = new CloseSearchTask(
      options, largeFileEditor.getProject(), fileDataProviderForSearch, this);
    ApplicationManager.getApplication().executeOnPooledThread(lastExecutedCloseSearchTask);
  }

  private void showRegexSearchWarningIfNeed() {
    EditorNotifications.getInstance(largeFileEditor.getProject()).updateNotifications(largeFileEditor.getFile());
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
        || normalCloseSearchOptions.regularExpression != oldOptions.regularExpression
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
      LOG.warn(e);
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.error.while.searching"),
                                 EditorBundle.message("large.file.editor.title.search.error"));
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
      .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
      .setCaseSensetive(myToggleCaseSensitiveAction.isSelected(null))
      .setWholeWords(myToggleWholeWordsAction.isSelected(null))
      .setRegularExpression(myToggleRegularExpression.isSelected(null))
      .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

    if (!myCloseSearchResultsList.isEmpty() && myCloseSearchResultsList.getSelectedIndex() != -1) {
      Position position = myCloseSearchResultsList.getSelectedValue().startPosition;
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
      long caretPageNumber = largeFileEditor.getCaretPageNumber();
      int caretPageOffset = largeFileEditor.getCaretPageOffset();
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
          setNewStatusText(EditorBundle.message("large.file.editor.message.searching.at.some.percent.of.file",
                                             Utils.calculatePagePositionPercent(curPageNumber, pagesAmount)));
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
        largeFileEditor.getEditorModel().showSearchResult(closestResult);
        largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(true);
      }
    });
  }

  @Override
  public void tellSearchIsFinished(CloseSearchTask caller, long lastScannedPageNumber) {
    ApplicationManager.getApplication().invokeLater(() -> {

      SearchTaskOptions options = caller.getOptions();
      if (!caller.isShouldStop()) {
        if (options.loopedPhase) {
          setNewStatusText(EditorBundle.message("large.file.editor.message.search.is.completed.and.no.more.matches"));
          mySearchReplaceComponent.setNotFoundBackground();
          if (!(largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent)) {
            String message = EditorBundle.message("large.file.editor.message.some.string.not.found", options.stringToFind);
            showSimpleHintInEditor(message, largeFileEditor.getEditor());
          }
        }
        else {
          notFoundState = true;
          AnAction action = ActionManager.getInstance().getAction(
            options.searchForwardDirection ? IdeActions.ACTION_FIND_NEXT : IdeActions.ACTION_FIND_PREVIOUS);
          String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
          String message;
          setNewStatusText("");
          message = !shortcutsText.isEmpty()
                    ? options.searchForwardDirection
                      ? EditorBundle.message("large.file.editor.some.string.not.found.press.some.shortcut.to.search.from.the.start",
                                          options.stringToFind, shortcutsText)
                      : EditorBundle.message("large.file.editor.some.string.not.found.press.some.shortcut.to.search.from.the.end",
                                          options.stringToFind, shortcutsText)
                    : options.searchForwardDirection
                      ? EditorBundle.message("large.file.editor.some.string.not.found.perform.some.action.again.to.search.from.start",
                                          options.stringToFind, action.getTemplatePresentation().getText())
                      : EditorBundle.message("large.file.editor.some.string.not.found.perform.some.action.again.to.search.from.end",
                                          options.stringToFind, action.getTemplatePresentation().getText());
          showSimpleHintInEditor(message, largeFileEditor.getEditor());
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
        setNewStatusText(EditorBundle.message("large.file.editor.message.search.stopped.because.something.went.wrong"));
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
        setNewStatusText(EditorBundle.message("large.file.editor.message.stopped.by.user"));
      }
    }
    else {
      stopSearchTaskIfItExists();
      IdeFocusManager
        .getInstance(largeFileEditor.getProject())
        .requestFocus(largeFileEditor.getEditor().getContentComponent(), false);
      largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);
      if (largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent) {
        largeFileEditor.getEditor().setHeaderComponent(null);
      }
    }
  }

  @Override
  public String getStatusText() {
    return myStatusText;
  }

  @Override
  public void updateStatusText() {
    if (myIsStatusTextHidden) {
      return;
    }

    if (System.currentTimeMillis() - myLastTimeStatusTextWasChanged > STATUS_TEXT_LIFE_TIME) {
      myStatusText = "";
      myIsStatusTextHidden = true;
    }
  }

  @Override
  public void updateSearchReplaceComponentActions() {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      mySearchReplaceComponent.updateActions();
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> mySearchReplaceComponent.updateActions());
    }
  }


  @CalledInAwt
  @Override
  public void onSearchParametersChanged() {
    if (lastExecutedCloseSearchTask != null) {
      lastExecutedCloseSearchTask.shouldStop();
      setNewStatusText("");
    }
    mySearchReplaceComponent.setRegularBackground();
    largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);

    String stringToFind = mySearchReplaceComponent.getSearchTextComponent().getText();
    boolean isMultiline = stringToFind.contains("\n");
    mySearchReplaceComponent.update(stringToFind, "", false, isMultiline);
  }

  @Override
  public void onCaretPositionChanged(CaretEvent e) {
    if (myCloseSearchResultsList.getSelectedIndex() != -1
        && e.getEditor().getCaretModel().getOffset() != myCloseSearchResultsList.getSelectedValue().startPosition.symbolOffsetInPage
        && e.getEditor().getCaretModel().getOffset() != 0) {
      myCloseSearchResultsList.clearSelection();
    }
  }

  @Override
  public void dispose() {
    stopSearchTaskIfItExists();
  }

  @Override
  public List<SearchResult> getSearchResultsInPage(Page page) {
    SearchTaskOptions options = new SearchTaskOptions()
      .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
      .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
      .setCaseSensetive(myToggleCaseSensitiveAction.isSelected(null))
      .setWholeWords(myToggleWholeWordsAction.isSelected(null))
      .setRegularExpression(myToggleRegularExpression.isSelected(null))
      .setSearchDirectionForward(true)
      .setSearchBounds(page.getPageNumber(), SearchTaskOptions.NO_LIMIT,
                       page.getPageNumber(), SearchTaskOptions.NO_LIMIT)
      .setContextOneSideLength(0);

    if (StringUtil.isEmpty(options.stringToFind)) {
      return null;
    }

    RangeSearch rangeSearch = new RangeSearch(
      getLargeFileEditor().getFile(), getLargeFileEditor().getProject(),
      new RangeSearchCallback() {
        @Override
        public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
          return fileDataProviderForSearch;
        }

        @Override
        public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) {
          // ignore
        }
      });

    rangeSearch.runNewSearch(options, fileDataProviderForSearch, false);
    return rangeSearch.getSearchResultsList();
  }

  @Override
  public boolean isSearchWorkingNow() {
    return (lastExecutedCloseSearchTask != null && !lastExecutedCloseSearchTask.isFinished());
  }

  @Override
  public boolean canShowRegexSearchWarning() {
    if (!myToggleRegularExpression.isSelected(null)) return false;

    String stringToFind = mySearchReplaceComponent.getSearchTextComponent().getText();

    // "pageSize / 10", because it's strictly shorter then even full page consisted of only 4-byte symbols and much longer then simple stringsToFind
    return stringToFind.length() > largeFileEditor.getPageSize() / 10 ||
           stringToFind.contains("*") ||
           stringToFind.contains("+") ||
           stringToFind.contains("{");
  }

  private void createActions() {
    myNextOccurrenceAction = new PrevNextOccurrenceAction(this, true);
    myPrevOccurrenceAction = new PrevNextOccurrenceAction(this, false);
    myFindAllAction = new FindAllAction(this);
    myFindForwardAction = new FindForwardBackwardAction(true, this);
    myFindBackwardAction = new FindForwardBackwardAction(false, this);
    myToggleCaseSensitiveAction = new ToggleAction(this, EditorBundle.message("large.file.editor.match.case.action.mnemonic.text"));
    myToggleWholeWordsAction = new ToggleAction(this, EditorBundle.message("large.file.editor.words.action.mnemonic.text")) {
      @Override
      public void update(@NotNull AnActionEvent e) {
        boolean enabled = myToggleRegularExpression != null && !myToggleRegularExpression.isSelected(e);
        boolean visible = mySearchReplaceComponent == null || !mySearchReplaceComponent.isMultiline();
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(visible);
        setSelected(e, isSelected(e) && enabled && visible);
        super.update(e);
      }
    };
    myToggleRegularExpression = new ToggleAction(this, EditorBundle.message("large.file.editor.regex.action.mnemonic.text")) {
      @Override
      public void setSelected(@Nullable AnActionEvent e, boolean state) {
        super.setSelected(e, state);
        if (state && myToggleWholeWordsAction != null) {
          myToggleWholeWordsAction.setSelected(e, false);
        }
      }
    };
    myStatusTextAction = new StatusTextAction(this);
  }

  private void createSearchReplaceComponent() {
    mySearchReplaceComponent = SearchReplaceComponent
      .buildFor(largeFileEditor.getProject(),
                largeFileEditor.getEditor().getContentComponent())
      .addPrimarySearchActions(myPrevOccurrenceAction,
                               myNextOccurrenceAction,
                               new Separator(),
                               myFindAllAction,
                               myFindBackwardAction,
                               myFindForwardAction)
      .addExtraSearchActions(myToggleCaseSensitiveAction,
                             myToggleWholeWordsAction,
                             myToggleRegularExpression,
                             new DefaultCustomComponentAction(
                               () -> RegExHelpPopup.createRegExLink("<html><body><b>?</b></body></html>", null, null, "FindInFile")),
                             myStatusTextAction)
      //.addSearchFieldActions(new RestorePreviousSettingsAction())
      .withCloseAction(this::onEscapePressed)
      .build();
  }

  private void attachListenersToSearchReplaceComponent() {
    mySearchReplaceComponent.addListener(new SearchReplaceComponent.Listener() {
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
    myStatusText = newStatusText;
    myLastTimeStatusTextWasChanged = System.currentTimeMillis();

    myIsStatusTextHidden = StringUtil.isEmpty(newStatusText);

    updateSearchReplaceComponentActions();
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
          largeFileEditor.showSearchResult(selectedSearchResult);
        }
      }
    }
  }
}
