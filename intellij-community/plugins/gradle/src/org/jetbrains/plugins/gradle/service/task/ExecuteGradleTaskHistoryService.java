// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.task;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
@State(
  name = "gradleExecuteTaskHistory",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class ExecuteGradleTaskHistoryService implements PersistentStateComponent<String[]> {

  private static final int MAX_HISTORY_LENGTH = 20;
  private final LinkedList<String> myHistory = new LinkedList<>();
  private String myWorkDirectory = "";
  private String myCanceledCommand;

  public static ExecuteGradleTaskHistoryService getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ExecuteGradleTaskHistoryService.class);
  }

  @Nullable
  public String getCanceledCommand() {
    return myCanceledCommand;
  }

  public void setCanceledCommand(@Nullable String canceledCommand) {
    myCanceledCommand = canceledCommand;
  }

  public void addCommand(@NotNull String command, @NotNull String projectPath) {
    myWorkDirectory = projectPath.trim();

    command = command.trim();

    if (command.length() == 0) return;

    myHistory.remove(command);
    myHistory.addFirst(command);

    while (myHistory.size() > MAX_HISTORY_LENGTH) {
      myHistory.removeLast();
    }
  }

  public List<String> getHistory() {
    return new ArrayList<>(myHistory);
  }

  @NotNull
  public String getWorkDirectory() {
    return myWorkDirectory;
  }

  @Nullable
  @Override
  public String[] getState() {
    String[] res = new String[myHistory.size() + 1];
    res[0] = myWorkDirectory;

    int i = 1;
    for (String goal : myHistory) {
      res[i++] = goal;
    }

    return res;
  }

  @Override
  public void loadState(@NotNull String[] state) {
    if (state.length == 0) {
      myWorkDirectory = "";
      myHistory.clear();
    }
    else {
      myWorkDirectory = state[0];
      myHistory.addAll(Arrays.asList(state).subList(1, state.length));
    }
  }
}