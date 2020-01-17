// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author cdr
 */
public final class ViewClassHierarchyAction extends ChangeViewTypeActionBase {
  public ViewClassHierarchyAction() {
    super(IdeBundle.message("action.view.class.hierarchy"),
          IdeBundle.message("action.description.view.class.hierarchy"), AllIcons.Hierarchy.Class);
  }

  @Override
  protected final HierarchyScopeType getTypeName() {
    return TypeHierarchyBrowserBase.getTypeHierarchyType();
  }

  @Override
  public final void update(@NotNull final AnActionEvent event) {
    super.update(event);
    final TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
    event.getPresentation().setEnabled(browser != null && !browser.isInterface());
  }
}
