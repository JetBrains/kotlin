// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

import com.intellij.model.ModelPatch;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VcsFacade {
  public static final Key<CharSequence> TEST_REVISION_CONTENT = Key.create("test.revision.content");
  protected static final Logger LOG = Logger.getInstance(VcsFacade.class);

  protected VcsFacade() {
  }

  @NotNull
  public static VcsFacade getInstance() {
    return ServiceManager.getService(VcsFacade.class);
  }

  public boolean hasChanges(@NotNull PsiFile file) {
    return false;
  }

  public boolean hasChanges(@NotNull PsiDirectory directory) {
    return hasChanges(directory.getVirtualFile(), directory.getProject());
  }

  public boolean hasChanges(@NotNull VirtualFile file, @NotNull Project project) {
    return false;
  }

  public boolean hasChanges(VirtualFile @NotNull [] files, @NotNull Project project) {
    for (VirtualFile file : files) {
      if (hasChanges(file, project))
        return true;
    }
    return false;
  }

  public boolean hasChanges(@NotNull Module module) {
    final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (VirtualFile root : rootManager.getSourceRoots()) {
      if (hasChanges(root, module.getProject())) {
        return true;
      }
    }
    return false;
  }

  public boolean hasChanges(@NotNull final Project project) {
    final ModifiableModuleModel moduleModel = ReadAction.compute(() -> ModuleManager.getInstance(project).getModifiableModel());
    try {
      for (Module module : moduleModel.getModules()) {
        if (hasChanges(module)) {
          return true;
        }
      }
      return false;
    }
    finally {
      moduleModel.dispose();
    }
  }

  public Boolean isFileUnderVcs(@NotNull PsiFile psiFile) {
    return false;
  }

  @NotNull
  public Set<String> getVcsIgnoreFileNames(@NotNull Project project) { return Collections.emptySet(); }

  @NotNull
  public List<PsiFile> getChangedFilesFromDirs(@NotNull Project project, @NotNull List<? extends PsiDirectory> dirs)  {
    return Collections.emptyList();
  }

  @NotNull
  public List<TextRange> getChangedTextRanges(@NotNull Project project, @NotNull PsiFile file) {
    return ContainerUtil.emptyList();
  }

  public int calculateChangedLinesNumber(@NotNull Document document, @NotNull CharSequence contentFromVcs) {
    return -1;
  }

  public boolean isChangeNotTrackedForFile(@NotNull Project project, @NotNull PsiFile file) {
    return false;
  }

  @Nullable
  public ChangedRangesInfo getChangedRangesInfo(@NotNull PsiFile file) {
    return null;
  }

  public void markFilesDirty(@NotNull Project project, @NotNull List<? extends VirtualFile> virtualFiles) { }

  /**
   * Allows to temporally suppress document modification tracking.
   *
   * Ex: To perform a task, that might delete whole document and re-create it from scratch.
   * Such modification would destroy all existing ranges. While using `runHeavyModificationTask` would make trackers to compare
   * only starting end finishing document states, ignoring intermediate modifications (because "actual" differences might be small).
   */
  public void runHeavyModificationTask(@NotNull Project project, @NotNull Document document, @NotNull Runnable o) {
    o.run();
  }

  @ApiStatus.Experimental
  @Nullable
  public JComponent createPatchPreviewComponent(@NotNull Project project, @NotNull ModelPatch patch) {
    return null;
  }
}
