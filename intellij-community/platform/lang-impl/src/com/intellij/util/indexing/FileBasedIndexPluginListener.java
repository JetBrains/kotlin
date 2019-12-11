// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NotNull;

class FileBasedIndexPluginListener implements DynamicPluginListener {
  @NotNull
  private final FileBasedIndexImpl myFileBasedIndex;

  FileBasedIndexPluginListener(@NotNull FileBasedIndexImpl index) {myFileBasedIndex = index;}

  @Override
  public void beforePluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    beforePluginSetChanged();
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
    beforePluginSetChanged();
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
    afterPluginSetChanged();
  }

  @Override
  public void pluginUnloaded(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
    afterPluginSetChanged();
  }

  private final Semaphore mySemaphore = new Semaphore();

  private void beforePluginSetChanged() {
    FileBasedIndexImpl.LOG.assertTrue(mySemaphore.isUp());
    mySemaphore.down();

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DumbService.getInstance(project).queueTask(new DumbModeTask() {
        @Override
        public void performInDumbMode(@NotNull ProgressIndicator indicator) {
          indicator.setText(IdeBundle.message("progress.indexing.reload"));
          mySemaphore.waitFor();
        }
      });
    }

    myFileBasedIndex.performShutdown(true);
    myFileBasedIndex.dropRegisteredIndexes();
  }

  private void afterPluginSetChanged() {
    myFileBasedIndex.initComponent();

    FileBasedIndexImpl.LOG.assertTrue(!mySemaphore.isUp());
    mySemaphore.up();
  }
}
