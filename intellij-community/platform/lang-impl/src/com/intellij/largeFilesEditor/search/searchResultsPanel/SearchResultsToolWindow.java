// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchResultsPanel;

import com.intellij.largeFilesEditor.GuiUtils;
import com.intellij.largeFilesEditor.Utils;
import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.largeFilesEditor.editor.EditorManagerAccessor;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.actions.FindFurtherAction;
import com.intellij.largeFilesEditor.search.actions.StopRangeSearchAction;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.largeFilesEditor.search.searchTask.RangeSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskBase;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SingleSelectionModel;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.CalledInAwt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

public class SearchResultsToolWindow extends SimpleToolWindowPanel {

  public static final Key<SearchResultsToolWindow> KEY = new Key<>("lfe.searchResultsToolWindow");

  private static final Logger logger = Logger.getInstance(SearchResultsToolWindow.class);
  private static final String ACTION_TOOLBAR_PLACE_ID = "lfe.searchResultsToolWindow.actionToolbar";
  private static final long UNDEFINED = -1;

  private final Project myProject;
  private final VirtualFile myVirtualFile;
  private final EditorManagerAccessor myEditorManagerAccessor;

  private Content myRelativeTab = null;
  private SearchTaskOptions mySearchTaskOptions = null;
  private long myLeftBorderPageNumber = UNDEFINED;
  private long myRightBorderPageNumber = UNDEFINED;
  private final CollectionListModel<SearchResult> myResultsListModel;
  private final ShowingListModel myShowingListModel;

  private final JBList<ListElementWrapper> myShowingResultsList;
  private final SimpleColoredComponent lblSearchStatusLeft;
  private final SimpleColoredComponent lblSearchStatusCenter;
  private final AnimatedProgressIcon progressIcon;
  private final ActionToolbar myActionToolbar;

  public boolean isButtonFindFurtherEnabled(boolean directionForward) {
    if (directionForward) {
      return myShowingListModel.btnSearchForwardWrapper.isEnabled;
    }
    else {
      return myShowingListModel.btnSearchBackwardWrapper.isEnabled;
    }
  }

  public void onClickSearchFurther(boolean directionForward, boolean additionMode) {
    int listSize = myShowingResultsList.getItemsCount();
    if (listSize > 0) {
      int indexToSelect = directionForward ? listSize - 1 : 0;
      myShowingResultsList.setSelectedIndex(indexToSelect);
      myShowingResultsList.ensureIndexIsVisible(indexToSelect);
    }
    launchSearchingFurther(directionForward, additionMode);
  }

  public SearchResultsToolWindow(@NotNull VirtualFile virtualFile, @NotNull Project project,
                                 @NotNull EditorManagerAccessor editorManagerAccessor) {
    super(false, true);

    myVirtualFile = virtualFile;
    myProject = project;
    myEditorManagerAccessor = editorManagerAccessor;

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

    AnAction action1 = new FindFurtherAction(false, true, this);
    //AnAction action2 = new FindFurtherAction(false, false, this);
    //AnAction action3 = new FindFurtherAction(true, false, this);
    AnAction action4 = new FindFurtherAction(true, true, this);
    AnAction action5 = new StopRangeSearchAction(this, editorManagerAccessor);

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(action1);
    //actionGroup.add(action2);
    //actionGroup.add(action3);
    actionGroup.add(action4);
    actionGroup.add(new Separator());
    actionGroup.add(action5);
    myActionToolbar = ActionManager.getInstance().createActionToolbar(
      ACTION_TOOLBAR_PLACE_ID, actionGroup, false);

    JPanel mainPanel = new JPanel();
    JPanel panelResultsList = new JPanel();
    JPanel panelHeader = new JPanel();
    JPanel actionToolbarPanel = new JPanel();

    setContent(mainPanel);

    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(panelHeader, BorderLayout.NORTH);
    mainPanel.add(actionToolbarPanel, BorderLayout.WEST);
    mainPanel.add(panelResultsList, BorderLayout.CENTER);

    FlowLayout panelHeaderFlowLayout = new FlowLayout();
    panelHeaderFlowLayout.setAlignment(FlowLayout.LEFT);
    panelHeaderFlowLayout.setHgap(0);
    panelHeader.setLayout(panelHeaderFlowLayout);
    panelHeader.add(lblSearchStatusLeft);
    panelHeader.add(lblSearchStatusCenter);
    panelHeader.add(progressIcon);

    actionToolbarPanel.setLayout(new BorderLayout());
    actionToolbarPanel.add(myActionToolbar.getComponent());

    JBScrollPane resultsListScrollPane = new JBScrollPane();
    resultsListScrollPane.setViewportView(myShowingResultsList);
    panelResultsList.setLayout(new BorderLayout());
    panelResultsList.add(resultsListScrollPane, BorderLayout.CENTER);

    GuiUtils.setStandardSizeForPanel(panelHeader, true);
    GuiUtils.setStandardSizeForPanel(actionToolbarPanel, false);
    GuiUtils.setStandardLineBorderToPanel(panelHeader, 0, 0, 1, 0);
    GuiUtils.setStandardLineBorderToPanel(actionToolbarPanel, 0, 0, 0, 1);
  }

