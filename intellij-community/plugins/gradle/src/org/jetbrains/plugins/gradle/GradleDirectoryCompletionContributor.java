// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle;

import com.intellij.ide.actions.CreateDirectoryCompletionContributor;
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class GradleDirectoryCompletionContributor implements CreateDirectoryCompletionContributor {
  @NotNull
  @Override
  public String getDescription() {
    return "Gradle Source Sets";
  }

  @NotNull
  @Override
  public Collection<Variant> getVariants(@NotNull PsiDirectory directory) {
    Project project = directory.getProject();

    Module module = ProjectFileIndex.getInstance(project).getModuleForFile(directory.getVirtualFile());
    if (module == null) return Collections.emptyList();

    return SourceFolderManager.getInstance(project)
      .getSourceFolders()
      .map(folder -> {
        ModuleSourceRootEditHandler<?> handler = ModuleSourceRootEditHandler.getEditHandler(folder.second);
        return new Variant(VfsUtilCore.urlToPath(folder.first), handler == null ? null : handler.getRootIcon());
      })
      .collect(Collectors.toList());
  }
}
