// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import com.intellij.openapi.util.text.StringUtilRt;
import gnu.trove.THashMap;
import org.gradle.tooling.model.idea.IdeaDependencyScope;

public class InternalIdeaDependencyScope implements IdeaDependencyScope {
  private static final THashMap<String, InternalIdeaDependencyScope> SCOPES_MAP = new THashMap<String, InternalIdeaDependencyScope>();

  static {
    SCOPES_MAP.put("Compile", new InternalIdeaDependencyScope("Compile"));
    SCOPES_MAP.put("Test", new InternalIdeaDependencyScope("Test"));
    SCOPES_MAP.put("Runtime", new InternalIdeaDependencyScope("Runtime"));
    SCOPES_MAP.put("Provided", new InternalIdeaDependencyScope("Provided"));
  }

  private final String myScope;

  public InternalIdeaDependencyScope(String scope) {
    myScope = scope;
  }

  @Override
  public String getScope() {
    return myScope;
  }

  @Override
  public String toString() {
    return "IdeaDependencyScope{" +
           "myScope='" + myScope + '\'' +
           '}';
  }

  public static InternalIdeaDependencyScope getInstance(String scope) {
    InternalIdeaDependencyScope dependencyScope = SCOPES_MAP.get(StringUtilRt.isEmpty(scope) ? "Compile" : scope);
    return dependencyScope == null ? new InternalIdeaDependencyScope(scope) : dependencyScope;
  }
}
