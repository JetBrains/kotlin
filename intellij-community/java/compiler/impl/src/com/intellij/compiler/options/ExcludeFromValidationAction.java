// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.compiler.options;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.compiler.options.ExcludesConfiguration;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class ExcludeFromValidationAction extends AnAction {

  public ExcludeFromValidationAction() {
    super(CompilerBundle.message("action.name.exclude.from.validation"));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    final Pair<ExcludesConfiguration, VirtualFile> pair = getExcludedConfigurationAndFile(e, project);
    if (pair == null) return;
    final ExcludeEntryDescription description = new ExcludeEntryDescription(pair.getSecond(), false, true, project);
    pair.getFirst().addExcludeEntryDescription(description);
  }

  @Nullable
  private static Pair<ExcludesConfiguration, VirtualFile> getExcludedConfigurationAndFile(final AnActionEvent event, Project project) {
    Navigatable navigatable = event.getData(CommonDataKeys.NAVIGATABLE);
    if (project != null && navigatable instanceof OpenFileDescriptor) {
      final VirtualFile file = ((OpenFileDescriptor)navigatable).getFile();
      final ExcludesConfiguration configuration = ValidationConfiguration.getExcludedEntriesConfiguration(project);
      return Pair.create(configuration, file);
    }
    return null;
  }


  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    final boolean applicable = getExcludedConfigurationAndFile(e, project) != null;
    e.getPresentation().setEnabledAndVisible(applicable);
  }
}
