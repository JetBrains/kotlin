// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.progress;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import org.jetbrains.annotations.NotNull;

public class ModuleLinkFilter implements Filter {

  private Project myProject;
  private static final String TESTS_PREFIX = "tests of "; //NON-NLS

  public ModuleLinkFilter(Project project) {
    myProject = project;
  }

  @Override
  public Result applyFilter(@NotNull String line, int entireLength) {
    int start = line.indexOf("[");
    if (start == -1) return null;
    int end = line.indexOf(']', start + 1);
    if (end == -1) return null;
    String moduleNameCandidate = line.substring(start + 1, end);
    boolean isTests = moduleNameCandidate.startsWith(TESTS_PREFIX);
    String moduleName = isTests ? moduleNameCandidate.substring(TESTS_PREFIX.length()) : moduleNameCandidate;

    int lineStart = entireLength - line.length();
    return new Result(lineStart + start + 1 + (isTests ? TESTS_PREFIX.length() : 0), lineStart + end, new HyperlinkInfo() {
      @Override
      public void navigate(Project project) {
        if (project.isDisposed()) return;
        Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleName);
        if (module == null || module.isDisposed()) return;
        ProjectSettingsService.getInstance(myProject).openContentEntriesSettings(module);
      }
    });
  }
}
