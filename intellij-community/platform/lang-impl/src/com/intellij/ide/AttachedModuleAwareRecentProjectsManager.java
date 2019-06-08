// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.platform.ModuleAttachProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * Used by IDEs where <a href="https://www.jetbrains.com/help/phpstorm/opening-multiple-projects.html">attaching modules</a> is supported.
 */
final class AttachedModuleAwareRecentProjectsManager extends RecentDirectoryProjectsManager {
  @NotNull
  @Override
  protected String getProjectDisplayName(@NotNull Project project) {
    final String name = ModuleAttachProcessor.getMultiProjectDisplayName(project);
    return name != null ? name : super.getProjectDisplayName(project);
  }
}