// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import org.jetbrains.annotations.NotNull;

final class FileBasedIndexPluginListener implements DynamicPluginListener {
  private final @NotNull FileBasedIndexSwitcher mySwitcher;

  FileBasedIndexPluginListener(@NotNull FileBasedIndexImpl index) {
    mySwitcher = new FileBasedIndexSwitcher(index);
  }

  @Override
  public void beforePluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    beforePluginSetChanged();
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
    beforePluginSetChanged();
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    afterPluginSetChanged();
  }

  @Override
  public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
    afterPluginSetChanged();
  }

  private void beforePluginSetChanged() {
    mySwitcher.turnOff();
  }

  private void afterPluginSetChanged() {
    mySwitcher.turnOn();
  }
}
