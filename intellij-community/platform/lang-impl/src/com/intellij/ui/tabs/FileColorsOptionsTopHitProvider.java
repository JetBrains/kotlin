// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs;

import com.intellij.ide.ui.AppearanceOptionsTopHitProvider;
import com.intellij.ide.ui.OptionsSearchTopHitProvider;
import com.intellij.ide.ui.PublicMethodBasedOptionDescription;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.FileColorManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
      BooleanOptionDescription enabled = new Option(manager, LangBundle.message("label.file.colors.enabled"), "isEnabled", "setEnabled");
      return !enabled.isOptionEnabled()
             ? Collections.singletonList(enabled)
             : Collections.unmodifiableCollection(Arrays.asList(
               enabled,
               new Option(manager, LangBundle.message("label.use.file.colors.in.editor.tabs"), "isEnabledForTabs", "setEnabledForTabs"),
               new Option(manager, LangBundle.message("label.use.file.colors.in.project.view"), "isEnabledForProjectView", "setEnabledForProjectView")));
    }
    return Collections.emptyList();
  }

  private static class Option extends PublicMethodBasedOptionDescription {
    private final FileColorManager myManager;

    Option(FileColorManager manager, @NlsContexts.Label String option, String getter, String setter) {
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
