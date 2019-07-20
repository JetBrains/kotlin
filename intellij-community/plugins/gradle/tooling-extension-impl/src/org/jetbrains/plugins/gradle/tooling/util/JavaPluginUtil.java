// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaPluginUtil {
  @Nullable
  public static JavaPluginConvention getJavaPluginConvention(@NotNull Project p) {
    return p.getConvention().findPlugin(JavaPluginConvention.class);
  }

  @Nullable
  public static SourceSetContainer getSourceSetContainer(@NotNull Project p) {
    final JavaPluginConvention convention = getJavaPluginConvention(p);
    return (convention == null ? null : convention.getSourceSets());
  }
}
