// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.find.SearchSession;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class PrevNextOccurrenceAction extends DumbAwareAction implements ContextAwareShortcutProvider {
  protected final boolean mySearch;

  PrevNextOccurrenceAction(@NotNull String templateActionId, boolean search) {
    mySearch = search;
    copyFrom(ActionManager.getInstance().getAction(templateActionId));
  }

  @Override
  public final void update(@NotNull AnActionEvent e) {
    SearchSession search = e.getData(SearchSession.KEY);
    boolean invokedByShortcut = e.isFromActionToolbar();
    e.getPresentation().setEnabled(search != null && (invokedByShortcut || search.hasMatches()));
  }

  @Nullable
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
