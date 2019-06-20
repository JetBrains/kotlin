// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

import org.jetbrains.annotations.NotNull;

public class ArtifactComponentNode extends AbstractComponentNode {

  private final String group;
  private final String module;
  private final String version;

  public ArtifactComponentNode(long id, @NotNull String group, @NotNull String module, @NotNull String version) {
    super(id);
    this.group = group;
    this.module = module;
    this.version = version;
  }

  @NotNull
  public String getGroup() {
    return group;
  }

  @NotNull
  public String getModule() {
    return module;
  }

  @NotNull
  public String getVersion() {
    return version;
  }

  @Override
  String getDisplayName() {
    return group + ':' + module + ':' + version;
  }
}
