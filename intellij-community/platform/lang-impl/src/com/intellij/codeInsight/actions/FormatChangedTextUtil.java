// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @deprecated use {@link VcsFacade} instead
 */
@Deprecated
public class FormatChangedTextUtil {

  @NotNull
  public static FormatChangedTextUtil getInstance() {
    return ServiceManager.getService(FormatChangedTextUtil.class);
  }

  public static boolean hasChanges(@NotNull PsiFile file) {
    return VcsFacade.getInstance().hasChanges(file);
  }

  public static boolean hasChanges(@NotNull PsiDirectory directory) {
    return VcsFacade.getInstance().hasChanges(directory);
  }

  public static boolean hasChanges(@NotNull VirtualFile file, @NotNull Project project) {
    return VcsFacade.getInstance().hasChanges(file, project);
  }

  public static boolean hasChanges(VirtualFile @NotNull [] files, @NotNull Project project) {
    return VcsFacade.getInstance().hasChanges(files, project);
  }

  public static boolean hasChanges(@NotNull Module module) {
    return VcsFacade.getInstance().hasChanges(module);
  }

  public static boolean hasChanges(@NotNull final Project project) {
    return VcsFacade.getInstance().hasChanges(project);
  }

  @NotNull
  public static List<PsiFile> getChangedFilesFromDirs(@NotNull Project project, @NotNull List<? extends PsiDirectory> dirs)  {
    return VcsFacade.getInstance().getChangedFilesFromDirs(project, dirs);
  }

  @NotNull
  public List<TextRange> getChangedTextRanges(@NotNull Project project, @NotNull PsiFile file) {
    return VcsFacade.getInstance().getChangedTextRanges(project, file);
  }

  public int calculateChangedLinesNumber(@NotNull Document document, @NotNull CharSequence contentFromVcs) {
    return VcsFacade.getInstance().calculateChangedLinesNumber(document, contentFromVcs);
  }

  public boolean isChangeNotTrackedForFile(@NotNull Project project, @NotNull PsiFile file) {
    return VcsFacade.getInstance().isChangeNotTrackedForFile(project, file);
  }

  @Nullable
  public ChangedRangesInfo getChangedRangesInfo(@NotNull PsiFile file) {
    return VcsFacade.getInstance().getChangedRangesInfo(file);
  }

  /**
   * Allows to temporally suppress document modification tracking.
   *
   * Ex: To perform a task, that might delete whole document and re-create it from scratch.
   * Such modification would destroy all existing ranges. While using `runHeavyModificationTask` would make trackers to compare
   * only starting end finishing document states, ignoring intermediate modifications (because "actual" differences might be small).
   */
  public void runHeavyModificationTask(@NotNull Project project, @NotNull Document document, @NotNull Runnable o) {
    VcsFacade.getInstance().runHeavyModificationTask(project, document, o);
  }
}
