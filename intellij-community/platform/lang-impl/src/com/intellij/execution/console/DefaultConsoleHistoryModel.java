// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory.Shrago
 */
public class DefaultConsoleHistoryModel extends SimpleModificationTracker implements ConsoleHistoryModel {

  private final static Map<String, DefaultConsoleHistoryModel> ourModels =
    ConcurrentFactoryMap.create(key -> new DefaultConsoleHistoryModel(null),
                                   ContainerUtil::createConcurrentWeakValueMap);

  public static DefaultConsoleHistoryModel createModel(String persistenceId) {
    return ourModels.get(persistenceId).copy();
  }

  private final Object myLock;
  private final LinkedList<String> myEntries;
  private int myIndex;
  private String myContent;

  DefaultConsoleHistoryModel(@Nullable DefaultConsoleHistoryModel masterModel) {
    myEntries = masterModel == null ? new LinkedList<>() : masterModel.myEntries;
    myLock = masterModel == null ? this : masterModel.myLock;  // hard ref to master model
    resetIndex();
  }

  public DefaultConsoleHistoryModel copy() {
    return new DefaultConsoleHistoryModel(this);
  }

  @Override
  public void resetEntries(@NotNull List<String> entries) {
    synchronized (myLock) {
      myEntries.clear();
      myEntries.addAll(ContainerUtil.getFirstItems(entries, getMaxHistorySize()));
      incModificationCount();
    }
  }

  @Override
  public void addToHistory(@Nullable String statement) {
    if (StringUtil.isEmptyOrSpaces(statement)) return;

    synchronized (myLock) {
      int maxHistorySize = getMaxHistorySize();
      myEntries.remove(statement);
      int size = myEntries.size();
      if (size >= maxHistorySize && size > 0) {
        myEntries.removeFirst();
      }
      myEntries.addLast(statement);
      incModificationCount();
    }
  }

  @Override
  public void incModificationCount() {
    resetIndex();
    super.incModificationCount();
  }

  protected void resetIndex() {
    synchronized (myLock) {
      myIndex = myEntries.size();
    }
  }

  @Override
  public int getMaxHistorySize() {
    return UISettings.getInstance().getConsoleCommandHistoryLimit();
  }

  @Override
  public void removeFromHistory(String statement) {
    synchronized (myLock) {
      myEntries.remove(statement);
      incModificationCount();
    }
  }

  @NotNull
  @Override
  public List<String> getEntries() {
    synchronized (myLock) {
      return new ArrayList<>(myEntries);
    }
  }

  @Override
  public boolean isEmpty() {
    synchronized (myLock) {
      return myEntries.isEmpty();
    }
  }

  @Override
  public int getHistorySize() {
    synchronized (myLock) {
      return myEntries.size();
    }
  }

  @Override
  @Nullable
  public Entry getHistoryNext() {
    synchronized (myLock) {
      if (myIndex >= 0) --myIndex;
      return new Entry(getCurrentEntry(), -1);
    }
  }

  @Override
  @Nullable
  public Entry getHistoryPrev() {
    synchronized (myLock) {
      if (myIndex <= myEntries.size() - 1) ++myIndex;
      return new Entry(getCurrentEntry(), -1);
    }
  }

  @Override
  public boolean hasHistory() {
    synchronized (myLock) {
      return myIndex <= myEntries.size() - 1;
    }
  }

  String getCurrentEntry() {
    synchronized (myLock) {
      return myIndex >= 0 && myIndex < myEntries.size() ? myEntries.get(myIndex) :
             myIndex == myEntries.size() ? myContent : null;
    }
  }

  @Override
  public int getCurrentIndex() {
    synchronized (myLock) {
      return myIndex;
    }
  }

  @Override
  public void setContent(@NotNull String userContent) {
    myContent = userContent;
  }
}
