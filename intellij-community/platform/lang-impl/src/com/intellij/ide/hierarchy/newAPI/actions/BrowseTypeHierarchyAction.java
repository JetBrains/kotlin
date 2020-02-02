// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy.newAPI.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.LanguageTypeHierarchy;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public final class BrowseTypeHierarchyAction extends BrowseHierarchyActionBase {
  public BrowseTypeHierarchyAction() {
    super(LanguageTypeHierarchy.INSTANCE);
  }

  @Override
  public final void update(@NotNull final AnActionEvent event){
    final Presentation presentation = event.getPresentation();
    if (!ActionPlaces.isMainMenuOrActionSearch(event.getPlace())) {
      presentation.setText(IdeBundle.lazyMessage("action.browse.type.hierarchy"));
    }
    super.update(event);
  }
}