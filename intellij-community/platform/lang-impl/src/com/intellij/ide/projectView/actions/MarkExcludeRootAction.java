// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.actions;

import com.intellij.icons.AllIcons;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class MarkExcludeRootAction extends MarkRootActionBase {
  public MarkExcludeRootAction() {
    super(null, null, AllIcons.Modules.ExcludeRoot);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

    if (Registry.is("ide.hide.excluded.files")) {
      String message = files.length == 1
                       ? FileUtil.toSystemDependentName(files[0].getPath())
                       : LangBundle.message("dialog.message.selected.files", files.length);
      final int rc = Messages.showOkCancelDialog(e.getData(CommonDataKeys.PROJECT), getPromptText(message),
                                                 LangBundle.message("dialog.title.mark.as.excluded"),
                                                 Messages.getQuestionIcon());
      if (rc != Messages.OK) {
        return;
      }
    }
    super.actionPerformed(e);
  }

  @NlsContexts.DialogMessage
  protected String getPromptText(@NlsContexts.DialogMessage String message) {
    return LangBundle.message("dialog.message.are.you.sure.you.would.like.to.exclude", message);
  }

  @Override
  protected void modifyRoots(@NotNull VirtualFile vFile, @NotNull ContentEntry entry) {
    entry.addExcludeFolder(vFile);
  }

  @Override
  protected boolean isEnabled(@NotNull RootsSelection selection, @NotNull Module module) {
    ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
    return selection.mySelectedDirectories.stream().allMatch(file -> index.isInContent(file));
  }
}