  public void setRelativeTab(Content content) {
    this.myRelativeTab = content;
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

  public void setSearchTaskOptions(SearchTaskOptions searchTaskOptions) {
    mySearchTaskOptions = searchTaskOptions;
  }

  public void setLeftBorderPageNumber(long leftBorderPageNumber) {
    myLeftBorderPageNumber = leftBorderPageNumber;
    updateStatusStringInfo();
  }

  public void setRightBorderPageNumber(long rightBorderPageNumber) {
    myRightBorderPageNumber = rightBorderPageNumber;
    updateStatusStringInfo();
  }

  public void setAdditionalStatusText(@Nullable String statusText) {
    lblSearchStatusCenter.clear();
    if (statusText != null) {
      lblSearchStatusCenter.append(statusText);
    }
    progressIcon.update();
  }

  public void clearAllResults() {
    myResultsListModel.removeAll();
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
    if (myRelativeTab != null && mySearchTaskOptions != null && mySearchTaskOptions.stringToFind != null) {
      String name = "\"" + mySearchTaskOptions.stringToFind + "\" in " + myVirtualFile.getName();
      myRelativeTab.setDisplayName(name);
      myRelativeTab.setDescription(name);
    }
  }

  @CalledInAwt
  public void updateSearchFurtherBtns() {
    try {
      EditorManager editorManager =
        myEditorManagerAccessor.getEditorManager(false, myProject, myVirtualFile);

      if (editorManager != null) {
        myShowingListModel.setSearchFurtherBtnsEnabled(
          false, canFindFurtherBackward());
        myShowingListModel.setSearchFurtherBtnsEnabled(
          true, canFindFurtherForward(editorManager.getFileDataProviderForSearch()));
      }
      else {
        myShowingListModel.setSearchFurtherBtnsEnabled(false, false);
        myShowingListModel.setSearchFurtherBtnsEnabled(true, false);
      }
    }
    catch (IOException e) {
      logger.info(e);
      Messages.showWarningDialog("Working with file error.", "Error");
    }
    myActionToolbar.updateActionsImmediately();
    SwingUtilities.updateComponentTreeUI(myShowingResultsList);
    progressIcon.update();
  }

  public void tellWasClosed() {
    EditorManager editorManager =
      myEditorManagerAccessor.getEditorManager(false, myProject, myVirtualFile);
    if (editorManager != null) {
      editorManager.getSearchManager().tellSearchResultsToolWindowWasClosed();
    }
  }

  private boolean canFindFurtherBackward() {
    return myLeftBorderPageNumber != UNDEFINED && myLeftBorderPageNumber > 0;
  }

  private boolean canFindFurtherForward(FileDataProviderForSearch fileDataProviderForSearch) throws IOException {
    long pagesAmount = fileDataProviderForSearch.getPagesAmount();
    return myRightBorderPageNumber != UNDEFINED && myRightBorderPageNumber < pagesAmount - 1;
  }

  private void launchSearchingFurther(boolean directionForward, boolean additionMode) {
    EditorManager editorManager =
      myEditorManagerAccessor.getEditorManager(true, myProject, myVirtualFile);
    if (editorManager == null) {
      logger.warn("Can't open Large File Editor for target file.");
      Messages.showWarningDialog("Can't open Large File Editor for target file.", "Error");
      return;
    }

    SearchTaskOptions newOptions;
    try {
      newOptions = mySearchTaskOptions.clone();
    }
    catch (CloneNotSupportedException e) {
      logger.warn(e);
      Messages.showWarningDialog("Can't launch searching because of unexpected error.", "Error");
      return;
    }

    if (additionMode) {
      newOptions.setCriticalAmountOfSearchResults(
        myResultsListModel.getSize() + SearchTaskOptions.DEFAULT_CRITICAL_AMOUNT_OF_SEARCH_RESULTS);
    }
    else {
      newOptions.setCriticalAmountOfSearchResults(SearchTaskOptions.DEFAULT_CRITICAL_AMOUNT_OF_SEARCH_RESULTS);
    }

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

    editorManager.getSearchManager().launchRangeSearch(newOptions, !additionMode);
  }

  private void updateStatusStringInfo() {
    lblSearchStatusLeft.clear();

    long pagesAmount = -1;
    EditorManager editorManager = myEditorManagerAccessor.getEditorManager(false, myProject, myVirtualFile);
    if (editorManager != null) {
      try {
        pagesAmount = editorManager.getFileDataProviderForSearch().getPagesAmount();
      }
      catch (IOException e) {
        logger.warn(e);
      }
    }

    lblSearchStatusLeft.append(String.format("Found %d matches", getAmountOfStoredSearchResults()));

    if (pagesAmount == -1 || myRightBorderPageNumber == UNDEFINED || myLeftBorderPageNumber == UNDEFINED) {
      lblSearchStatusLeft.append(".");
      return;
    }

    if (myLeftBorderPageNumber == 0 && myRightBorderPageNumber == pagesAmount - 1) {
      lblSearchStatusLeft.append(" in the whole file.");
    }
    else {
      lblSearchStatusLeft.append(" in bounds ");
      lblSearchStatusLeft.append(String.valueOf(Utils.calculatePagePositionPercent(myLeftBorderPageNumber, pagesAmount)));
      lblSearchStatusLeft.append("% to ");
      lblSearchStatusLeft.append(String.valueOf(Utils.calculatePagePositionPercent(myRightBorderPageNumber, pagesAmount)));
      lblSearchStatusLeft.append("% of file.");
    }
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
      coloredListCellRenderer.append(mySearchResult.stringToFind, attrForMatchers);
      coloredListCellRenderer.append(mySearchResult.contextPostfix);
    }

    @Override
    public void onSelected() {
      EditorManager editorManager =
        myEditorManagerAccessor.getEditorManager(true, myProject, myVirtualFile);
      if (editorManager != null) {
        myEditorManagerAccessor.showEditorTab(editorManager);
        editorManager.showSearchResult(mySearchResult);
      }
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
        text = "find next matches";
      }
      else {
        text = "find previous matches";
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
        onClickSearchFurther(isForwardDirection, true);
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
      EditorManager editorManager =
        myEditorManagerAccessor.getEditorManager(false, getProject(), getVirtualFile());
      if (editorManager != null) {
        SearchManager searchManager = editorManager.getSearchManager();
        SearchTaskBase task = searchManager.getLastExecutedSearchTask();
        if (task instanceof RangeSearchTask
            && !task.isFinished()
            && !task.isShouldStop()) {
          enabled = true;
        }
      }
      if (enabled) {
        setVisible(true);
        resume();
      }
      else {
        setVisible(false);
        suspend();
      }
    }
  }
}