// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.actions;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.Locale;

/**
 * @author nik
 */
public class MarkSourceRootAction extends MarkRootActionBase {
  private static final Logger LOG = Logger.getInstance(MarkSourceRootAction.class);
  private final JpsModuleSourceRootType<?> myRootType;

  public MarkSourceRootAction(@NotNull JpsModuleSourceRootType<?> type) {
    myRootType = type;
    Presentation presentation = getTemplatePresentation();
    ModuleSourceRootEditHandler<?> editHandler = ModuleSourceRootEditHandler.getEditHandler(type);
    LOG.assertTrue(editHandler != null);
    presentation.setIcon(editHandler.getRootIcon());
    presentation.setText(editHandler.getFullRootTypeName());
    presentation.setDescription(ProjectBundle.message("module.toggle.sources.action.description",
                                                      editHandler.getFullRootTypeName().toLowerCase(Locale.getDefault())));
  }

  @Override
  protected void modifyRoots(@NotNull VirtualFile vFile, @NotNull ContentEntry entry) {
    entry.addSourceFolder(vFile, myRootType);
  }

  @Override
  protected boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module) {
    final ModuleType moduleType = ModuleType.get(module);
    if (!moduleType.isSupportedRootType(myRootType) || ModuleSourceRootEditHandler.getEditHandler(myRootType) == null
        || (selection.myHaveSelectedFilesUnderSourceRoots && !moduleType.isMarkInnerSupportedFor(myRootType))) {
      return false;
    }

    if (!selection.mySelectedDirectories.isEmpty()) {
      return true;
    }

    for (SourceFolder root : selection.mySelectedRoots) {
      if (!myRootType.equals(root.getRootType())) {
        return true;
      }
    }
    return false;
  }
}
