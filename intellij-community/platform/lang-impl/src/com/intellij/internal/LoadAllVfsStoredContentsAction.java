/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class LoadAllVfsStoredContentsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance("com.intellij.internal.LoadAllContentsAction");

  private final AtomicInteger count = new AtomicInteger();
  private final AtomicLong totalSize = new AtomicLong();

  public LoadAllVfsStoredContentsAction() {
    super("Load all VirtualFiles content", "Measure virtualFile.contentsToByteArray() for all virtual files stored in the VFS", null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    String m = "Started loading content";
    LOG.info(m);
    System.out.println(m);
    long start = System.currentTimeMillis();

    count.set(0);
    totalSize.set(0);
    ApplicationManagerEx.getApplicationEx().runProcessWithProgressSynchronously(() -> {
      PersistentFS vfs = (PersistentFS)application.getComponent(ManagingFS.class);
      VirtualFile[] roots = vfs.getRoots();
      for (VirtualFile root : roots) {
        iterateCached(root);
      }
    }, "Loading", false, null);

    long end = System.currentTimeMillis();
    String message = "Finished loading content of " + count + " files. " +
                     "Total size=" + StringUtil.formatFileSize(totalSize.get()) + ". " +
                     "Elapsed=" + ((end - start) / 1000) + "sec.";
    LOG.info(message);
    System.out.println(message);
  }

  private void iterateCached(VirtualFile root) {
    processFile((NewVirtualFile)root);
    Collection<VirtualFile> children = ((NewVirtualFile)root).getCachedChildren();
    for (VirtualFile child : children) {
      iterateCached(child);
    }
  }

  public boolean processFile(NewVirtualFile file) {
    if (file.isDirectory() || file.is(VFileProperty.SPECIAL)) {
      return true;
    }
    try {
      try (InputStream stream = PersistentFS.getInstance().getInputStream(file)) {
        // check if it's really cached in VFS
        if (!(stream instanceof DataInputStream)) return true;
        byte[] bytes = FileUtil.loadBytes(stream);
        totalSize.addAndGet(bytes.length);
        count.incrementAndGet();
        ProgressManager.getInstance().getProgressIndicator().setText(file.getPresentableUrl());
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return true;
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}