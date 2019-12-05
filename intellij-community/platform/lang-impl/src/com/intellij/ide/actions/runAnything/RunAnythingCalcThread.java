// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGeneralGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingRecentGroup;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

class RunAnythingCalcThread implements Computable<RunAnythingSearchListModel> {
  @NotNull private final String myPattern;
  @NotNull private final DataContext myDataContext;
  @NotNull private final Project myProject;

  RunAnythingCalcThread(@NotNull Project project, @NotNull DataContext context, @NotNull String pattern) {
    myProject = project;
    myPattern = pattern;
    myDataContext = context;
  }

  @Override
  public RunAnythingSearchListModel compute() {
    List<RunAnythingItem> items;
    RunAnythingSearchListModel model;

    if (RunAnythingPopupUI.isHelpMode(myPattern)) {
      model = new RunAnythingSearchListModel.RunAnythingHelpListModel();
      items = buildHelpGroups((RunAnythingSearchListModel.RunAnythingHelpListModel)model);
    }
    else {
      model = new RunAnythingSearchListModel.RunAnythingMainListModel();
      items = buildAllGroups(model);
    }

    model.addAll(0, items);

    return model;
  }

  @NotNull
  private List<RunAnythingItem> buildHelpGroups(@NotNull RunAnythingSearchListModel.RunAnythingHelpListModel model) {
    List<RunAnythingItem> items = new ArrayList<>();

    model.getGroups().forEach(group -> {
      group.collectItems(myDataContext, items, RunAnythingPopupUI.trimHelpPattern(myPattern));
    });

    return items;
  }

  @NotNull
  private List<RunAnythingItem> buildAllGroups(@NotNull RunAnythingSearchListModel model) {
    List<RunAnythingItem> items = new ArrayList<>();
    if (myPattern.trim().length() == 0) {
      RunAnythingRecentGroup.INSTANCE.collectItems(myDataContext, items, myPattern);
    }
    else {
      buildCompletionGroups(myDataContext, items, model);
    }

    return items;
  }

  private void buildCompletionGroups(@NotNull DataContext dataContext, @NotNull List<RunAnythingItem> items, @NotNull RunAnythingSearchListModel model) {
    if (DumbService.getInstance(myProject).isDumb()) {
      return;
    }

    StreamEx.of(RunAnythingRecentGroup.INSTANCE)
      .select(RunAnythingGroup.class)
      .append(model.getGroups().stream()
                .filter(group -> group instanceof RunAnythingCompletionGroup || group instanceof RunAnythingGeneralGroup)
                .filter(group -> RunAnythingCache.getInstance(myProject).isGroupVisible(group.getTitle())))
      .forEach(group -> {
        group.collectItems(dataContext, items, myPattern);
      });
  }
}