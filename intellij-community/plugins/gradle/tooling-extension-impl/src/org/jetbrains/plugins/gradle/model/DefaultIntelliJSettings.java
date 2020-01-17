// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import com.intellij.serialization.PropertyMapping;

public final class DefaultIntelliJSettings implements IntelliJSettings, IntelliJProjectSettings {
  private static final long serialVersionUID = 1L;

  private final String settings;

  @PropertyMapping("settings")
  public DefaultIntelliJSettings(String settings) {
    this.settings = settings;
  }

  @Override
  public String getSettings() {
    return settings;
  }
}
