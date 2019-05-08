// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.completion.CodeCompletionFeatures;
import com.intellij.codeInsight.completion.ShowHideIntentionIconLookupAction;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBList;
import com.intellij.util.Alarm;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
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
  private final Project myProject;
  private final ModalityState myModalityState;
  private final Alarm myHintAlarm = new Alarm();
  private final JLabel mySortingLabel = new JLabel();
  private final JScrollPane myScrollPane;
  private final AsyncProcessIcon myProcessIcon = new AsyncProcessIcon("Completion progress");
  private final JPanel myIconPanel = new JPanel(new BorderLayout());
  private final LookupLayeredPane myLayeredPane = new LookupLayeredPane();

  private LookupHint myElementHint = null;
  private int myMaximumHeight = Integer.MAX_VALUE;
  private Boolean myPositionedAbove = null;

  LookupUi(@NotNull LookupImpl lookup, Advertiser advertiser, JBList list, Project project) {
    myLookup = lookup;
    myAdvertiser = advertiser;
    myList = list;
    myProject = project;

    myIconPanel.setVisible(false);
    myIconPanel.setOpaque(false);
    myIconPanel.add(myProcessIcon);

    JComponent adComponent = advertiser.getAdComponent();
    myLayeredPane.mainPanel.add(adComponent, BorderLayout.SOUTH);

    myScrollPane = ScrollPaneFactory.createScrollPane(lookup.getList(), true);
    myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    lookup.getComponent().add(myLayeredPane, BorderLayout.CENTER);

    //IDEA-82111
    fixMouseCheaters();

    myLayeredPane.mainPanel.add(myScrollPane, BorderLayout.CENTER);

    mySortingLabel.setBorder(new CustomLineBorder(new JBColor(Gray._192, JBColor.background()), JBUI.insets(1)));
    mySortingLabel.setOpaque(true);
    new ChangeLookupSorting().installOn(mySortingLabel);
    updateSorting();
    myModalityState = ModalityState.stateForComponent(lookup.getTopLevelEditor().getComponent());

    addListeners();

    updateScrollbarVisibility();

    Disposer.register(lookup, myProcessIcon);
    Disposer.register(lookup, myHintAlarm);
  }

  private void addListeners() {
    myList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myLookup.isLookupDisposed()) return;

        myHintAlarm.cancelAllRequests();

        final LookupElement item = myLookup.getCurrentItem();
        if (item != null) {
          updateHint(item);
        }
      }
    });
  }

  private void updateScrollbarVisibility() {
    boolean showSorting = myLookup.isCompletion() && myList.getModel().getSize() >= 3;
    mySortingLabel.setVisible(showSorting);
    myScrollPane.setVerticalScrollBarPolicy(showSorting ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
  }

  private void updateHint(@NotNull final LookupElement item) {
    myLookup.checkValid();
    if (myElementHint != null) {
      myLayeredPane.remove(myElementHint);
      myElementHint = null;
      final JRootPane rootPane = myLookup.getComponent().getRootPane();
      if (rootPane != null) {
        rootPane.revalidate();
        rootPane.repaint();
      }
    }
    if (!item.isValid()) {
      return;
    }

    final Collection<LookupElementAction> actions = myLookup.getActionsFor(item);
    if (!actions.isEmpty()) {
      myHintAlarm.addRequest(() -> {
        if (!ShowHideIntentionIconLookupAction.shouldShowLookupHint() ||
            ((CompletionExtender)myList.getExpandableItemsHandler()).isShowing()) {
          return;
        }
        myElementHint = new LookupHint();
        myLayeredPane.add(myElementHint, 20, 0);
        myLayeredPane.layoutHint();
      }, 500, myModalityState);
    }
  }

  //Yes, it's possible to move focus to the hint. It's inconvenient, it doesn't make sense, but it's possible.
  // This fix is for those jerks
  private void fixMouseCheaters() {
    myLookup.getComponent().addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        final ActionCallback done = IdeFocusManager.getInstance(myProject).requestFocus(myLookup.getTopLevelEditor().getContentComponent(), true);
        IdeFocusManager.getInstance(myProject).typeAheadUntil(done);
        new Alarm(myLookup).addRequest(() -> {
          if (!done.isDone()) {
            done.setDone();
          }
        }, 300, myModalityState);
      }
    });
  }

  void setCalculating(final boolean calculating) {
    Runnable setVisible = () -> myIconPanel.setVisible(myLookup.isCalculating());
    if (myLookup.isCalculating()) {
      new Alarm(myLookup).addRequest(setVisible, 100, myModalityState);
    } else {
      setVisible.run();
    }

    if (calculating) {
      myProcessIcon.resume();
    } else {
      myProcessIcon.suspend();
    }
  }

  private void updateSorting() {
    final boolean lexi = UISettings.getInstance().getSortLookupElementsLexicographically();
    mySortingLabel.setIcon(lexi ? AllIcons.Ide.LookupAlphanumeric : AllIcons.Ide.LookupRelevance);
    mySortingLabel.setToolTipText(lexi ? "Click to sort variants by relevance" : "Click to sort variants alphabetically");

    myLookup.resort(false);
  }

  void refreshUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
    Editor editor = myLookup.getTopLevelEditor();
    if (editor.getComponent().getRootPane() == null || editor instanceof EditorWindow && !((EditorWindow)editor).isValid()) {
      return;
    }

    updateScrollbarVisibility();

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

    final JRootPane rootPane = editor.getComponent().getRootPane();
    if (rootPane == null) {
      LOG.error("editor.disposed=" + editor.isDisposed() + "; lookup.disposed=" + myLookup.isLookupDisposed() + "; editorShowing=" + editor.getContentComponent().isShowing());
    }
    Rectangle candidate = new Rectangle(location, dim);
    ScreenUtil.cropRectangleToFitTheScreen(candidate);

    SwingUtilities.convertPointFromScreen(location, rootPane.getLayeredPane());
    myMaximumHeight = candidate.height;
    return new Rectangle(location.x, location.y, dim.width, candidate.height);
  }

  private class LookupLayeredPane extends JBLayeredPane {
    final JPanel mainPanel = new JPanel(new BorderLayout());

    private LookupLayeredPane() {
      add(mainPanel, 0, 0);
      add(myIconPanel, 42, 0);
      add(mySortingLabel, 10, 0);

      setLayout(new AbstractLayoutManager() {
        @Override
        public Dimension preferredLayoutSize(@Nullable Container parent) {
          int maxCellWidth = myLookup.myLookupTextWidth + myLookup.myCellRenderer.getTextIndent();
          int scrollBarWidth = myScrollPane.getPreferredSize().width - myScrollPane.getViewport().getPreferredSize().width;
          int listWidth = Math.min(scrollBarWidth + maxCellWidth, UISettings.getInstance().getMaxLookupWidth());

          Dimension adSize = myAdvertiser.getAdComponent().getPreferredSize();

          int panelHeight = myScrollPane.getPreferredSize().height + adSize.height;
          int width = Math.max(listWidth, adSize.width);
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
          layoutStatusIcons();
          layoutHint();
        }
      });
    }

    private void layoutStatusIcons() {
      int adHeight = myAdvertiser.getAdComponent().getPreferredSize().height;
      int bottomOffset = adHeight > 0 || !mySortingLabel.isVisible() ? 0 : AllIcons.Ide.LookupRelevance.getIconHeight();
      JScrollBar vScrollBar = myScrollPane.getVerticalScrollBar();
      ScrollBottomBorder.update(vScrollBar, bottomOffset);

      final Dimension iconSize = myProcessIcon.getPreferredSize();
      myIconPanel.setBounds(getWidth() - iconSize.width - (vScrollBar.isVisible() ? vScrollBar.getWidth() : 0), 0, iconSize.width,
                            iconSize.height);

      final Dimension sortSize = mySortingLabel.getPreferredSize();
      final int sortWidth = vScrollBar.isVisible() ? vScrollBar.getWidth() : sortSize.width;
      final int sortHeight = Math.max(sortSize.height, adHeight);
      mySortingLabel.setBounds(getWidth() - sortWidth, getHeight() - sortHeight, sortSize.width, sortHeight);
    }

    void layoutHint() {
      if (myElementHint != null && myLookup.getCurrentItem() != null) {
        final Rectangle bounds = myLookup.getCurrentItemBounds();
        myElementHint.setSize(myElementHint.getPreferredSize());

        JScrollBar sb = myScrollPane.getVerticalScrollBar();
        int x = bounds.x + bounds.width - myElementHint.getWidth() + (sb.isVisible() ? sb.getWidth() : 0);
        x = Math.min(x, getWidth() - myElementHint.getWidth());
        myElementHint.setLocation(new Point(x, bounds.y));
      }
    }
  }

  private class LookupHint extends JLabel {
    private final Border INACTIVE_BORDER = JBUI.Borders.empty(2);
    private final Border ACTIVE_BORDER = new CompoundBorder(new CustomLineBorder(JBColor.BLACK, JBUI.insets(1)), JBUI.Borders.empty(1));
    private LookupHint() {
      setOpaque(false);
      setBorder(INACTIVE_BORDER);
      setIcon(AllIcons.Actions.IntentionBulb);
      String acceleratorsText = KeymapUtil.getFirstKeyboardShortcutText(
        ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
      if (acceleratorsText.length() > 0) {
        setToolTipText(CodeInsightBundle.message("lightbulb.tooltip", acceleratorsText));
      }

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          setBorder(ACTIVE_BORDER);
        }

        @Override
        public void mouseExited(MouseEvent e) {
          setBorder(INACTIVE_BORDER);
        }
        @Override
        public void mousePressed(MouseEvent e) {
          if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
            myLookup.showElementActions();
          }
        }
      });
    }
  }

  private class ChangeLookupSorting extends ClickListener {

    @Override
    public boolean onClick(@NotNull MouseEvent e, int clickCount) {
      DataContext context = DataManager.getInstance().getDataContext(mySortingLabel);
      DefaultActionGroup group = new DefaultActionGroup();
      group.add(createSortingAction(true));
      group.add(createSortingAction(false));
      JBPopupFactory.getInstance().createActionGroupPopup("Change Sorting", group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false).showInBestPositionFor(
        context);
      return true;
    }

    private AnAction createSortingAction(boolean checked) {
      boolean isAlpha = UISettings.getInstance().getSortLookupElementsLexicographically();
      boolean makeAlpha = checked == isAlpha;
      class ChangeSortingAction extends DumbAwareAction implements HintManagerImpl.ActionToIgnore {
        ChangeSortingAction() {
          super(makeAlpha ? "Sort alphabetically" : "Sort by relevance", null, checked ? PlatformIcons.CHECK_ICON : null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CHANGE_SORTING);
          UISettings.getInstance().setSortLookupElementsLexicographically(makeAlpha);
          updateSorting();
        }
      }
      return new ChangeSortingAction();
    }
  }

  private static class ScrollBottomBorder extends AbstractBorder {
    private final int myBottomOffset;

    ScrollBottomBorder(int bottomOffset) {
      myBottomOffset = bottomOffset;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
      insets.set(0, 0, myBottomOffset, 0);
      return insets;
    }

    static void update(JScrollBar bar, int bottomOffset) {
      if (bar != null) {
        Border border = bar.getBorder();
        if (border instanceof ScrollBottomBorder) {
          ScrollBottomBorder sbb = (ScrollBottomBorder)border;
          if (bottomOffset != sbb.myBottomOffset) {
            bar.setBorder(new ScrollBottomBorder(bottomOffset));
          }
        }
        else if (bottomOffset != 0) {
          bar.setBorder(new ScrollBottomBorder(bottomOffset));
        }
      }
    }
  }
}
