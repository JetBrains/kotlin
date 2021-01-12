// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.ui.ListActions;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public final class RunAnythingScrollingUtil {

  /**
   * @deprecated unused
   */
  @NonNls
  @Deprecated
  protected static final String SELECT_PREVIOUS_ROW_ACTION_ID = ListActions.Up.ID;

  /**
   * @deprecated unused
   */
  @NonNls
  @Deprecated
  protected static final String SELECT_NEXT_ROW_ACTION_ID = ListActions.Down.ID;

  public static void installActions(@NotNull JList list,
                                    @NotNull JTextField focusParent,
                                    @NotNull Runnable handleFocusParent,
                                    boolean isCycleScrolling) {
    ActionMap actionMap = list.getActionMap();
    actionMap.put(ListActions.Up.ID, new MoveAction(ListActions.Up.ID, list, handleFocusParent, isCycleScrolling));
    actionMap.put(ListActions.Down.ID, new MoveAction(ListActions.Down.ID, list, handleFocusParent, isCycleScrolling));

    maybeInstallDefaultShortcuts(list);

    installMoveUpAction(list, focusParent, handleFocusParent, isCycleScrolling);
    installMoveDownAction(list, focusParent, handleFocusParent, isCycleScrolling);
  }

  private static void maybeInstallDefaultShortcuts(@NotNull JComponent component) {
    InputMap map = component.getInputMap(JComponent.WHEN_FOCUSED);
    UIUtil.maybeInstall(map, ListActions.Up.ID, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
    UIUtil.maybeInstall(map, ListActions.Down.ID, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
  }

  private static void installMoveDownAction(@NotNull JList list,
                                            @NotNull JComponent focusParent,
                                            @NotNull Runnable handleFocusParent,
                                            final boolean isCycleScrolling) {
    new ScrollingUtil.ListScrollAction(CommonShortcuts.getMoveDown(), focusParent) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        moveDown(list, handleFocusParent, isCycleScrolling);
      }
    };
  }

  private static void installMoveUpAction(@NotNull JList list,
                                          @NotNull JComponent focusParent,
                                          @NotNull Runnable handleFocusParent,
                                          final boolean isCycleScrolling) {
    new ScrollingUtil.ListScrollAction(CommonShortcuts.getMoveUp(), focusParent) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        moveUp(list, handleFocusParent, isCycleScrolling);
      }
    };
  }

  private static void moveDown(@NotNull JList list, @NotNull Runnable handleFocusParent, boolean isCycleScrolling) {
    move(list, list.getSelectionModel(), list.getModel().getSize(), +1, handleFocusParent, isCycleScrolling);
  }

  private static void moveUp(@NotNull JList list, @NotNull Runnable handleFocusParent, boolean isCycleScrolling) {
    move(list, list.getSelectionModel(), list.getModel().getSize(), -1, handleFocusParent, isCycleScrolling);
  }

  private static void move(@NotNull JList c,
                           @NotNull ListSelectionModel selectionModel,
                           int size,
                           int direction,
                           @NotNull Runnable handleFocusParent,
                           boolean isCycleScrolling) {
    if (size == 0) return;
    int index = selectionModel.getMaxSelectionIndex();
    int indexToSelect = index + direction;

    if ((indexToSelect == -2 || indexToSelect >= size) && !isCycleScrolling) {
      return;
    }

    if (indexToSelect == -2) {
      indexToSelect = size - 1;
    }
    else if (indexToSelect == -1 || indexToSelect >= size) {
      handleFocusParent.run();
      return;
    }

    ScrollingUtil.ensureIndexIsVisible(c, indexToSelect, -1);
    selectionModel.setSelectionInterval(indexToSelect, indexToSelect);
  }

  private static class MoveAction extends AbstractAction {
    @NotNull private final String myId;
    @NotNull private final JList myComponent;
    @NotNull private final Runnable myHandleFocusParent;
    private final boolean myIsCycleScrolling;

    MoveAction(@NotNull String id, @NotNull JList component, @NotNull Runnable handleFocusParent, boolean isCycleScrolling) {
      myId = id;
      myComponent = component;
      myHandleFocusParent = handleFocusParent;
      myIsCycleScrolling = isCycleScrolling;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (ListActions.Up.ID.equals(myId)) {
        moveUp(myComponent, myHandleFocusParent, myIsCycleScrolling);
      }
      else if (ListActions.Down.ID.equals(myId)) {
        moveDown(myComponent, myHandleFocusParent, myIsCycleScrolling);
      }
    }
  }
}