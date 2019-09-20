// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.tasks;

public class ReferenceNode implements ComponentNode {
  private final long id;

  public ReferenceNode(long id) {this.id = id;}

  @Override
  public long getId() {
    return id;
  }
}
