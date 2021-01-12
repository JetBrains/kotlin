// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.jetbrains.completion.ranker.WeakModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@State(name = "CompletionMLRankingSettings", storages = @Storage("completionMLRanking.xml"))
public class CompletionMLRankingSettings implements PersistentStateComponent<CompletionMLRankingSettings.State> {
  private static final Logger LOG = Logger.getInstance(CompletionMLRankingSettings.class);

  private static final Collection<String> ENABLED_BY_DEFAULT = WeakModelProvider.enabledByDefault();
  private final State myState = new State();

  @NotNull
  public static CompletionMLRankingSettings getInstance() {
    return ServiceManager.getService(CompletionMLRankingSettings.class);
  }

  public boolean isRankingEnabled() {
    return myState.rankingEnabled;
  }

  public boolean isShowDiffEnabled() {
    return myState.showDiff;
  }

  public void setRankingEnabled(boolean value) {
    myState.rankingEnabled = value;
  }

  public boolean isCompletionLogsSendAllowed() {
    return ApplicationManager.getApplication().isEAP() && Registry.is("completion.stats.send.logs");
  }

  public boolean isLanguageEnabled(@NotNull String languageName) {
    return myState.language2state.getOrDefault(languageName, ENABLED_BY_DEFAULT.contains(languageName));
  }

  public void setLanguageEnabled(@NotNull String languageName, boolean isEnabled) {
    boolean defaultValue = ENABLED_BY_DEFAULT.contains(languageName);
    if (defaultValue == isEnabled) {
      myState.language2state.remove(languageName);
    }
    else {
      myState.language2state.put(languageName, isEnabled);
    }

    logCompletionState(languageName, isEnabled);
  }

  public void setShowDiffEnabled(boolean isEnabled) {
    myState.showDiff = isEnabled;
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState.rankingEnabled = state.rankingEnabled;
    myState.showDiff = state.showDiff;
    state.language2state.forEach((lang, enabled) -> setLanguageEnabled(lang, enabled));
  }

  private void logCompletionState(@NotNull String languageName, boolean isEnabled) {
    final boolean enabled = myState.rankingEnabled && isEnabled;
    final boolean showDiff = enabled && myState.showDiff;
    LOG.info("ML Completion " + (enabled ? "enabled" : "disabled") + " ,show diff " + (showDiff ? "on" : "off") + " for: " + languageName);
  }

  public static class State {
    public boolean rankingEnabled = !ENABLED_BY_DEFAULT.isEmpty();
    public boolean showDiff = false;
    // this map stores only different compare to default values to have ability to enable/disable models from build to build
    public Map<String, Boolean> language2state = new HashMap<>();
  }
}
