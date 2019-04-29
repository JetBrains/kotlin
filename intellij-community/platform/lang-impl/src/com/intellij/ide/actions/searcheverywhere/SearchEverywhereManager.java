// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Mikhail.Sokolov
 */
public interface SearchEverywhereManager {

  static SearchEverywhereManager getInstance(Project project) {
    return ServiceManager.getService(project, SearchEverywhereManager.class);
  }

  boolean isShown();

  void show(@NotNull String selectedContributorID, @Nullable String searchText, @NotNull AnActionEvent initEvent); //todo change to contributor??? UX-1

  String getShownContributorID();

  void setShownContributor(@NotNull String newSelectedContributorID); //todo change to contributor??? UX-1

  boolean isShowNonProjectItems();

  void setShowNonProjectItems(boolean show);

}
