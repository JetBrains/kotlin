// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates.actions;

import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class FileTemplateSeparator extends ActionGroup {

  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return new AnAction[]{Separator.create(shouldShowNamedSeparator(e) ? "File templates" : null)};
  }

  private static boolean shouldShowNamedSeparator(@Nullable AnActionEvent e) {
    if (e == null || e.isFromContextMenu()) return false; // popup menus show the name, but no separator currently, which looks ugly
    return new CreateFromTemplateGroup().getChildren(e).length > 0;
  }
}
