/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VFileProperty;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class LoadAllContentsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance("com.intellij.internal.LoadAllContentsAction");

  private final AtomicInteger count = new AtomicInteger();
  private final AtomicLong totalSize = new AtomicLong();

  public LoadAllContentsAction() {
    super("Load all files content", "Measure FileUtil.loadFile() for all files in the project", null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    String m = "Started loading content";
    LOG.info(m);
    System.out.println(m);
    long start = System.currentTimeMillis();

    count.set(0);
    totalSize.set(0);
    ApplicationManagerEx.getApplicationEx().runProcessWithProgressSynchronously(
      () -> ProjectRootManager.getInstance(project).getFileIndex().iterateContent(fileOrDir -> {
        if (fileOrDir.isDirectory() || fileOrDir.is(VFileProperty.SPECIAL)) {
          return true;
        }
        try {
          count.incrementAndGet();
          byte[] bytes = FileUtil.loadFileBytes(new File(fileOrDir.getPath()));
          totalSize.addAndGet(bytes.length);
          ProgressManager.getInstance().getProgressIndicator().setText(fileOrDir.getPresentableUrl());
        }
        catch (IOException e1) {
          LOG.error(e1);
        }
        return true;
      }), "Loading", false, project);

    long end = System.currentTimeMillis();
    String message = "Finished loading content of " + count + " files. " +
                     "Total size=" + StringUtil.formatFileSize(totalSize.get()) + ". " +
                     "Elapsed=" + ((end - start) / 1000) + "sec.";
    LOG.info(message);
    System.out.println(message);
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}