// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "StatsCollectorSettings", storages = @Storage("statsCollector.xml"))
public class CompletionStatsCollectorSettings implements PersistentStateComponent<CompletionStatsCollectorSettings.State> {
  private final State myState = new State();

  @NotNull
  public static CompletionStatsCollectorSettings getInstance() {
    return ServiceManager.getService(CompletionStatsCollectorSettings.class);
  }

  public boolean isCompletionLogsSendAllowed() {
    return myState.dataSendAllowed;
  }

  public boolean isRankingEnabled() {
    return myState.rankingEnabled;
  }

  void setRankingEnabled(boolean value) {
    myState.rankingEnabled = value;
  }

  public void setCompletionLogsSendAllowed(boolean value) {
    myState.dataSendAllowed = value;
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState.rankingEnabled = state.rankingEnabled;
    myState.dataSendAllowed = state.dataSendAllowed;
  }

  public static class State {
    public boolean rankingEnabled = false;
    public boolean dataSendAllowed = false;
  }
}
