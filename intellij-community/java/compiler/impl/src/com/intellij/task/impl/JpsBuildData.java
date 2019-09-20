// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * {@link JpsBuildData} is available via the {@link ProjectTaskContext}.
 * It provides aggregated details for the multiple invocations of the "JPS" builds during the single run session of a {@link ProjectTaskManager}.
 * This data was introduced for compatibility reasons with the code which still depends on the internal context of "JPS" builder.
 * And shouldn't be used by plugins or new code.
 *
 * @see ProjectTaskContext
 * @see JpsProjectTaskRunner#JPS_BUILD_DATA_KEY
 */
@ApiStatus.Internal
public interface JpsBuildData {
  @NotNull
  Set<String> getArtifactsWrittenPaths();

  @NotNull
  List<CompileContext> getFinishedBuildsContexts();
}
