// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CodeCompletionFeatures;
import com.intellij.codeInsight.completion.ShowHideIntentionIconLookupAction;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.Alarm;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * @author peter
 */
class LookupUi {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.lookup.impl.LookupUi");

  @NotNull
  private final LookupImpl myLookup;
  private final Advertiser myAdvertiser;
  private final JBList myList;
  private final ModalityState myModalityState;
  private final Alarm myHintAlarm = new Alarm();
  private final JScrollPane myScrollPane;
  private final AsyncProcessIcon myProcessIcon = new AsyncProcessIcon("Completion progress");
  private final ActionButton myMenuButton;
  private final ActionButton myHintButton;
  private final JComponent myBottomPanel;

  private int myMaximumHeight = Integer.MAX_VALUE;
  private Boolean myPositionedAbove = null;

  LookupUi(@NotNull LookupImpl lookup, Advertiser advertiser, JBList list) {
    myLookup = lookup;
    myAdvertiser = advertiser;
    myList = list;

    myProcessIcon.setVisible(false);
    myLookup.resort(false);

    MenuAction menuAction = new MenuAction();
    menuAction.add(new ChangeSortingAction());
    menuAction.add(new DelegatedAction(ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_JAVADOC)){
      @Override
      public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(!CodeInsightSettings.getInstance().AUTO_POPUP_JAVADOC_INFO);
      }
    });
    menuAction.add(new DelegatedAction(ActionManager.getInstance().getAction(IdeActions.ACTION_QUICK_IMPLEMENTATIONS)));

    Presentation presentation = new Presentation();
    presentation.setIcon(AllIcons.Actions.More);
    presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, Boolean.TRUE);

    myMenuButton = new ActionButton(menuAction, presentation, ActionPlaces.EDITOR_POPUP, ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE);

    AnAction hintAction = new HintAction();
    myHintButton = new ActionButton(hintAction, hintAction.getTemplatePresentation(),
                                    ActionPlaces.EDITOR_POPUP, ActionToolbar.NAVBAR_MINIMUM_BUTTON_SIZE);
    myHintButton.setVisible(false);

    myBottomPanel = new NonOpaquePanel(new LookupBottomLayout());
    myBottomPanel.add(myAdvertiser.getAdComponent());
    myBottomPanel.add(myProcessIcon);
    myBottomPanel.add(myHintButton);
    myBottomPanel.add(myMenuButton);

    LookupLayeredPane layeredPane = new LookupLayeredPane();
    layeredPane.mainPanel.add(myBottomPanel, BorderLayout.SOUTH);

    myScrollPane = ScrollPaneFactory.createScrollPane(lookup.getList(), true);
    myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    UIUtil.putClientProperty(myScrollPane.getVerticalScrollBar(), JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, true);

    lookup.getComponent().add(layeredPane, BorderLayout.CENTER);

    layeredPane.mainPanel.add(myScrollPane, BorderLayout.CENTER);

    myModalityState = ModalityState.stateForComponent(lookup.getTopLevelEditor().getComponent());

    addListeners();

    Disposer.register(lookup, myProcessIcon);
    Disposer.register(lookup, myHintAlarm);
  }

  private void addListeners() {
    myList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myLookup.isLookupDisposed()) return;

        myHintAlarm.cancelAllRequests();
        updateHint();
      }
    });
  }

  private void updateHint() {
    myLookup.checkValid();
    if (myHintButton.isVisible()) {
      myHintButton.setVisible(false);
    }

    LookupElement item = myLookup.getCurrentItem();
    if (item != null && item.isValid()) {
      Collection<LookupElementAction> actions = myLookup.getActionsFor(item);
      if (!actions.isEmpty()) {
        myHintAlarm.addRequest(() -> {
          if (ShowHideIntentionIconLookupAction.shouldShowLookupHint() &&
              !((CompletionExtender)myList.getExpandableItemsHandler()).isShowing() &&
              !myProcessIcon.isVisible()) {
            myHintButton.setVisible(true);
          }
        }, 500, myModalityState);
      }
    }
  }

  void setCalculating(boolean calculating) {
    Runnable iconUpdater = () -> {
      if (calculating && myHintButton.isVisible()) {
        myHintButton.setVisible(false);
      }

      ApplicationManager.getApplication().invokeLater(() -> {
        myProcessIcon.setVisible(calculating);

        if (!calculating) {
          updateHint();
        }
      }, myModalityState);
    };

    if (calculating) {
      myProcessIcon.resume();
    } else {
      myProcessIcon.suspend();
    }
    new Alarm(myLookup).addRequest(iconUpdater, 100, myModalityState);
  }

  void refreshUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
    Editor editor = myLookup.getTopLevelEditor();
    if (editor.getComponent().getRootPane() == null || editor instanceof EditorWindow && !((EditorWindow)editor).isValid()) {
      return;
    }

    if (myLookup.myResizePending || itemsChanged) {
      myMaximumHeight = Integer.MAX_VALUE;
    }
    Rectangle rectangle = calculatePosition();
    myMaximumHeight = rectangle.height;

    if (myLookup.myResizePending || itemsChanged) {
      myLookup.myResizePending = false;
      myLookup.pack();
    }
    HintManagerImpl.updateLocation(myLookup, editor, rectangle.getLocation());

    if (reused || selectionVisible || onExplicitAction) {
      myLookup.ensureSelectionVisible(false);
    }
  }

  boolean isPositionedAboveCaret() {
    return myPositionedAbove != null && myPositionedAbove.booleanValue();
  }

  // in layered pane coordinate system.
  Rectangle calculatePosition() {
    final JComponent lookupComponent = myLookup.getComponent();
    Dimension dim = lookupComponent.getPreferredSize();
    int lookupStart = myLookup.getLookupStart();
    Editor editor = myLookup.getTopLevelEditor();
    if (lookupStart < 0 || lookupStart > editor.getDocument().getTextLength()) {
      LOG.error(lookupStart + "; offset=" + editor.getCaretModel().getOffset() + "; element=" +
                myLookup.getPsiElement());
    }

    LogicalPosition pos = editor.offsetToLogicalPosition(lookupStart);
    Point location = editor.logicalPositionToXY(pos);
    location.y += editor.getLineHeight();
    location.x -= myLookup.myCellRenderer.getTextIndent();
    // extra check for other borders
    final Window window = UIUtil.getWindow(lookupComponent);
    if (window != null) {
      final Point point = SwingUtilities.convertPoint(lookupComponent, 0, 0, window);
      location.x -= point.x;
    }

    SwingUtilities.convertPointToScreen(location, editor.getContentComponent());
    final Rectangle screenRectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());

    if (!isPositionedAboveCaret()) {
      int shiftLow = screenRectangle.y + screenRectangle.height - (location.y + dim.height);
      myPositionedAbove = shiftLow < 0 && shiftLow < location.y - dim.height && location.y >= dim.height;
    }
    if (isPositionedAboveCaret()) {
      location.y -= dim.height + editor.getLineHeight();
      if (pos.line == 0) {
        location.y += 1;
        //otherwise the lookup won't intersect with the editor and every editor's resize (e.g. after typing in console) will close the lookup
      }
    }

    if (!screenRectangle.contains(location)) {
      location = ScreenUtil.findNearestPointOnBorder(screenRectangle, location);
    }

    Rectangle candidate = new Rectangle(location, dim);
    ScreenUtil.cropRectangleToFitTheScreen(candidate);

    JRootPane rootPane = editor.getComponent().getRootPane();
    if (rootPane != null) {
      SwingUtilities.convertPointFromScreen(location, rootPane.getLayeredPane());
    }
    else {
      LOG.error("editor.disposed=" + editor.isDisposed() + "; lookup.disposed=" + myLookup.isLookupDisposed() + "; editorShowing=" + editor.getContentComponent().isShowing());
    }

    myMaximumHeight = candidate.height;
    return new Rectangle(location.x, location.y, dim.width, candidate.height);
  }

  private class LookupLayeredPane extends JBLayeredPane {
    final JPanel mainPanel = new JPanel(new BorderLayout());

    private LookupLayeredPane() {
      mainPanel.setBackground(LookupCellRenderer.BACKGROUND_COLOR);
      add(mainPanel, 0, 0);

      setLayout(new AbstractLayoutManager() {
        @Override
        public Dimension preferredLayoutSize(@Nullable Container parent) {
          int maxCellWidth = myLookup.myLookupTextWidth + myLookup.myCellRenderer.getTextIndent();
          int scrollBarWidth = myScrollPane.getVerticalScrollBar().getWidth();
          int listWidth = Math.min(scrollBarWidth + maxCellWidth, UISettings.getInstance().getMaxLookupWidth());

          Dimension bottomPanelSize = myBottomPanel.getPreferredSize();

          int panelHeight = myScrollPane.getPreferredSize().height + bottomPanelSize.height;
          int width = Math.max(listWidth, bottomPanelSize.width);
          width = Math.min(width, Registry.intValue("ide.completion.max.width"));
          int height = Math.min(panelHeight, myMaximumHeight);

          return new Dimension(width, height);
        }

        @Override
        public void layoutContainer(Container parent) {
          Dimension size = getSize();
          mainPanel.setSize(size);
          mainPanel.validate();

          if (IdeEventQueue.getInstance().getTrueCurrentEvent().getID() == MouseEvent.MOUSE_DRAGGED) {
            Dimension preferredSize = preferredLayoutSize(null);
            if (preferredSize.width != size.width) {
              UISettings.getInstance().setMaxLookupWidth(Math.max(500, size.width));
            }

            int listHeight = myList.getLastVisibleIndex() - myList.getFirstVisibleIndex() + 1;
            if (listHeight != myList.getModel().getSize() && listHeight != myList.getVisibleRowCount() && preferredSize.height != size.height) {
              UISettings.getInstance().setMaxLookupListHeight(Math.max(5, listHeight));
            }
          }

          myList.setFixedCellWidth(myScrollPane.getViewport().getWidth());
        }
      });
    }
  }

  private class HintAction extends DumbAwareAction {
    private HintAction() {
      super(null, null, AllIcons.Actions.IntentionBulb);

      AnAction showIntentionAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
      if (showIntentionAction != null) {
        copyShortcutFrom(showIntentionAction);
        getTemplatePresentation().setText("Click or Press");
      }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myLookup.showElementActions(e.getInputEvent());
    }
  }

  private static class MenuAction extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore {
    private MenuAction() {
      setPopup(true);
    }
  }

  private class ChangeSortingAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {
    private boolean sortByName = UISettings.getInstance().getSortLookupElementsLexicographically();
    private ChangeSortingAction() {
      super("Sort by Name");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (e.getPlace() == ActionPlaces.EDITOR_POPUP) {
        sortByName = !sortByName;

        FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CHANGE_SORTING);
        UISettings.getInstance().setSortLookupElementsLexicographically(sortByName);
        myLookup.resort(false);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setIcon(sortByName ? PlatformIcons.CHECK_ICON : null);
    }
  }

  private static class DelegatedAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {
    private final AnAction delegateAction;
    private DelegatedAction(AnAction action) {
      delegateAction = action;
      getTemplatePresentation().setText(delegateAction.getTemplateText(), true);
      copyShortcutFrom(delegateAction);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (e.getPlace() == ActionPlaces.EDITOR_POPUP) {
        delegateAction.actionPerformed(e);
      }
    }
  }

  private class LookupBottomLayout implements LayoutManager {
    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      Dimension adSize = myAdvertiser.getAdComponent().getPreferredSize();
      Dimension hintButtonSize = myHintButton.getPreferredSize();
      Dimension menuButtonSize = myMenuButton.getPreferredSize();

      return new Dimension(adSize.width + hintButtonSize.width + menuButtonSize.width,
                           Math.max(adSize.height, menuButtonSize.height));
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension adSize = myAdvertiser.getAdComponent().getMinimumSize();
      Dimension hintButtonSize = myHintButton.getMinimumSize();
      Dimension menuButtonSize = myMenuButton.getMinimumSize();

      return new Dimension(adSize.width + hintButtonSize.width + menuButtonSize.width,
                           Math.max(adSize.height, menuButtonSize.height));
    }

    @Override
    public void layoutContainer(Container parent) {
      Dimension size = parent.getSize();

      Dimension menuButtonSize = myMenuButton.getPreferredSize();
      int x = size.width - menuButtonSize.width;
      int y = (size.height - menuButtonSize.height) / 2;

      myMenuButton.setBounds(x, y, menuButtonSize.width, menuButtonSize.height);

      Dimension myHintButtonSize = myHintButton.getPreferredSize();
      if (myHintButton.isVisible() && !myProcessIcon.isVisible()) {
        x -= myHintButtonSize.width;
        y = (size.height - myHintButtonSize.height) / 2;
        myHintButton.setBounds(x, y, myHintButtonSize.width, myHintButtonSize.height);
      }
      else if (!myHintButton.isVisible() && myProcessIcon.isVisible()) {
        Dimension myProcessIconSize = myProcessIcon.getPreferredSize();
        x -= myProcessIconSize.width;
        y = (size.height - myProcessIconSize.height) / 2;
        myProcessIcon.setBounds(x, y, myProcessIconSize.width, myProcessIconSize.height);
      }
      else if (!myHintButton.isVisible() && !myProcessIcon.isVisible()) {
        x -= myHintButtonSize.width;
      }
      else {
        throw new IllegalStateException("Can't show both process icon and hint button");
      }

      Dimension adSize = myAdvertiser.getAdComponent().getPreferredSize();
      y = (size.height - adSize.height) / 2;
      myAdvertiser.getAdComponent().setBounds(0, y, x, adSize.height);
    }
  }
}
