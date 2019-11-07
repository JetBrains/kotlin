// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class FormatChangedTextUtil {
  public static final Key<CharSequence> TEST_REVISION_CONTENT = Key.create("test.revision.content");
  protected static final Logger LOG = Logger.getInstance(FormatChangedTextUtil.class);

  protected FormatChangedTextUtil() {
  }

  @NotNull
  public static FormatChangedTextUtil getInstance() {
    return ServiceManager.getService(FormatChangedTextUtil.class);
  }

  public static boolean hasChanges(@NotNull PsiFile file) {
    final Project project = file.getProject();
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile != null) {
      final Change change = ChangeListManager.getInstance(project).getChange(virtualFile);
      return change != null;
    }
    return false;
  }

  public static boolean hasChanges(@NotNull PsiDirectory directory) {
    return hasChanges(directory.getVirtualFile(), directory.getProject());
  }

  public static boolean hasChanges(@NotNull VirtualFile file, @NotNull Project project) {
    final Collection<Change> changes = ChangeListManager.getInstance(project).getChangesIn(file);
    for (Change change : changes) {
      if (change.getType() == Change.Type.NEW || change.getType() == Change.Type.MODIFICATION) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasChanges(@NotNull VirtualFile[] files, @NotNull Project project) {
    for (VirtualFile file : files) {
      if (hasChanges(file, project))
        return true;
    }
    return false;
  }

  public static boolean hasChanges(@NotNull Module module) {
    final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (VirtualFile root : rootManager.getSourceRoots()) {
      if (hasChanges(root, module.getProject())) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasChanges(@NotNull final Project project) {
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

  @NotNull
  public static List<PsiFile> getChangedFilesFromDirs(@NotNull Project project, @NotNull List<? extends PsiDirectory> dirs)  {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    Collection<Change> changes = new ArrayList<>();

    for (PsiDirectory dir : dirs) {
      changes.addAll(changeListManager.getChangesIn(dir.getVirtualFile()));
    }

    return getChangedFiles(project, changes);
  }

  @NotNull
  public static List<PsiFile> getChangedFiles(@NotNull final Project project, @NotNull Collection<? extends Change> changes) {
    Function<Change, PsiFile> changeToPsiFileMapper = new Function<Change, PsiFile>() {
      private final PsiManager myPsiManager = PsiManager.getInstance(project);

      @Override
      public PsiFile fun(Change change) {
        VirtualFile vFile = change.getVirtualFile();
        return vFile != null ? myPsiManager.findFile(vFile) : null;
      }
    };

    return ContainerUtil.mapNotNull(changes, changeToPsiFileMapper);
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

  @NotNull
  public <T extends PsiElement> List<T> getChangedElements(@NotNull Project project,
                                                           @NotNull Change[] changes,
                                                           @NotNull java.util.function.Function<? super VirtualFile, ? extends List<T>> elementsConvertor) {
    return Arrays.stream(changes).map(Change::getVirtualFile).filter(Objects::nonNull)
                 .flatMap(file -> elementsConvertor.apply(file).stream()).filter(Objects::nonNull)
                 .collect(Collectors.toList());
  }

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
}
