// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {
  protected final boolean mySearch;

  PrevNextOccurrenceAction(@NotNull String templateActionId, boolean search) {
    mySearch = search;
    ActionUtil.copyFrom(this, templateActionId);
  }

  @Override
  public final void update(@NotNull AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    e.getPresentation().setEnabled(search != null && !search.isSearchInProgress() && search.hasMatches());
  }

  @Override
  public final ShortcutSet getShortcut(@NotNull DataContext context) {
    SearchSession search = SearchSession.KEY.getData(context);
    boolean singleLine = search != null && !search.getFindModel().isMultiline();
    return Utils.shortcutSetOf(singleLine ? ContainerUtil.concat(getDefaultShortcuts(), getSingleLineShortcuts()) : getDefaultShortcuts());
  }

  @NotNull
  protected abstract List<Shortcut> getDefaultShortcuts();

  @NotNull
  protected abstract List<Shortcut> getSingleLineShortcuts();
}
