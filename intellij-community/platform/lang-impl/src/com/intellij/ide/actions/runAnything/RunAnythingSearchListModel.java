// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingHelpGroup;
import com.intellij.ide.actions.runAnything.groups.RunAnythingRecentGroup;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("unchecked")
public abstract class RunAnythingSearchListModel extends DefaultListModel<Object> {
  @SuppressWarnings("UseOfObsoleteCollectionType")
  Vector myDelegate;

  protected RunAnythingSearchListModel() {
    super();
    myDelegate = ReflectionUtil.getField(DefaultListModel.class, this, Vector.class, "delegate");
    clearIndexes();
  }

  @NotNull
  protected abstract List<RunAnythingGroup> getGroups();

  void clearIndexes() {
    RunAnythingGroup.clearIndexes(getGroups());
  }

  @Nullable
  RunAnythingGroup findGroupByMoreIndex(int index) {
    return RunAnythingGroup.findGroupByMoreIndex(getGroups(), index);
  }

  void shiftIndexes(int baseIndex, int shift) {
    RunAnythingGroup.shiftIndexes(getGroups(), baseIndex, shift);
  }

  @Nullable
  String getTitle(int titleIndex) {
    return RunAnythingGroup.getTitle(getGroups(), titleIndex);
  }

  @Nullable
  RunAnythingGroup findItemGroup(int titleIndex) {
    return RunAnythingGroup.findItemGroup(getGroups(), titleIndex);
  }

  int[] getAllIndexes() {
    RunAnythingGroup.getAllIndexes(getGroups());
    return new int[0];
  }

  boolean isMoreIndex(int index) {
    return RunAnythingGroup.isMoreIndex(getGroups(), index);
  }

  int next(int index) {
    int[] all = getAllIndexes();
    Arrays.sort(all);
    for (int next : all) {
      if (next > index) return next;
    }
    return 0;
  }

  int prev(int index) {
    int[] all = getAllIndexes();
    Arrays.sort(all);
    for (int i = all.length - 1; i >= 0; i--) {
      if (all[i] != -1 && all[i] < index) return all[i];
    }
    return all[all.length - 1];
  }

  @Override
  public void addElement(Object obj) {
    myDelegate.add(obj);
  }

  public void update() {
    fireContentsChanged(this, 0, getSize() - 1);
  }

  public static class RunAnythingMainListModel extends RunAnythingSearchListModel {
    @NotNull
    @Override
    public List<RunAnythingGroup> getGroups() {
      List<RunAnythingGroup> groups = ContainerUtil.newArrayList(RunAnythingRecentGroup.INSTANCE);
      groups.addAll(RunAnythingCompletionGroup.MAIN_GROUPS);
      return groups;
    }
  }

  public static class RunAnythingHelpListModel extends RunAnythingSearchListModel {
    @Nullable private List<RunAnythingGroup> myHelpGroups;

    @NotNull
    @Override
    protected List<RunAnythingGroup> getGroups() {
      if (myHelpGroups == null) {
        myHelpGroups = new ArrayList<>();
        myHelpGroups.addAll(ContainerUtil.map(StreamEx.of(RunAnythingProvider.EP_NAME.extensions())
                                                .filter(provider -> provider.getHelpGroupTitle() != null)
                                                .groupingBy(provider -> provider.getHelpGroupTitle())
                                                .entrySet(), entry -> new RunAnythingHelpGroup(entry.getKey(), entry.getValue())));

        List<RunAnythingGroup> epGroups = Arrays.asList(RunAnythingHelpGroup.EP_NAME.getExtensions());
        if (!epGroups.isEmpty()) {
          myHelpGroups.addAll(epGroups);
        }
      }
      return myHelpGroups;
    }
  }
}
