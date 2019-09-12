// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.editorHeaderActions.ContextAwareShortcutProvider;
import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.largeFilesEditor.search.LfeSearchManager;
import com.intellij.largeFilesEditor.search.searchTask.CloseSearchTask;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {

  private final LfeSearchManager mySearchManager;
  private final boolean myDirectionForward;

  public PrevNextOccurrenceAction(LfeSearchManager searchManager, boolean directionForward) {
    mySearchManager = searchManager;
    myDirectionForward = directionForward;

    copyFrom(ActionManager.getInstance().getAction(
      directionForward ? IdeActions.ACTION_NEXT_OCCURENCE : IdeActions.ACTION_PREVIOUS_OCCURENCE));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (isEnabled()) {
      mySearchManager.gotoNextOccurrence(myDirectionForward);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled());
  }

  private boolean isEnabled() {
    CloseSearchTask task = mySearchManager.getLastExecutedCloseSearchTask();
    return task == null || task.isFinished();
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut(@NotNull DataContext context) {
    List<Shortcut> list = new ArrayList<>();
    boolean isSingleLine = !isMultiLine();
    if (myDirectionForward) {
      list.addAll(Utils.shortcutsOf(IdeActions.ACTION_FIND_NEXT));
      if (isSingleLine) {
        list.addAll(Utils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN));
        Collections.addAll(list, CommonShortcuts.ENTER.getShortcuts());
      }
    }
    else {
      list.addAll(Utils.shortcutsOf(IdeActions.ACTION_FIND_PREVIOUS));
      if (isSingleLine) {
        list.addAll(Utils.shortcutsOf(IdeActions.ACTION_EDITOR_MOVE_CARET_UP));
        list.add(new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null));
      }
    }
    return Utils.shortcutSetOf(list);
  }

  private boolean isMultiLine() {
    SearchReplaceComponent searchReplaceComponent = mySearchManager.getSearchReplaceComponent();
    return searchReplaceComponent != null && searchReplaceComponent.isMultiline();
  }
}
