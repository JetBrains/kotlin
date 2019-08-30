// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.ide.ui.OptionsSearchTopHitProvider;
import com.intellij.ide.ui.search.OptionDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

class EditorOptionsTopHitProvider implements OptionsSearchTopHitProvider.ApplicationLevelProvider {
  @NotNull
  @Override
  public String getId() {
    return EditorOptionsPanel.ID;
  }

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions() {
    return EditorCheckBoxConfigurableKt.getOptionDescriptors();
  }
}
