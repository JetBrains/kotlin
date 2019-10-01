// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.layout.*;
import com.intellij.execution.ui.layout.actions.CloseViewAction;
import com.intellij.execution.ui.layout.actions.MinimizeViewAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.MutualMap;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tabs.JBTabPainter;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.*;
import com.intellij.ui.tabs.impl.singleRow.ScrollableSingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.SingleRowLayout;
import com.intellij.util.SmartList;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GridCellImpl implements GridCell {
  private final GridImpl myContainer;

  private final MutualMap<Content, TabInfo> myContents = new MutualMap<>(true);
  private final Set<Content> myMinimizedContents = new HashSet<>();

  private final JBTabs myTabs;
  private final GridImpl.Placeholder myPlaceholder;
  private final PlaceInGrid myPlaceInGrid;

  private final ViewContextEx myContext;
  private JBPopup myPopup;

  public GridCellImpl(ViewContextEx context, @NotNull GridImpl container, GridImpl.Placeholder placeholder, PlaceInGrid placeInGrid) {
    myContext = context;
    myContainer = container;

    myPlaceInGrid = placeInGrid;
    myPlaceholder = placeholder;
    myPlaceholder.setContentProvider(() -> getContents());
    myTabs = new GridCellTabs(context, container);
    myTabs.setDataProvider(dataId -> {
      if (ViewContext.CONTENT_KEY.is(dataId)) {
        TabInfo target = myTabs.getTargetInfo();
        if (target != null) {
          return new Content[]{getContentFor(target)};
        }
      }
      else if (ViewContext.CONTEXT_KEY.is(dataId)) {
        return myContext;
      }

      return null;
    });

    myTabs.getPresentation().setSideComponentVertical(!context.getLayoutSettings().isToolbarHorizontal())
      .setFocusCycle(false).setPaintFocus(true)
      .setTabDraggingEnabled(context.isMoveToGridActionEnabled()).setSideComponentOnTabs(false);

    myTabs.addTabMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (UIUtil.isCloseClick(e)) {
          // see RunnerContentUi tabMouseListener as well
          closeOrMinimize(e);
        }
      }
    });
    rebuildPopupGroup();
    myTabs.addListener(new TabsListener() {

      @Override
      public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
        if (oldSelection != null && myContext.isStateBeingRestored()) {
          saveUiState();
        }
      }

      @Override
      public void selectionChanged(final TabInfo oldSelection, final TabInfo newSelection) {
        updateSelection(myTabs.getComponent().isShowing());

        if (!myTabs.getComponent().isShowing()) return;

        if (newSelection != null) {
          newSelection.stopAlerting();
        }
      }
    });
  }

  public void rebuildPopupGroup() {
    myTabs.setPopupGroup(myContext.getCellPopupGroup(ViewContext.CELL_POPUP_PLACE),
                         ViewContext.CELL_POPUP_PLACE, true);
  }

  public PlaceInGrid getPlaceInGrid() {
    return myPlaceInGrid;
  }

  void add(final Content content) {
    if (myContents.containsKey(content)) return;
    myContents.put(content, null);

    revalidateCell(() -> myTabs.addTab(createTabInfoFor(content)));

    updateSelection(myTabs.getComponent().getRootPane() != null);
  }

  void remove(Content content) {
    if (!myContents.containsKey(content)) return;

    final TabInfo info = getTabFor(content);
    myContents.remove(content);

    revalidateCell(() -> myTabs.removeTab(info));

    updateSelection(myTabs.getComponent().getRootPane() != null);
  }

  private void revalidateCell(Runnable contentAction) {
    if (myContents.size() == 0) {
      myPlaceholder.removeAll();
      myTabs.removeAllTabs();

      if (myPopup != null) {
        myPopup.cancel();
        myPopup = null;
      }
    }
    else {
      if (myPlaceholder.isNull()) {
        myPlaceholder.setContent(myTabs.getComponent());
      }

      contentAction.run();
    }

    restoreProportions();

    myTabs.getComponent().revalidate();
    myTabs.getComponent().repaint();
  }

  void setHideTabs(boolean hide) {
    myTabs.getPresentation().setHideTabs(hide);
  }

  private TabInfo createTabInfoFor(Content content) {
    final TabInfo tabInfo = updatePresentation(new TabInfo(new ProviderWrapper(content, myContext)), content)
      .setObject(content)
      .setPreferredFocusableComponent(content.getPreferredFocusableComponent())
      .setActionsContextComponent(content.getActionsContextComponent());

    myContents.remove(content);
    myContents.put(content, tabInfo);

    ActionGroup group = (ActionGroup)myContext.getActionManager().getAction(RunnerContentUi.VIEW_TOOLBAR);
    tabInfo.setTabLabelActions(group, ViewContext.CELL_TOOLBAR_PLACE);
    tabInfo.setDragOutDelegate(((RunnerContentUi)myContext).myDragOutDelegate);
    return tabInfo;
  }

  @Nullable
  private static TabInfo updatePresentation(TabInfo info, Content content) {
    if (info == null) {
      return null;
    }

    return info.
      setIcon(content.getIcon()).
      setText(content.getDisplayName()).
      setTooltipText(content.getDescription()).
      setActionsContextComponent(content.getActionsContextComponent()).
      setActions(content.getActions(), content.getPlace());
  }

  public ActionCallback select(final Content content, final boolean requestFocus) {
    final TabInfo tabInfo = myContents.getValue(content);
    return tabInfo != null ? myTabs.select(tabInfo, requestFocus) : ActionCallback.DONE;
  }

  public void processAlert(final Content content, final boolean activate) {
    if (myMinimizedContents.contains(content)) {
      content.fireAlert();
    }

    TabInfo tab = getTabFor(content);
    if (tab == null) return;
    if (myTabs.getSelectedInfo() != tab) {
      if (activate) {
        tab.fireAlert();
      }
      else {
        tab.stopAlerting();
      }
    }
  }

  public void updateTabPresentation(Content content) {
    updatePresentation(myTabs.findInfo(content), content);
  }

  public boolean isMinimized(Content content) {
    return myMinimizedContents.contains(content);
  }

  public boolean contains(Component c) {
    return myTabs.getComponent().isAncestorOf(c);
  }

  private static class ProviderWrapper extends NonOpaquePanel implements DataProvider {
    Content myContent;
    ViewContext myContext;

    private ProviderWrapper(final Content content, final ViewContext context) {
      myContent = content;
      myContext = context;
      setLayout(new BorderLayout());
      add(content.getComponent(), BorderLayout.CENTER);
    }

    @Override
    @Nullable
    public Object getData(@NotNull @NonNls final String dataId) {
      if (ViewContext.CONTENT_KEY.is(dataId)) {
        return new Content[]{myContent};
      }
      else if (ViewContext.CONTEXT_KEY.is(dataId)) {
        return myContext;
      }
      return null;
    }
  }

  @Nullable
  TabInfo getTabFor(Content content) {
    return myContents.getValue(content);
  }

  @NotNull
  private Content getContentFor(TabInfo tab) {
    return myContents.getKey(tab);
  }

  public void setToolbarHorizontal(final boolean horizontal) {
    myTabs.getPresentation().setSideComponentVertical(!horizontal);
  }

  public void setToolbarBefore(final boolean before) {
    myTabs.getPresentation().setSideComponentBefore(before);
  }

  public ActionCallback restoreLastUiState() {
    final ActionCallback result = new ActionCallback();

    restoreProportions();

    final Content[] contents = getContents();
    final List<Content> toMinimize = new SmartList<>();

    int window = 0;
    for (final Content each : contents) {
      final View view = myContainer.getStateFor(each);
      if (view.isMinimizedInGrid()) {
        toMinimize.add(each);
      }

      window = view.getWindow();
    }

    minimize(toMinimize.toArray(new Content[0]));

    final Tab tab = myContainer.getTab();
    final boolean detached = (tab != null && tab.isDetached(myPlaceInGrid)) || window != myContext.getWindow();
    if (detached && contents.length > 0) {
      if (tab != null) {
        tab.setDetached(myPlaceInGrid, false);
      }
      myContext.detachTo(window, this).notifyWhenDone(result);
    }
    else {
      result.setDone();
    }

    return result;
  }

  Content[] getContents() {
    return myContents.getKeys().toArray(new Content[myContents.size()]);
  }

  @Override
  public int getContentCount() {
    return myContents.size();
  }

  public void saveUiState() {
    saveProportions();

    for (Content each : myContents.getKeys()) {
      saveState(each, false);
    }

    for (Content each : myMinimizedContents) {
      saveState(each, true);
    }

    final DimensionService service = DimensionService.getInstance();
    final Dimension size = myContext.getContentManager().getComponent().getSize();
    service.setSize(getDimensionKey(), size, myContext.getProject());
    if (myContext.getWindow() != 0) {
      final Window frame = SwingUtilities.getWindowAncestor(myPlaceholder);
      if (frame != null) {
        service.setLocation(getDimensionKey(), frame.getLocationOnScreen());
      }
    }
  }

  public void saveProportions() {
    myContainer.saveSplitterProportions(myPlaceInGrid);
  }

  private void saveState(Content content, boolean minimized) {
    View state = myContext.getStateFor(content);
    state.setMinimizedInGrid(minimized);
    state.setPlaceInGrid(myPlaceInGrid);
    final List<Content> contents = myContainer.getContents();
    final Tab tab = myContainer.getTabIndex();
    if (minimized && contents.size() == 1 && contents.get(0).equals(content)) {
      state.setTabIndex(-1);
      if (tab instanceof TabImpl) {
        ((TabImpl)tab).setIndex(-1);
      }
    }
    state.assignTab(tab);
    state.setWindow(myContext.getWindow());
  }

  public void restoreProportions() {
    myContainer.restoreLastSplitterProportions(myPlaceInGrid);
  }

  public void updateSelection(final boolean isShowing) {
    ContentManager contentManager = myContext.getContentManager();
    if (contentManager.isDisposed()) return;

    for (Content each : myContents.getKeys()) {
      final TabInfo eachTab = getTabFor(each);
      boolean isSelected = eachTab != null && myTabs.getSelectedInfo() == eachTab;
      if (isSelected && isShowing) {
        contentManager.addSelectedContent(each);
      }
      else {
        contentManager.removeFromSelection(each);
      }
    }

    for (Content each : myMinimizedContents) {
      contentManager.removeFromSelection(each);
    }
  }

  public void minimize(Content[] contents) {
    if (contents.length == 0) return;
    myContext.saveUiState();

    for (final Content each : contents) {
      myMinimizedContents.add(each);
      remove(each);
      saveState(each, true);
      boolean isShowing = myTabs.getComponent().getRootPane() != null;
      myContainer.minimize(each, new CellTransform.Restore() {
        @Override
        public ActionCallback restoreInGrid() {
          return restore(each);
        }
      });
      updateSelection(isShowing);
    }
  }

  @Nullable
  public Point getLocation() {
    return DimensionService.getInstance().getLocation(getDimensionKey(), myContext.getProject());
  }

  @Nullable
  public Dimension getSize() {
    return DimensionService.getInstance().getSize(getDimensionKey(), myContext.getProject());
  }

  private String getDimensionKey() {
    return "GridCell.Tab." + myContainer.getTab().getIndex() + "." + myPlaceInGrid.name();
  }

  public boolean isValidForCalculateProportions() {
    return getContentCount() > 0;
  }

  @Override
  public void minimize(Content content) {
    minimize(new Content[]{content});
  }

  public void closeOrMinimize(MouseEvent e) {
    TabInfo tabInfo = myTabs.findInfo(e);
    if (tabInfo == null) return;

    Content content = getContentFor(tabInfo);
    if (CloseViewAction.isEnabled(new Content[]{content})) {
      CloseViewAction.perform(myContext, content);
    }
    else if (MinimizeViewAction.isEnabled(myContext, getContents(), ViewContext.CELL_TOOLBAR_PLACE)) {
      minimize(content);
    }
  }

  ActionCallback restore(Content content) {
    myMinimizedContents.remove(content);
    return ActionCallback.DONE;
  }

  private static class GridCellTabs extends SingleHeightTabs {
    private final ViewContextEx myContext;

    @Override
    protected TabPainterAdapter createTabPainterAdapter() {
      return new DefaultTabPainterAdapter(JBTabPainter.getDEBUGGER());
    }

    private GridCellTabs(ViewContextEx context, GridImpl container) {
      super(context.getProject(), context.getActionManager(), context.getFocusManager(), container);
      myContext = context;
      JBRunnerTabsBase tabs = ((RunnerContentUi)myContext).myTabs;
      ((JBTabsImpl)tabs).addNestedTabs(this);
    }

    @Override
    public boolean useSmallLabels() {
      return true;
    }

    @Override
    protected SingleRowLayout createSingleRowLayout() {
      return new ScrollableSingleRowLayout(this);
    }

    @Override
    public int tabMSize() {
      return 12;
    }

    @Override
    public void processDropOver(TabInfo over, RelativePoint point) {
      ((RunnerContentUi)myContext).myTabs.processDropOver(over, point);
    }

    @Override
    public Image startDropOver(TabInfo tabInfo, RelativePoint point) {
      return ((RunnerContentUi)myContext).myTabs.startDropOver(tabInfo, point);
    }

    @Override
    public void resetDropOver(TabInfo tabInfo) {
      ((RunnerContentUi)myContext).myTabs.resetDropOver(tabInfo);
    }

    @Override
    protected TabLabel createTabLabel(TabInfo info) {
      return new SingleHeightTabs.SingleHeightLabel(this, info) {
        @Override
        public void setAlignmentToCenter(boolean toCenter) {
          super.setAlignmentToCenter(false);
        }
      };
    }
  }
}
