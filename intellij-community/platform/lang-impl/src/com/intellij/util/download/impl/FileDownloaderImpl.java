// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.download.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.FileDownloader;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.net.IOExceptionDialog;
import com.intellij.util.progress.ConcurrentTasksProgressManager;
import com.intellij.util.progress.SubTaskProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

class FileDownloaderImpl implements FileDownloader {
  private static final Logger LOG = Logger.getInstance(FileDownloaderImpl.class);
  private static final String LIB_SCHEMA = "lib://";

  private final List<? extends DownloadableFileDescription> myFileDescriptions;
  private final JComponent myParentComponent;
  @Nullable private final Project myProject;
  private String myDirectoryForDownloadedFilesPath;
  private final String myDialogTitle;

  FileDownloaderImpl(@NotNull List<? extends DownloadableFileDescription> fileDescriptions,
                            @Nullable Project project,
                            @Nullable JComponent parentComponent,
                            @NotNull String presentableDownloadName) {
    myProject = project;
    myFileDescriptions = fileDescriptions;
    myParentComponent = parentComponent;
    myDialogTitle = IdeBundle.message("progress.download.0.title", StringUtil.capitalize(presentableDownloadName));
  }

  @Nullable
  @Override
  public List<VirtualFile> downloadFilesWithProgress(@Nullable String targetDirectoryPath,
                                                     @Nullable Project project,
                                                     @Nullable JComponent parentComponent) {
    final List<Pair<VirtualFile, DownloadableFileDescription>> pairs = downloadWithProgress(targetDirectoryPath, project, parentComponent);
    if (pairs == null) return null;

    List<VirtualFile> files = new ArrayList<>();
    for (Pair<VirtualFile, DownloadableFileDescription> pair : pairs) {
      files.add(pair.getFirst());
    }
    return files;
  }

  @Nullable
  @Override
  public List<Pair<VirtualFile, DownloadableFileDescription>> downloadWithProgress(@Nullable String targetDirectoryPath,
                                                                                   @Nullable Project project,
                                                                                   @Nullable JComponent parentComponent) {
    File dir;
    if (targetDirectoryPath != null) {
      dir = new File(targetDirectoryPath);
    }
    else {
      VirtualFile virtualDir = chooseDirectoryForFiles(project, parentComponent);
      if (virtualDir != null) {
        dir = VfsUtilCore.virtualToIoFile(virtualDir);
      }
      else {
        return null;
      }
    }

    return downloadWithProcess(dir, project, parentComponent);
  }

  @Nullable
  @Override
  public CompletableFuture<List<Pair<VirtualFile, DownloadableFileDescription>>> downloadWithBackgroundProgress(@Nullable String targetDirectoryPath,
                                                                                                               @Nullable Project project) {
    File dir;
    if (targetDirectoryPath != null) {
      dir = new File(targetDirectoryPath);
    }
    else {
      VirtualFile virtualDir = chooseDirectoryForFiles(project, null);
      if (virtualDir != null) {
        dir = VfsUtilCore.virtualToIoFile(virtualDir);
      }
      else {
        return null;
      }
    }

    return downloadWithBackgroundProcess(dir, project);
  }

