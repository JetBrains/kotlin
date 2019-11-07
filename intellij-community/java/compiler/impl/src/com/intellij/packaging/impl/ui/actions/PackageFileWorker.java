/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.packaging.impl.ui.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.deployment.DeploymentUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.ArtifactRootElement;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.artifacts.PackagingElementPath;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.util.PathUtil;
import com.intellij.util.io.zip.JBZipEntry;
import com.intellij.util.io.zip.JBZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class PackageFileWorker {
  private static final Logger LOG = Logger.getInstance(PackageFileWorker.class);
  private final File myFile;
  private final String myRelativeOutputPath;
  private final boolean myPackIntoArchives;

  private PackageFileWorker(File file, String relativeOutputPath, boolean packIntoArchives) {
    myFile = file;
    myRelativeOutputPath = relativeOutputPath;
    myPackIntoArchives = packIntoArchives;
  }

  public static void startPackagingFiles(Project project, List<? extends VirtualFile> files, Artifact[] artifacts, final @NotNull Runnable onFinishedInAwt) {
    startPackagingFiles(project, files, artifacts, true).doWhenProcessed(
      () -> ApplicationManager.getApplication().invokeLater(onFinishedInAwt));
  }

  public static ActionCallback startPackagingFiles(final Project project, final List<? extends VirtualFile> files,
                                                   final Artifact[] artifacts, final boolean packIntoArchives) {
    final ActionCallback callback = new ActionCallback();
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Packaging Files") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          for (final VirtualFile file : files) {
            indicator.checkCanceled();
            ReadAction.run(() -> {
              try {
                packageFile(file, project, artifacts, packIntoArchives);
              }
              catch (IOException e) {
                LOG.info(e);
                String message = CompilerBundle.message("message.tect.package.file.io.error", e.toString());
                Notifications.Bus.notify(new Notification("Package File", "Cannot package file", message, NotificationType.ERROR));
              }
            });
            callback.setDone();
          }
        }
        finally {
          if (!callback.isDone()) {
            callback.setRejected();
          }
        }
      }
    });
    return callback;
  }

  public static void packageFile(@NotNull VirtualFile file, @NotNull Project project, final Artifact[] artifacts,
                                 final boolean packIntoArchives) throws IOException {
    LOG.debug("Start packaging file: " + file.getPath());
    final Collection<Trinity<Artifact, PackagingElementPath, String>> items = ArtifactUtil.findContainingArtifactsWithOutputPaths(file, project, artifacts);
    File ioFile = VfsUtilCore.virtualToIoFile(file);
    for (Trinity<Artifact, PackagingElementPath, String> item : items) {
      final Artifact artifact = item.getFirst();
      final String outputPath = artifact.getOutputPath();
      if (!StringUtil.isEmpty(outputPath)) {
        PackageFileWorker worker = new PackageFileWorker(ioFile, item.getThird(), packIntoArchives);
        LOG.debug(" package to " + outputPath);
        worker.packageFile(outputPath, item.getSecond().getParents());
      }
    }
  }

  private void packageFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
    List<CompositePackagingElement<?>> parentsList = new ArrayList<>(parents);
    Collections.reverse(parentsList);
    if (!parentsList.isEmpty() && parentsList.get(0) instanceof ArtifactRootElement) {
      parentsList = parentsList.subList(1, parentsList.size());
    }
    copyFile(outputPath, parentsList);
  }

  private void copyFile(String outputPath, List<CompositePackagingElement<?>> parents) throws IOException {
    if (parents.isEmpty()) {
      final String fullOutputPath = DeploymentUtil.appendToPath(outputPath, myRelativeOutputPath);
      File target = new File(fullOutputPath);
      if (FileUtil.filesEqual(myFile, target)) {
        LOG.debug("  skipping copying file to itself");
      }
      else {
        LOG.debug("  copying to " + fullOutputPath);
        FileUtil.copy(myFile, target);
      }
      return;
    }

    final CompositePackagingElement<?> element = parents.get(0);
    final String nextOutputPath = outputPath + "/" + element.getName();
    final List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
    if (element instanceof ArchivePackagingElement) {
      if (myPackIntoArchives) {
        packFile(nextOutputPath, "", parentsTrail);
      }
    }
    else {
      copyFile(nextOutputPath, parentsTrail);
    }
  }

  private void packFile(String archivePath, String pathInArchive, List<CompositePackagingElement<?>> parents) throws IOException {
    final File archiveFile = new File(archivePath);
    if (parents.isEmpty()) {
      LOG.debug("  adding to archive " + archivePath);
      try (JBZipFile file = getOrCreateZipFile(archiveFile)) {
        final String fullPathInArchive =
          DeploymentUtil.trimForwardSlashes(DeploymentUtil.appendToPath(pathInArchive, myRelativeOutputPath));
        final JBZipEntry entry = file.getOrCreateEntry(fullPathInArchive);
        entry.setDataFromFile(myFile);
      }
      return;
    }

    final CompositePackagingElement<?> element = parents.get(0);
    final String nextPathInArchive = DeploymentUtil.trimForwardSlashes(DeploymentUtil.appendToPath(pathInArchive, element.getName()));
    final List<CompositePackagingElement<?>> parentsTrail = parents.subList(1, parents.size());
    if (element instanceof ArchivePackagingElement) {
      try (JBZipFile zipFile = getOrCreateZipFile(archiveFile)) {
        final JBZipEntry entry = zipFile.getOrCreateEntry(nextPathInArchive);
        LOG.debug("  extracting to temp file: " + nextPathInArchive + " from " + archivePath);
        final File tempFile = FileUtil.createTempFile("packageFile" + FileUtil.sanitizeFileName(nextPathInArchive),
                                                      FileUtilRt.getExtension(PathUtil.getFileName(nextPathInArchive)));
        if (entry.getSize() != -1) {
          try (OutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            entry.writeDataTo(output);
          }
        }
        packFile(FileUtil.toSystemIndependentName(tempFile.getAbsolutePath()), "", parentsTrail);
        entry.setDataFromFile(tempFile);
        FileUtil.delete(tempFile);
      }
    }
    else {
      packFile(archivePath, nextPathInArchive, parentsTrail);
    }
  }

  private static JBZipFile getOrCreateZipFile(File archiveFile) throws IOException {
    FileUtil.createIfDoesntExist(archiveFile);
    try {
      return new JBZipFile(archiveFile);
    }
    catch (IllegalArgumentException e) {
      throw new IOException(e);
    }
  }
}
