// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs;

import com.intellij.ide.ui.AppearanceOptionsTopHitProvider;
import com.intellij.ide.ui.OptionsSearchTopHitProvider;
import com.intellij.ide.ui.PublicMethodBasedOptionDescription;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.project.Project;
import com.intellij.ui.FileColorManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey.Malenkov
 */
public final class FileColorsOptionsTopHitProvider implements OptionsSearchTopHitProvider.ProjectLevelProvider {
  @NotNull
  @Override
  public String getId() {
    return AppearanceOptionsTopHitProvider.ID;
  }

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions(@NotNull Project project) {
    FileColorManager manager = FileColorManager.getInstance(project);
    if (manager != null) {
      BooleanOptionDescription enabled = new Option(manager, "File Colors enabled", "isEnabled", "setEnabled");
      return !enabled.isOptionEnabled()
             ? Collections.singletonList(enabled)
             : Collections.unmodifiableCollection(Arrays.asList(
               enabled,
               new Option(manager, "Use File Colors in Editor Tabs", "isEnabledForTabs", "setEnabledForTabs"),
               new Option(manager, "Use File Colors in Project View", "isEnabledForProjectView", "setEnabledForProjectView")));
    }
    return Collections.emptyList();
  }

  private static class Option extends PublicMethodBasedOptionDescription {
    private final FileColorManager myManager;

    Option(FileColorManager manager, String option, String getter, String setter) {
      super(option, "reference.settings.ide.settings.file-colors", getter, setter);
      myManager = manager;
    }

    @Override
    public Object getInstance() {
      return myManager;
    }

    @Override
    protected void fireUpdated() {
      UISettings.getInstance().fireUISettingsChanged();
    }
  }
}
