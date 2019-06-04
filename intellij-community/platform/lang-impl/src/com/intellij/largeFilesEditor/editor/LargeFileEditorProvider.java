// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.impl.DefaultPlatformFileEditorProvider;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jetbrains.annotations.NotNull;

public class LargeFileEditorProvider implements DefaultPlatformFileEditorProvider, DumbAware {

  public static final String PROVIDER_ID = "LargeFileEditorProvider";

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return Experiments.isFeatureEnabled("new.large.text.file.viewer")
           && TextEditorProvider.isTextFile(file)
           && !file.getFileType().isBinary()
           && SingleRootFileViewProvider.isTooLargeForContentLoading(file);
  }

  @NotNull
  @Override
  public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new EditorManagerImpl(project, file);
  }

  @NotNull
  @Override
  public String getEditorTypeId() {
    return PROVIDER_ID;
  }

  @NotNull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.NONE;
  }
}
