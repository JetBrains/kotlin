// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.platform.ModuleAttachProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used by IDEs where <a href="https://www.jetbrains.com/help/phpstorm/opening-multiple-projects.html">attaching modules</a> is supported.
 */
final class AttachedModuleAwareRecentProjectsManager extends RecentProjectsManagerBase {
  @Nullable
  @Override
  protected String getProjectDisplayName(@NotNull Project project) {
    String name = ModuleAttachProcessor.getMultiProjectDisplayName(project);
    return name != null ? name : super.getProjectDisplayName(project);
  }
}