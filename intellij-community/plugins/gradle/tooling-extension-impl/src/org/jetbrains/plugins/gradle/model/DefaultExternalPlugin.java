// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;

public final class DefaultExternalPlugin implements ExternalPlugin {
  private static final long serialVersionUID = 1L;

  @NotNull
  private String id;

  public DefaultExternalPlugin() {
  }

  public DefaultExternalPlugin(ExternalPlugin plugin) {
    id = plugin.getId();
  }

  @NotNull
  @Override
  public String getId() {
    return id;
  }

  public void setId(@NotNull String id) {
    this.id = id;
  }
}
