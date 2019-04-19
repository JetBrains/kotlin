/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectView.actions;

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class MarkRootActionBase extends DumbAwareAction {
  public MarkRootActionBase() {
  }

  public MarkRootActionBase(@Nullable String text) {
    super(text);
  }

  public MarkRootActionBase(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    final Module module = getModule(e, files);
    if (module == null) {
      return;
    }
    modifyRoots(e, module, files);
  }

  protected void modifyRoots(@NotNull  AnActionEvent e, @NotNull final Module module, @NotNull VirtualFile[] files) {
    final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    for (VirtualFile file : files) {
      ContentEntry entry = findContentEntry(model, file);
      if (entry != null) {
        final SourceFolder[] sourceFolders = entry.getSourceFolders();
        for (SourceFolder sourceFolder : sourceFolders) {
          if (Comparing.equal(sourceFolder.getFile(), file)) {
            entry.removeSourceFolder(sourceFolder);
            break;
          }
        }
        modifyRoots(file, entry);
      }
    }
    commitModel(module, model);
  }

  static void commitModel(@NotNull Module module, ModifiableRootModel model) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      model.commit();
      module.getProject().save();
    });
  }

  protected abstract void modifyRoots(VirtualFile file, ContentEntry entry);

  @Nullable
  public static ContentEntry findContentEntry(@NotNull ModuleRootModel model, @NotNull VirtualFile vFile) {
    final ContentEntry[] contentEntries = model.getContentEntries();
    for (ContentEntry contentEntry : contentEntries) {
      final VirtualFile contentEntryFile = contentEntry.getFile();
      if (contentEntryFile != null && VfsUtilCore.isAncestor(contentEntryFile, vFile, false)) {
        return contentEntry;
      }
    }
    return null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    RootsSelection selection = getSelection(e);
    doUpdate(e, selection.myModule, selection);
  }

  protected void doUpdate(@NotNull AnActionEvent e, @Nullable Module module, @NotNull RootsSelection selection) {
    boolean enabled = module != null && (!selection.mySelectedRoots.isEmpty() || !selection.mySelectedDirectories.isEmpty())
                      && selection.mySelectedExcludeRoots.isEmpty() && isEnabled(selection, module);
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  protected abstract boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module);

  protected static RootsSelection getSelection(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    Module module = getModule(e, files);
    if (module == null) return RootsSelection.EMPTY;

    RootsSelection selection = new RootsSelection(module);
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();
    for (VirtualFile file : files) {
      if (!file.isDirectory()) {
        return RootsSelection.EMPTY;
      }
      ExcludeFolder excludeFolder = ProjectRootsUtil.findExcludeFolder(module, file);
      if (excludeFolder != null) {
        selection.mySelectedExcludeRoots.add(excludeFolder);
        continue;
      }
      SourceFolder folder = ProjectRootsUtil.findSourceFolder(module, file);
      if (folder != null) {
        selection.mySelectedRoots.add(folder);
      }
      else {
        selection.mySelectedDirectories.add(file);
        if (fileIndex.isInSourceContent(file)) {
          selection.myHaveSelectedFilesUnderSourceRoots = true;
        }
      }
    }
    return selection;
  }

  @Nullable
  static Module getModule(@NotNull AnActionEvent e, @Nullable VirtualFile[] files) {
    if (files == null) return null;
    Module module = e.getData(LangDataKeys.MODULE);
    if (module == null) {
      module = findParentModule(e.getProject(), files);
    }
    return module;
  }

  @Nullable
  private static Module findParentModule(@Nullable Project project, @NotNull VirtualFile[] files) {
    if (project == null) return null;
    Module result = null;
    DirectoryIndex index = DirectoryIndex.getInstance(project);
    for (VirtualFile file : files) {
      Module module = index.getInfoForFile(file).getModule();
      if (module == null) return null;
      if (result == null) {
        result = module;
      }
      else if (!result.equals(module)) {
        return null;
      }
    }
    return result;
  }

  public static class RootsSelection {
    public static final RootsSelection EMPTY = new RootsSelection(null);
    public final Module myModule;

    public RootsSelection(Module module) {
      myModule = module;
    }

    public List<SourceFolder> mySelectedRoots = new ArrayList<>();
    public List<ExcludeFolder> mySelectedExcludeRoots = new ArrayList<>();
    public List<VirtualFile> mySelectedDirectories = new ArrayList<>();
    public boolean myHaveSelectedFilesUnderSourceRoots;
  }
}
