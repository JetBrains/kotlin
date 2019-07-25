// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ComponentNotRegistered")
public class SelectAllAction extends OccurrenceAction {
  public SelectAllAction() {
    super(IdeActions.ACTION_SELECT_ALL_OCCURRENCES, AllIcons.Actions.CheckMulticaret);
  }

  @Override
  protected boolean availableForReplace() {
    return true;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    search.selectAllOccurrences();
    search.close();
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    List<Shortcut> shortcuts = new ArrayList<>();
    AnAction selectAllOccurrences = ActionManager.getInstance().getAction(IdeActions.ACTION_SELECT_ALL_OCCURRENCES);
    if (selectAllOccurrences != null) {
      ContainerUtil.addAll(shortcuts, selectAllOccurrences.getShortcutSet().getShortcuts());
    }
    ContainerUtil.addAll(shortcuts, CommonShortcuts.ALT_ENTER.getShortcuts());
    return Utils.shortcutSetOf(shortcuts);
  }
}
