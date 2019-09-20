// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@State(name = "RunAnythingCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RunAnythingCache implements PersistentStateComponent<RunAnythingCache.State> {
  private final State mySettings = new State();

  public static RunAnythingCache getInstance(Project project) {
    return ServiceManager.getService(project, RunAnythingCache.class);
  }

  /**
   * @return true is group is visible; false if it's hidden
   */
  public boolean isGroupVisible(@NotNull String key) {
    return mySettings.myKeys.get(key);
  }

  /**
   * Saves group visibility flag
   *
   * @param key     to store visibility flag
   * @param visible true if group should be shown
   */
  public void saveGroupVisibilityKey(@NotNull String key, boolean visible) {
    mySettings.myKeys.put(key, visible);
  }

  @NotNull
  @Override
  public State getState() {
    return mySettings;
  }

  @Override
  public void loadState(@NotNull State state) {
    XmlSerializerUtil.copyBean(state, mySettings);

    updateNewProvidersGroupVisibility(mySettings);
  }

  /**
   * Updates group visibilities store for new providers
   */
  private static void updateNewProvidersGroupVisibility(@NotNull State settings) {
    StreamEx.of(RunAnythingProvider.EP_NAME.getExtensions())
      .filter(provider -> provider.getCompletionGroupTitle() != null)
      .distinct(RunAnythingProvider::getCompletionGroupTitle)
      .filter(provider -> !settings.myKeys.containsKey(provider.getCompletionGroupTitle()))
      .forEach(provider -> settings.myKeys.put(provider.getCompletionGroupTitle(), true));
  }

  public static class State {
    @XMap(entryTagName = "visibility", keyAttributeName = "group", valueAttributeName = "flag")
    @NotNull private final Map<String, Boolean> myKeys =
      StreamEx.of(RunAnythingProvider.EP_NAME.getExtensions())
              .filter(provider -> provider.getCompletionGroupTitle() != null)
              .distinct(RunAnythingProvider::getCompletionGroupTitle)
              .collect(Collectors.toMap(RunAnythingProvider::getCompletionGroupTitle, group -> true));

    @XCollection(elementName = "command")
    @NotNull private final List<String> myCommands = new ArrayList<>();

    @NotNull
    public List<String> getCommands() {
      return myCommands;
    }
  }
}