// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchResultsPanel;

import com.intellij.CommonBundle;
import com.intellij.largeFilesEditor.GuiUtils;
import com.intellij.largeFilesEditor.Utils;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.actions.FindFurtherAction;
import com.intellij.largeFilesEditor.search.actions.StopRangeSearchAction;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.RangeSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SingleSelectionModel;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RangeSearch implements RangeSearchTask.Callback {

  public static final Key<RangeSearch> KEY = new Key<>("lfe.searchResultsToolWindow");

  private static final Logger logger = Logger.getInstance(RangeSearch.class);
  private static final String ACTION_TOOLBAR_PLACE_ID = "lfe.searchResultsToolWindow.actionToolbar";
  private static final long UNDEFINED = -1;
  private static final int SCHEDULED_UPDATE_DELAY = 150;

  private final Project myProject;
  private final VirtualFile myVirtualFile;
  private final RangeSearchCallback myRangeSearchCallback;

  private final AtomicBoolean isScheduledUpdateCalled = new AtomicBoolean(false);

  private Content myContent = null;
  private final JComponent myComponent;
  private long myLeftBorderPageNumber = UNDEFINED;
  private long myRightBorderPageNumber = UNDEFINED;
  private final CollectionListModel<SearchResult> myResultsListModel;
  private final ShowingListModel myShowingListModel;

  private final JBList<ListElementWrapper> myShowingResultsList;
  private final SimpleColoredComponent lblSearchStatusLeft;
  private final SimpleColoredComponent lblSearchStatusCenter;
  private final AnimatedProgressIcon progressIcon;
  private final ActionToolbar myActionToolbar;

  private RangeSearchTask lastExecutedRangeSearchTask;
  private boolean inBackground;

  private final List<EdtRangeSearchEventsListener> myEdtRangeSearchEventsListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public boolean isButtonFindFurtherEnabled(boolean directionForward) {
    if (directionForward) {
      return myShowingListModel.btnSearchForwardWrapper.isEnabled;
    }
    else {
      return myShowingListModel.btnSearchBackwardWrapper.isEnabled;
    }
  }

  public void onClickSearchFurther(boolean directionForward) {
    int listSize = myShowingResultsList.getItemsCount();
    if (listSize > 0) {
      int indexToSelect = directionForward ? listSize - 1 : 0;
      myShowingResultsList.setSelectedIndex(indexToSelect);
      myShowingResultsList.ensureIndexIsVisible(indexToSelect);
    }
    launchSearchingFurther(directionForward);
  }

  public RangeSearch(@NotNull VirtualFile virtualFile,
                     @NotNull Project project,
                     @NotNull RangeSearchCallback rangeSearchCallback) {
    myVirtualFile = virtualFile;
    myProject = project;
    myRangeSearchCallback = rangeSearchCallback;

    lblSearchStatusLeft = new SimpleColoredComponent();
    lblSearchStatusLeft.setBorder(JBUI.Borders.emptyLeft(5));
    lblSearchStatusCenter = new SimpleColoredComponent();

    progressIcon = new AnimatedProgressIcon();

    myResultsListModel = new CollectionListModel<>();
    myShowingListModel = new ShowingListModel(myResultsListModel);

    myShowingResultsList = new JBList<>(myShowingListModel);
    myShowingResultsList.setSelectionModel(new SingleSelectionModel());
    myShowingResultsList.addMouseListener(new MouseInputAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          int index = myShowingResultsList.locationToIndex(e.getPoint());
          if (index != -1) {
            ListElementWrapper element = myShowingResultsList.getModel().getElementAt(index);
            element.onSelected();
          }
        }
      }
    });
    myShowingResultsList.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          ListElementWrapper element = myShowingResultsList.getSelectedValue();
          if (element != null) {
            element.onSelected();
          }
        }
      }
    });
    myShowingResultsList.setCellRenderer(new ColoredListCellRenderer<ListElementWrapper>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList<? extends ListElementWrapper> list,
                                           ListElementWrapper value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        value.render(this, selected, hasFocus);
      }
    });

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new FindFurtherAction(false, this));
    actionGroup.add(new FindFurtherAction(true, this));
    actionGroup.add(new Separator());
    actionGroup.add(new StopRangeSearchAction(this));
    myActionToolbar = ActionManager.getInstance().createActionToolbar(
      ACTION_TOOLBAR_PLACE_ID, actionGroup, false);

    myComponent = new JPanel();

    JPanel panelHeader = new JPanel();
    FlowLayout panelHeaderFlowLayout = new FlowLayout();
    panelHeaderFlowLayout.setAlignment(FlowLayout.LEFT);
    panelHeaderFlowLayout.setHgap(0);
    panelHeader.setLayout(panelHeaderFlowLayout);
    panelHeader.add(lblSearchStatusLeft);
    panelHeader.add(lblSearchStatusCenter);
    panelHeader.add(progressIcon);

    myActionToolbar.setTargetComponent(myComponent);

    JPanel panelResultsList = new JPanel();
    JBScrollPane resultsListScrollPane = new JBScrollPane();
    resultsListScrollPane.setViewportView(myShowingResultsList);
    panelResultsList.setLayout(new BorderLayout());
    panelResultsList.add(resultsListScrollPane, BorderLayout.CENTER);

    myComponent.setLayout(new BorderLayout());
    myComponent.add(panelHeader, BorderLayout.NORTH);
    myComponent.add(myActionToolbar.getComponent(), BorderLayout.WEST);
    myComponent.add(panelResultsList, BorderLayout.CENTER);

    UIUtil.removeScrollBorder(resultsListScrollPane);
    GuiUtils.setStandardSizeForPanel(panelHeader, true);
    GuiUtils.setStandardLineBorderToPanel(panelHeader, 0, 0, 1, 0);
    GuiUtils.setStandardLineBorderToPanel(panelResultsList, 0, 1, 0, 0);
  }

  public void setContent(Content content) {
    this.myContent = content;
  }

  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  public Project getProject() {
    return myProject;
  }

  public int getAmountOfStoredSearchResults() {
    return myResultsListModel.getSize();
  }

  public void setLeftBorderPageNumber(long leftBorderPageNumber) {
    myLeftBorderPageNumber = leftBorderPageNumber;
    callScheduledUpdate();
  }

  public void setRightBorderPageNumber(long rightBorderPageNumber) {
    myRightBorderPageNumber = rightBorderPageNumber;
    callScheduledUpdate();
  }

  public void setAdditionalStatusText(@Nullable String statusText) {
    lblSearchStatusCenter.clear();
    if (statusText != null) {
      lblSearchStatusCenter.append(statusText);
    }
    callScheduledUpdate();
  }

  public void addSearchResultsIntoBeginning(List<SearchResult> searchResults) {
    if (searchResults == null || searchResults.isEmpty()) {
      return;
    }

    int oldSelectedIndex = myShowingResultsList.getSelectedIndex();
    ListElementWrapper oldSelectedValue = myShowingResultsList.getSelectedValue();

    myResultsListModel.addAll(0, searchResults);

    if (myShowingResultsList.getSelectedValue() != oldSelectedValue) {
      for (int i = -1; i <= 1; i++) {
        int probablySelectedIndex = oldSelectedIndex + searchResults.size() + i;
        if (myShowingListModel.getElementAt(probablySelectedIndex) == oldSelectedValue) {
          myShowingResultsList.setSelectedIndex(probablySelectedIndex);
          break;
        }
      }
    }

    // compensating for shift of viewport
    Rectangle cellBounds = myShowingResultsList.getCellBounds(0, 0);
    int cellHeight = cellBounds.height;
    int additionalHeight = cellHeight * searchResults.size();
    Rectangle visibleRect = myShowingResultsList.getVisibleRect();
    visibleRect.y += additionalHeight;
    myShowingResultsList.scrollRectToVisible(visibleRect);
  }

  public void addSearchResultsIntoEnd(List<SearchResult> searchResults) {
    if (searchResults == null || searchResults.isEmpty()) {
      return;
    }

    int oldSelectedIndex = myShowingResultsList.getSelectedIndex();

    myResultsListModel.add(searchResults);

    if (myShowingResultsList.getSelectedIndex() != oldSelectedIndex) {
      myShowingResultsList.setSelectedIndex(oldSelectedIndex);
    }
  }

  public void updateTabName() {
    if (myContent != null && lastExecutedRangeSearchTask != null) {
      String name = EditorBundle
        .message("large.file.editor.tab.name.some.string.in.some.file", lastExecutedRangeSearchTask.getOptions().stringToFind,
                 myVirtualFile.getName());
      myContent.setDisplayName(name);
      myContent.setDescription(name);
    }
  }

  public void callScheduledUpdate() {
    if (isScheduledUpdateCalled.compareAndSet(false, true)) {
      EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
        isScheduledUpdateCalled.set(false);
        updateInEdt();
      }, SCHEDULED_UPDATE_DELAY, TimeUnit.MILLISECONDS);
    }
  }

  @CalledInAwt
  private void updateInEdt() {
    try {
      FileDataProviderForSearch fileDataProviderForSearch
        = myRangeSearchCallback.getFileDataProviderForSearch(false, myProject, myVirtualFile);

      if (fileDataProviderForSearch != null) {
        myShowingListModel.setSearchFurtherBtnsEnabled(
          false, canFindFurtherBackward());
        myShowingListModel.setSearchFurtherBtnsEnabled(
          true, canFindFurtherForward(fileDataProviderForSearch));
      }
      else {
        myShowingListModel.setSearchFurtherBtnsEnabled(false, false);
        myShowingListModel.setSearchFurtherBtnsEnabled(true, false);
      }
    }
    catch (IOException e) {
      logger.info(e);
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.working.with.file.error"),
                                 CommonBundle.getErrorTitle());
    }

    updateTabName();
    updateStatusStringInfo();
    myActionToolbar.updateActionsImmediately();
    SwingUtilities.updateComponentTreeUI(myShowingResultsList);
    progressIcon.update();
  }

  private boolean canFindFurtherBackward() {
    return myLeftBorderPageNumber != UNDEFINED && myLeftBorderPageNumber > 0;
  }

  private boolean canFindFurtherForward(FileDataProviderForSearch fileDataProviderForSearch) throws IOException {
    long pagesAmount = fileDataProviderForSearch.getPagesAmount();
    return myRightBorderPageNumber != UNDEFINED && myRightBorderPageNumber < pagesAmount - 1;
  }

  private void launchSearchingFurther(boolean directionForward) {
    FileDataProviderForSearch fileDataProviderForSearch =
      myRangeSearchCallback.getFileDataProviderForSearch(true, myProject, myVirtualFile);

    if (fileDataProviderForSearch == null) {
      logger.warn("Can't open Large File Editor for target file.");
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.cant.open.large.file.editor.for.target.file"),
                                 CommonBundle.getErrorTitle());
      return;
    }

    SearchTaskOptions newOptions;
    try {
      newOptions = lastExecutedRangeSearchTask.getOptions().clone();
    }
    catch (CloneNotSupportedException e) {
      logger.warn(e);
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.cant.launch.searching.because.of.unexpected.error"),
                                 CommonBundle.getErrorTitle());
      return;
    }

    newOptions.setCriticalAmountOfSearchResults(
      myResultsListModel.getSize() + SearchTaskOptions.DEFAULT_CRITICAL_AMOUNT_OF_SEARCH_RESULTS);

    if (directionForward) {
      newOptions
        .setSearchDirectionForward(true)
        .setSearchBounds(myRightBorderPageNumber + 1, SearchTaskOptions.NO_LIMIT,
                         SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
    }
    else {
      newOptions
        .setSearchDirectionForward(false)
        .setSearchBounds(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT,
                         myLeftBorderPageNumber - 1, SearchTaskOptions.NO_LIMIT);
    }

    runSearchTask(newOptions, fileDataProviderForSearch);
  }

  private void updateStatusStringInfo() {
    lblSearchStatusLeft.clear();

    long pagesAmount = -1;
    FileDataProviderForSearch fileDataProviderForSearch =
      myRangeSearchCallback.getFileDataProviderForSearch(false, myProject, myVirtualFile);
    if (fileDataProviderForSearch != null) {
      try {
        pagesAmount = fileDataProviderForSearch.getPagesAmount();
      }
      catch (IOException e) {
        logger.warn(e);
      }
    }

    String newStatus;
    if (pagesAmount == -1 || myRightBorderPageNumber == UNDEFINED || myLeftBorderPageNumber == UNDEFINED) {
      newStatus = EditorBundle.message("large.file.editor.message.found.some.matches", getAmountOfStoredSearchResults());
    }
    else {
      if (myLeftBorderPageNumber == 0 && myRightBorderPageNumber == pagesAmount - 1) {
        newStatus = EditorBundle.message("large.file.editor.message.found.some.matches.in.the.whole.file", getAmountOfStoredSearchResults());
      }
      else {
        newStatus = EditorBundle
          .message("large.file.editor.message.found.some.matches.in.some.bounds.of.file", getAmountOfStoredSearchResults(),
                   Utils.calculatePagePositionPercent(myLeftBorderPageNumber, pagesAmount),
                   Utils.calculatePagePositionPercent(myRightBorderPageNumber, pagesAmount));
      }
    }
    lblSearchStatusLeft.append(newStatus);
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public void runNewSearch(SearchTaskOptions options, FileDataProviderForSearch fileDataProviderForSearch) {
    runNewSearch(options, fileDataProviderForSearch, true);
  }

  public void runNewSearch(SearchTaskOptions options, FileDataProviderForSearch fileDataProviderForSearch, boolean inBackground) {
    this.inBackground = inBackground;

    long pagesAmount;
    try {
      pagesAmount = fileDataProviderForSearch.getPagesAmount();
    }
    catch (IOException e) {
      logger.warn(e);
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.working.with.file.error"),
                                 CommonBundle.getErrorTitle());
      return;
    }

    long initialBorderPageNumber;
    if (options.searchForwardDirection) {
      initialBorderPageNumber = options.leftBoundPageNumber == SearchTaskOptions.NO_LIMIT ?
                                0 : options.leftBoundPageNumber;
    }
    else {
      initialBorderPageNumber = options.rightBoundPageNumber == SearchTaskOptions.NO_LIMIT ?
                                pagesAmount - 1 : options.rightBoundPageNumber;
    }

    setLeftBorderPageNumber(initialBorderPageNumber);
    setRightBorderPageNumber(initialBorderPageNumber);

    runSearchTask(options, fileDataProviderForSearch);
  }

  private void runSearchTask(SearchTaskOptions searchTaskOptions, FileDataProviderForSearch fileDataProviderForSearch) {
    final RangeSearchTask newRangeSearchTask = new RangeSearchTask(
      searchTaskOptions, myProject, fileDataProviderForSearch, this);
    lastExecutedRangeSearchTask = newRangeSearchTask;
    if (inBackground) {
      String title = newRangeSearchTask.getTitleForBackgroundableTask();
      Task.Backgroundable task = new Task.Backgroundable(null, title, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          newRangeSearchTask.setProgressIndicator(indicator);
          newRangeSearchTask.run();
        }
      };
      ProgressManager.getInstance().run(task);
      setAdditionalStatusText(null);
    }
    else {
      newRangeSearchTask.run();
    }
  }

  @Override
  public void tellSearchIsFinished(RangeSearchTask caller, long lastScannedPageNumber) {
    if (inBackground) {
      ApplicationManager.getApplication().invokeLater(() -> {
        onSearchIsFinished(caller, lastScannedPageNumber);
      });
    }
    else {
      onSearchIsFinished(caller, lastScannedPageNumber);
    }
  }

  protected void onSearchIsFinished(RangeSearchTask caller, long lastScannedPageNumber) {
    SearchTaskOptions options = caller.getOptions();
    if (!caller.isShouldStop()) {
      if (options.searchForwardDirection) {
        setRightBorderPageNumber(lastScannedPageNumber);
      }
      else {
        setLeftBorderPageNumber(lastScannedPageNumber);
      }
      setAdditionalStatusText(EditorBundle.message("large.file.editor.message.search.complete"));
    }
    callScheduledUpdate();
    fireSearchFinished();
  }

  private void fireSearchFinished() {
    for (EdtRangeSearchEventsListener listener : myEdtRangeSearchEventsListeners) {
      listener.onSearchFinished();
    }
  }

  @Override
  public void tellFrameSearchResultsFound(RangeSearchTask caller,
                                          long curPageNumber,
                                          ArrayList<SearchResult> allMatchesAtFrame) {
    if (inBackground) {
      ApplicationManager.getApplication().invokeLater(() -> {
        onFrameSearchResultsFound(caller, curPageNumber, allMatchesAtFrame);
      });
    }
    else {
      onFrameSearchResultsFound(caller, curPageNumber, allMatchesAtFrame);
    }
  }

  protected void onFrameSearchResultsFound(RangeSearchTask caller, long curPageNumber, ArrayList<SearchResult> allMatchesAtFrame) {
    if (caller != lastExecutedRangeSearchTask  // means new search task has been already launched
        || caller.isShouldStop()) {
      return;
    }

    SearchTaskOptions options = caller.getOptions();

    if (options.searchForwardDirection) {
      addSearchResultsIntoEnd(allMatchesAtFrame);
      setRightBorderPageNumber(curPageNumber);
    }
    else {
      addSearchResultsIntoBeginning(allMatchesAtFrame);
      setLeftBorderPageNumber(curPageNumber);
    }

    if (getAmountOfStoredSearchResults() > options.criticalAmountOfSearchResults) {
      stopSearchTaskIfItExists();
      setAdditionalStatusText(EditorBundle.message("large.file.editor.message.search.stopped.because.too.many.results.were.found"));
      callScheduledUpdate();
    }
  }

  @Override
  public void tellSearchIsStopped(long curPageNumber) {
    callScheduledUpdate();

    if (!myEdtRangeSearchEventsListeners.isEmpty()) {
      if (inBackground) {
        ApplicationManager.getApplication().invokeLater(() -> {
          fireSearchStopped();
        });
      }
      else {
        fireSearchStopped();
      }
    }
  }

  protected void fireSearchStopped() {
    for (EdtRangeSearchEventsListener listener : myEdtRangeSearchEventsListeners) {
      listener.onSearchStopped();
    }
  }

  @Override
  public void tellSearchCatchedException(RangeSearchTask caller, IOException e) {
    if (inBackground) {
      ApplicationManager.getApplication().invokeLater(() -> {
        onSearchCatchedException(caller, e);
      });
    }
    else {
      onSearchCatchedException(caller, e);
    }
  }

  protected void onSearchCatchedException(RangeSearchTask caller, IOException e) {
    if (!caller.isShouldStop()) {
      setAdditionalStatusText(EditorBundle.message("large.file.editor.message.search.stopped.because.something.went.wrong"));
      logger.warn(e);
    }
  }

  private void stopSearchTaskIfItExists() {
    if (lastExecutedRangeSearchTask != null) {
      lastExecutedRangeSearchTask.shouldStop();
    }
  }

  public RangeSearchTask getLastExecutedRangeSearchTask() {
    return lastExecutedRangeSearchTask;
  }

  public void dispose() {
    if (lastExecutedRangeSearchTask != null) {
      lastExecutedRangeSearchTask.shouldStop();
    }
  }

  @TestOnly
  void addEdtRangeSearchEventsListener(EdtRangeSearchEventsListener listener) {
    myEdtRangeSearchEventsListeners.add(listener);
  }

  @TestOnly
  void removeEdtRangeSearchEventsListener(EdtRangeSearchEventsListener listener) {
    myEdtRangeSearchEventsListeners.remove(listener);
  }

  public List<SearchResult> getSearchResultsList() {
    return myResultsListModel.getItems();
  }

  private interface ListElementWrapper {
    void render(ColoredListCellRenderer coloredListCellRenderer, boolean selected, boolean hasFocus);

    void onSelected();
  }

  private class SearchResultWrapper implements ListElementWrapper {
    private final SimpleTextAttributes attrForMatchers = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_SEARCH_MATCH, null);

    private final SearchResult mySearchResult;

    SearchResultWrapper(SearchResult mySearchResult) {
      this.mySearchResult = mySearchResult;
    }

    @Override
    public void render(ColoredListCellRenderer coloredListCellRenderer, boolean selected, boolean hasFocus) {
      coloredListCellRenderer.setBackground(UIUtil.getListBackground(selected, hasFocus));

      coloredListCellRenderer.append(mySearchResult.contextPrefix);
      coloredListCellRenderer.append(mySearchResult.foundString, attrForMatchers);
      coloredListCellRenderer.append(mySearchResult.contextPostfix);
    }

    @Override
    public void onSelected() {
      myRangeSearchCallback.showResultInEditor(mySearchResult, myProject, myVirtualFile);
    }
  }

  private class SearchFurtherBtnWrapper implements ListElementWrapper {
    private final SimpleTextAttributes linkText = new SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN, JBUI.CurrentTheme.Link.linkPressedColor());
    private final boolean isForwardDirection;
    private boolean isEnabled = false;

    SearchFurtherBtnWrapper(boolean isForwardDirection) {
      this.isForwardDirection = isForwardDirection;
    }

    @Override
    public void render(ColoredListCellRenderer coloredListCellRenderer, boolean selected, boolean hasFocus) {
      String text;
      if (isForwardDirection) {
        text = EditorBundle.message("large.file.editor.text.find.next.matches");
      }
      else {
        text = EditorBundle.message("large.file.editor.text.find.previous.matches");
      }

      if (selected) {
        coloredListCellRenderer.append(text);
      }
      else {
        coloredListCellRenderer.append(text, linkText);
      }

      coloredListCellRenderer.setBackground(UIUtil.getListBackground(selected, hasFocus));
    }

    @Override
    public void onSelected() {
      if (isEnabled) {
        onClickSearchFurther(isForwardDirection);
      }
      else {
        logger.warn("[Large File Editor Subsystem] SearchResultsToolWindow.SearchFurtherBtnWrapper.onSelected():"
                    + " called onSelected() on disabled element, which should be hidden.");
      }
    }

    void setEnabled(boolean enabled) {
      isEnabled = enabled;
    }
  }

  private class ShowingListModel implements ListModel<ListElementWrapper> {

    private final CollectionListModel<SearchResult> mySearchResultsListModel;
    private final SearchFurtherBtnWrapper btnSearchBackwardWrapper = new SearchFurtherBtnWrapper(false);
    private final SearchFurtherBtnWrapper btnSearchForwardWrapper = new SearchFurtherBtnWrapper(true);

    ShowingListModel(CollectionListModel<SearchResult> model) {
      mySearchResultsListModel = model;
    }

    @Override
    public int getSize() {
      int resultSize = mySearchResultsListModel.getSize();
      if (btnSearchBackwardWrapper.isEnabled) {
        resultSize += 1;
      }
      if (btnSearchForwardWrapper.isEnabled) {
        resultSize += 1;
      }
      return resultSize;
    }

    @Override
    public ListElementWrapper getElementAt(int index) {
      if (btnSearchBackwardWrapper.isEnabled) {
        if (index == 0) {
          return btnSearchBackwardWrapper;
        }
        if (index == mySearchResultsListModel.getSize() + 1) {
          return btnSearchForwardWrapper;
        }
        return new SearchResultWrapper(mySearchResultsListModel.getElementAt(index - 1));
      }
      else {
        if (index == mySearchResultsListModel.getSize()) {
          return btnSearchForwardWrapper;
        }
        return new SearchResultWrapper(mySearchResultsListModel.getElementAt(index));
      }
    }

    @Override
    public void addListDataListener(ListDataListener l) {
      mySearchResultsListModel.addListDataListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
      mySearchResultsListModel.removeListDataListener(l);
    }

    void setSearchFurtherBtnsEnabled(boolean forwardDirection, boolean enabled) {
      if (forwardDirection) {
        btnSearchForwardWrapper.setEnabled(enabled);
      }
      else {
        btnSearchBackwardWrapper.setEnabled(enabled);
      }
    }
  }

  private class AnimatedProgressIcon extends AsyncProcessIcon {

    AnimatedProgressIcon() {
      super("");
    }

    void update() {
      boolean enabled = false;
      if (lastExecutedRangeSearchTask != null
          && !lastExecutedRangeSearchTask.isFinished()
          && !lastExecutedRangeSearchTask.isShouldStop()) {
        enabled = true;
      }
      setVisible(enabled);
      if (enabled) {
        resume();
      }
      else {
        suspend();
      }
    }
  }

  interface EdtRangeSearchEventsListener {

    @CalledInAwt
    void onSearchStopped();

    @CalledInAwt
    void onSearchFinished();
  }
}