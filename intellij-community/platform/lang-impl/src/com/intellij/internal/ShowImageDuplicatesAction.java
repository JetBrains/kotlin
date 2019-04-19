// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.io.DigestUtil;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class ShowImageDuplicatesAction extends AnAction {
  //FileTypeManager.getInstance().getFileTypeByExtension("png").getAllPossibleExtensions() ?
  private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("png", "jpg", "jpeg", "gif", "tiff", "bmp");

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getEventProject(e);
    assert project != null;
    ProgressManager.getInstance()
      .runProcessWithProgressSynchronously(() -> collectAndShowDuplicates(project), "Gathering Images", true, project);
  }

  private static void collectAndShowDuplicates(final Project project) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null && !indicator.isCanceled()) {
      indicator.setText("Collecting project images...");
      indicator.setIndeterminate(false);
      final List<VirtualFile> images = new ArrayList<>();
      for (String ext : IMAGE_EXTENSIONS) {
        images.addAll(ReadAction.compute(() -> FilenameIndex.getAllFilesByExt(project, ext)));
      }

      final Map<Long, Set<VirtualFile>> duplicates = new HashMap<>();
      final Map<Long, VirtualFile> all = new HashMap<>();
      for (int i = 0; i < images.size(); i++) {
        indicator.setFraction((double)(i + 1) / (double)images.size());
        final VirtualFile file = images.get(i);
        ReadAction.run(() -> {
          if (!(file.getFileSystem() instanceof LocalFileSystem)) return;
          final long length = file.getLength();
          if (all.containsKey(length)) {
            if (!duplicates.containsKey(length)) {
              final HashSet<VirtualFile> files = new HashSet<>();
              files.add(all.get(length));
              duplicates.put(length, files);
            }
            duplicates.get(length).add(file);
          }
          else {
            all.put(length, file);
          }
        });
        indicator.checkCanceled();
      }
      showResults(project, images, duplicates);
    }
  }

  private static void showResults(final Project project, final List<VirtualFile> images, Map<Long, Set<VirtualFile>> duplicates) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator == null || indicator.isCanceled()) return;
    indicator.setText("MD5 check");

    int count = duplicates.values().stream().mapToInt(Set::size).sum();
    final Map<String, Set<VirtualFile>> realDuplicates = new HashMap<>();
    int seek = 0;
    for (Set<VirtualFile> files : duplicates.values()) {
      for (VirtualFile file : files) {
        seek++;
        indicator.setFraction((double)seek / (double)count);
        try {
          ReadAction.run(() -> {
            final String md5 = getMD5Checksum(file);
            realDuplicates.computeIfAbsent(md5, k -> new HashSet<>()).add(file);
          });
        }
        catch (Exception ignored) {
        }
      }
    }
    count = 0;
    for (String key : new ArrayList<>(realDuplicates.keySet())) {
      final int size = realDuplicates.get(key).size();
      if (size == 1) {
        realDuplicates.remove(key);
      } else {
        count+=size;
      }
    }

    ApplicationManager.getApplication().invokeLater(() -> new ImageDuplicateResultsDialog(project, images, realDuplicates).show());

  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(getEventProject(e) != null);
  }

  private static byte[] createChecksum(VirtualFile file) throws Exception {
    byte[] buffer = new byte[1024];
    MessageDigest md5 = DigestUtil.md5();
    int read;

    try (InputStream fis = file.getInputStream()) {
      while ((read = fis.read(buffer)) > 0) md5.update(buffer, 0, read);

      return md5.digest();
    }
  }

  private static String getMD5Checksum(VirtualFile fis) throws Exception {
    byte[] bytes = createChecksum(fis);
    StringBuilder md5 = new StringBuilder();

    for (byte b : bytes) {
      md5.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
    }
    return md5.toString();
  }
}
