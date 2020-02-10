// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash.building;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.PathKt;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class DumpIndexAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project == null) return;

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    descriptor.withTitle("Select Index Dump Directory");
    VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
    if (file != null) {
      ProgressManager.getInstance().run(new Task.Modal(project, "Exporting Indexes..." , true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          Path outputDirectory = VfsUtilCore.virtualToIoFile(file).toPath();
          PathKt.delete(outputDirectory);
          Path indexZipFile = outputDirectory.resolve("index.zip");

          ProjectIndexesExporter.getInstance(project).exportIndices(indexZipFile, indicator);
        }
      });
    }
  }
}
