// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@State(name = "CompletionMLRankingSettings", storages = @Storage("completionMLRanking.xml"))
public class CompletionMLRankingSettings implements PersistentStateComponent<CompletionMLRankingSettings.State> {
  private static final Logger LOG = Logger.getInstance(CompletionMLRankingSettings.class);

  private static final Collection<String> ENABLE_RANKING_BY_DEFAULT = Collections.emptyList();
  private final State myState = new State();

  @NotNull
  public static CompletionMLRankingSettings getInstance() {
    return ServiceManager.getService(CompletionMLRankingSettings.class);
  }

  public boolean isRankingEnabled() {
    return myState.rankingEnabled;
  }

  void setRankingEnabled(boolean value) {
    myState.rankingEnabled = value;
  }

  public boolean isCompletionLogsSendAllowed() {
    return ApplicationManager.getApplication().isEAP() && Registry.is("completion.stats.send.logs");
  }

  public boolean isLanguageEnabled(@NotNull String languageName) {
    return myState.enabledLanguages.contains(StringUtil.toLowerCase(languageName));
  }

  public void setLanguageEnabled(@NotNull String languageName, boolean isEnabled) {
    String lowerCase = StringUtil.toLowerCase(languageName);
    if (isEnabled) {
      myState.enabledLanguages.add(lowerCase);
    }
    else {
      myState.enabledLanguages.remove(lowerCase);
    }
    logCompletionState(languageName, isEnabled);
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState.rankingEnabled = state.rankingEnabled;
    myState.enabledLanguages.clear();
    myState.enabledLanguages.addAll(state.enabledLanguages);

    for (String language : state.enabledLanguages) {
      logCompletionState(language, true);
    }
  }

  private void logCompletionState(@NotNull String languageName, boolean isEnabled) {
    final boolean enabled = myState.rankingEnabled && isEnabled;
    LOG.info("ML Completion " + (enabled ? "enabled" : "disabled") + " for: " + languageName);
  }

  public static class State {
    public boolean rankingEnabled = false;

    public Set<String> enabledLanguages = new HashSet<>(ENABLE_RANKING_BY_DEFAULT);
  }
}
