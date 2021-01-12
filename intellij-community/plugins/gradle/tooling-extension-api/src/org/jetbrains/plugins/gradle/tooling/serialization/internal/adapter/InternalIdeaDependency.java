// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.tooling.model.idea.IdeaDependency;

public abstract class InternalIdeaDependency implements IdeaDependency {
  private InternalIdeaDependencyScope myDependencyScope;
  private boolean myExported;

  @Override
  public InternalIdeaDependencyScope getScope() {
    return myDependencyScope;
  }

  public void setScope(InternalIdeaDependencyScope dependencyScope) {
    myDependencyScope = dependencyScope;
  }

  @Override
  public boolean getExported() {
    return myExported;
  }

  public void setExported(boolean exported) {
    myExported = exported;
  }

  @Override
  public String toString() {
    return "IdeaDependency{" +
           "myDependencyScope=" + myDependencyScope +
           ", myExported=" + myExported +
           '}';
  }
}