  @Nullable
  private List<Pair<VirtualFile,DownloadableFileDescription>> downloadWithProcess(final File targetDir,
                                                                                  Project project,
                                                                                  JComponent parentComponent) {
    final Ref<List<Pair<File, DownloadableFileDescription>>> localFiles = Ref.create(null);
    final Ref<IOException> exceptionRef = Ref.create(null);

    boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        localFiles.set(download(targetDir));
      }
      catch (IOException e) {
        exceptionRef.set(e);
      }
    }, myDialogTitle, true, project, parentComponent);
    if (!completed) {
      return null;
    }

    Exception exception = exceptionRef.get();
    if (exception != null) {
      final boolean tryAgain = IOExceptionDialog.showErrorDialog(myDialogTitle, exception.getMessage());
      if (tryAgain) {
        return downloadWithProcess(targetDir, project, parentComponent);
      }
      return null;
    }

    return findVirtualFiles(localFiles.get());
  }

  private @NotNull CompletableFuture<@Nullable List<Pair<VirtualFile,DownloadableFileDescription>>> downloadWithBackgroundProcess(final File targetDir,
                                                                                  Project project) {
    final Ref<List<Pair<File, DownloadableFileDescription>>> localFiles = Ref.create(null);
    final Ref<IOException> exceptionRef = Ref.create(null);

    CompletableFuture<List<Pair<VirtualFile, DownloadableFileDescription>>> result = new CompletableFuture<>();

    ProgressManager.getInstance().run(new Task.Backgroundable(project, myDialogTitle, true) {
      @Override
      public boolean shouldStartInBackground() {
        return true;
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          localFiles.set(download(targetDir));
        }
        catch (IOException exception) {
          final boolean tryAgain = IOExceptionDialog.showErrorDialog(myDialogTitle, exception.getMessage());
          if (tryAgain) {
            downloadWithBackgroundProcess(targetDir, project).thenAccept(pairs -> result.complete(pairs));
          }
          result.complete(null);
        }
      }

      @Override
      public void onSuccess() {
        result.complete(findVirtualFiles(localFiles.get()));
      }

      @Override
      public void onCancel() {
        result.complete(null);
      }
    });

    return result;
  }

  @NotNull
  @Override
  public List<Pair<File, DownloadableFileDescription>> download(@NotNull final File targetDir) throws IOException {
    List<Pair<File, DownloadableFileDescription>> downloadedFiles = Collections.synchronizedList(new ArrayList<>());
    List<Pair<File, DownloadableFileDescription>> existingFiles = Collections.synchronizedList(new ArrayList<>());
    ProgressIndicator parentIndicator = ProgressManager.getInstance().getProgressIndicator();
    if (parentIndicator == null) {
      parentIndicator = new EmptyProgressIndicator();
    }

    try {
      final ConcurrentTasksProgressManager progressManager = new ConcurrentTasksProgressManager(parentIndicator, myFileDescriptions.size());
      parentIndicator.setText(IdeBundle.message("progress.downloading.0.files.text", myFileDescriptions.size()));
      int maxParallelDownloads = Runtime.getRuntime().availableProcessors();
      LOG.debug("Downloading " + myFileDescriptions.size() + " files using " + maxParallelDownloads + " threads");
      long start = System.currentTimeMillis();
      ExecutorService executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("FileDownloaderImpl Pool", maxParallelDownloads);
      List<Future<Void>> results = new ArrayList<>();
      final AtomicLong totalSize = new AtomicLong();
      for (final DownloadableFileDescription description : myFileDescriptions) {
        results.add(executor.submit(() -> {
          SubTaskProgressIndicator indicator = progressManager.createSubTaskIndicator(1);
          indicator.checkCanceled();

          final File existing = new File(targetDir, description.getDefaultFileName());
          final String url = description.getDownloadUrl();
          if (url.startsWith(LIB_SCHEMA)) {
            final String path = FileUtil.toSystemDependentName(StringUtil.trimStart(url, LIB_SCHEMA));
            final File file = PathManager.findFileInLibDirectory(path);
            existingFiles.add(Pair.create(file, description));
          }
          else if (url.startsWith(LocalFileSystem.PROTOCOL_PREFIX)) {
            String path = FileUtil.toSystemDependentName(StringUtil.trimStart(url, LocalFileSystem.PROTOCOL_PREFIX));
            File file = new File(path);
            if (file.exists()) {
              existingFiles.add(Pair.create(file, description));
            }
          }
          else {
            File downloaded;
            try {
              downloaded = downloadFile(description, existing, indicator);
            }
            catch (IOException e) {
              throw new IOException(IdeBundle.message("error.file.download.failed", description.getDownloadUrl(),
                                                      e.getMessage()), e);
            }
            if (FileUtil.filesEqual(downloaded, existing)) {
              existingFiles.add(Pair.create(existing, description));
            }
            else {
              totalSize.addAndGet(downloaded.length());
              downloadedFiles.add(Pair.create(downloaded, description));
            }
          }
          indicator.finished();
          return null;
        }));
      }

      for (Future<Void> result : results) {
        try {
          result.get();
        }
        catch (InterruptedException e) {
          throw new ProcessCanceledException();
        }
        catch (ExecutionException e) {
          if (e.getCause() instanceof IOException) {
            throw ((IOException)e.getCause());
          }
          if (e.getCause() instanceof ProcessCanceledException) {
            throw ((ProcessCanceledException)e.getCause());
          }
          LOG.error(e);
        }
      }
      long duration = System.currentTimeMillis() - start;
      LOG.debug("Downloaded " + StringUtil.formatFileSize(totalSize.get()) + " in " + StringUtil.formatDuration(duration) + "(" + duration + "ms)");

      List<Pair<File, DownloadableFileDescription>> localFiles = new ArrayList<>();
      localFiles.addAll(moveToDir(downloadedFiles, targetDir));
      localFiles.addAll(existingFiles);
      return localFiles;
    }
    catch (ProcessCanceledException | IOException e) {
      deleteFiles(downloadedFiles);
      throw e;
    }
  }

  @Nullable
  private static VirtualFile chooseDirectoryForFiles(Project project, JComponent parentComponent) {
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
      .withTitle(IdeBundle.message("dialog.directory.for.downloaded.files.title"))
      .withDescription(IdeBundle.message("dialog.directory.for.downloaded.files.description"));
    VirtualFile baseDir = project != null ? project.getBaseDir() : null;
    return FileChooser.chooseFile(descriptor, parentComponent, project, baseDir);
  }

  private static List<Pair<File, DownloadableFileDescription>> moveToDir(List<Pair<File, DownloadableFileDescription>> downloadedFiles,
                                                                         final File targetDir) throws IOException {
    FileUtil.createDirectory(targetDir);
    List<Pair<File, DownloadableFileDescription>> result = new ArrayList<>();
    for (Pair<File, DownloadableFileDescription> pair : downloadedFiles) {
      final DownloadableFileDescription description = pair.getSecond();
      final String fileName = description.generateFileName(s -> !new File(targetDir, s).exists());
      final File toFile = new File(targetDir, fileName);
      FileUtil.rename(pair.getFirst(), toFile);
      result.add(Pair.create(toFile, description));
    }
    return result;
  }

  @NotNull
  private static List<Pair<VirtualFile, DownloadableFileDescription>> findVirtualFiles(List<Pair<File, DownloadableFileDescription>> ioFiles) {
    List<Pair<VirtualFile,DownloadableFileDescription>> result = new ArrayList<>();
    for (final Pair<File, DownloadableFileDescription> pair : ioFiles) {
      final File ioFile = pair.getFirst();
      VirtualFile libraryRootFile = WriteAction.computeAndWait(() -> {
        final String url = VfsUtil.getUrlForLibraryRoot(ioFile);
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile);
        return VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
      });
      if (libraryRootFile != null) {
        result.add(Pair.create(libraryRootFile, pair.getSecond()));
      }
    }
    return result;
  }

  private static void deleteFiles(final List<Pair<File, DownloadableFileDescription>> pairs) {
    for (Pair<File, DownloadableFileDescription> pair : pairs) {
      FileUtil.delete(pair.getFirst());
    }
  }

  @NotNull
  private static File downloadFile(@NotNull final DownloadableFileDescription description,
                                   @NotNull final File existingFile,
                                   @NotNull final ProgressIndicator indicator) throws IOException {
    final String presentableUrl = description.getPresentableDownloadUrl();
    indicator.setText(IdeBundle.message("progress.connecting.to.download.file.text", presentableUrl));

    return HttpRequests.request(description.getDownloadUrl()).connect(new HttpRequests.RequestProcessor<File>() {
      @Override
      public File process(@NotNull HttpRequests.Request request) throws IOException {
        int size = request.getConnection().getContentLength();
        if (existingFile.exists() && size == existingFile.length()) {
          return existingFile;
        }

        indicator.setText(IdeBundle.message("progress.download.file.text", description.getPresentableFileName(), presentableUrl));
        return request.saveToFile(FileUtil.createTempFile("download.", ".tmp"), indicator);
      }
    });
  }

  @NotNull
  @Override
  public FileDownloader toDirectory(@NotNull String directoryForDownloadedFilesPath) {
    myDirectoryForDownloadedFilesPath = directoryForDownloadedFilesPath;
    return this;
  }

  @Override
  public VirtualFile @Nullable [] download() {
    List<VirtualFile> files = downloadFilesWithProgress(myDirectoryForDownloadedFilesPath, myProject, myParentComponent);
    return files != null ? VfsUtilCore.toVirtualFileArray(files) : null;
  }

  @Nullable
  @Override
  public List<Pair<VirtualFile, DownloadableFileDescription>> downloadAndReturnWithDescriptions() {
    return downloadWithProgress(myDirectoryForDownloadedFilesPath, myProject, myParentComponent);
  }
}
