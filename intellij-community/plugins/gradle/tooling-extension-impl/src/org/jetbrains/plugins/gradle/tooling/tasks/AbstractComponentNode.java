// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractComponentNode implements ComponentNode {
  private final long id;
  private final Set<ComponentNode> children = new LinkedHashSet<ComponentNode>();
  private String state;

  protected AbstractComponentNode(long id) {this.id = id;}

  @Override
  public long getId() {
    return id;
  }

  abstract String getDisplayName();

  public Set<ComponentNode> getChildren() {
    return children;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
