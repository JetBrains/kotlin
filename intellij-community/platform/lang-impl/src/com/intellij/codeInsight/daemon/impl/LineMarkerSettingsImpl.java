// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.GutterIconDescriptor;
import com.intellij.codeInsight.daemon.LineMarkerSettings;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
@State(
  name = "LineMarkerSettings",
  storages = @Storage("gutter.xml")
)
public class LineMarkerSettingsImpl extends LineMarkerSettings implements PersistentStateComponent<LineMarkerSettingsImpl> {

  @Override
  public boolean isEnabled(@NotNull GutterIconDescriptor descriptor) {
    Boolean aBoolean = providers.get(descriptor.getId());
    return aBoolean == null || aBoolean;
  }

  @Override
  public void setEnabled(@NotNull GutterIconDescriptor descriptor, boolean selected) {
    providers.put(descriptor.getId(), selected);
  }

  @MapAnnotation
  public Map<String, Boolean> providers = new HashMap<>();

  @Nullable
  @Override
  public LineMarkerSettingsImpl getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull LineMarkerSettingsImpl state) {
    providers.clear();
    providers.putAll(state.providers);
  }
}
