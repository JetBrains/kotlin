// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

import java.util.LinkedHashSet;
import java.util.Set;

public class DependencyNode {
  private final long id;
  private String name;
  private String state;
  private final Set<DependencyNode> children = new LinkedHashSet<DependencyNode>();

  public DependencyNode(long id) {this.id = id;}

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<DependencyNode> getChildren() {
    return children;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
