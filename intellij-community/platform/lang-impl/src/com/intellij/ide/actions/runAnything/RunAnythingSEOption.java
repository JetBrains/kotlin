// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

class RunAnythingSEOption extends BooleanOptionDescription {
  @NotNull private final String myKey;
  @NotNull private final Project myProject;

  RunAnythingSEOption(@NotNull Project project, @NotNull String option, @NotNull String key) {
    super(option, null);
    myProject = project;
    myKey = key;
  }

  @Override
  public boolean isOptionEnabled() {
    return RunAnythingCache.getInstance(myProject).isGroupVisible(myKey);
  }

  @Override
  public void setOptionState(boolean enabled) {
    RunAnythingCache.getInstance(myProject).saveGroupVisibilityKey(myKey, enabled);
  }
}
