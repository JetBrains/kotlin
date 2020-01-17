// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.util.Key;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.impl.JpsBuildData;
import com.intellij.task.impl.JpsProjectTaskRunner;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ArtifactsCompiler {
  private static final Key<Set<String>> WRITTEN_PATHS_KEY = Key.create("artifacts_written_paths");

  public static void addWrittenPaths(final CompileContext context, Set<String> writtenPaths) {
    Set<String> paths = context.getUserData(WRITTEN_PATHS_KEY);
    if (paths == null) {
      paths = new THashSet<>();
      context.putUserData(WRITTEN_PATHS_KEY, paths);
    }
    paths.addAll(writtenPaths);
  }

  @Nullable
  public static Set<String> getWrittenPaths(@NotNull CompileContext context) {
    return context.getUserData(WRITTEN_PATHS_KEY);
  }

  @Nullable
  public static Set<String> getWrittenPaths(@NotNull ProjectTaskContext context) {
    JpsBuildData jpsBuildData = context.getUserData(JpsProjectTaskRunner.JPS_BUILD_DATA_KEY);
    return jpsBuildData == null ? null : jpsBuildData.getArtifactsWrittenPaths();
  }
}
