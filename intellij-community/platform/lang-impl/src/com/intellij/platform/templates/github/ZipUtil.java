// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform.templates.github;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.NullableFunction;
import com.intellij.util.io.Decompressor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author Sergey Simonchik
 */
public final class ZipUtil {

  private static final Logger LOG = Logger.getInstance(ZipUtil.class);

  public interface ContentProcessor {
    /** Return null to skip the file */
    byte @Nullable [] processContent(byte[] content, File file) throws IOException;
  }

  public static void unzipWithProgressSynchronously(
    @Nullable Project project,
    @NotNull String progressTitle,
    @NotNull final File zipArchive,
    @NotNull final File extractToDir,
    final boolean unwrapSingleTopLevelFolder) throws GeneratorException
  {
    unzipWithProgressSynchronously(project, progressTitle, zipArchive, extractToDir, null, unwrapSingleTopLevelFolder);
  }

  public static void unzipWithProgressSynchronously(
    @Nullable Project project,
    @NotNull String progressTitle,
    @NotNull final File zipArchive,
    @NotNull final File extractToDir,
    @Nullable final NullableFunction<? super String, String> pathConvertor,
    final boolean unwrapSingleTopLevelFolder) throws GeneratorException
  {
    final Outcome<Boolean> outcome = DownloadUtil.provideDataWithProgressSynchronously(
      project, progressTitle, "Unpacking ...",
      () -> {
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        unzip(progress, extractToDir, zipArchive, pathConvertor, null, unwrapSingleTopLevelFolder);
        return true;
      },
      () -> false
    );
    Boolean result = outcome.get();
    if (result == null) {
      Exception e = outcome.getException();
      if (e != null) {
        throw new GeneratorException(LangBundle.message("dialog.message.unpacking.failed.downloaded.archive.broken"));
      }
      throw new GeneratorException(LangBundle.message("dialog.message.unpacking.was.cancelled"));
    }
  }

  private static File getUnzipToDir(@Nullable ProgressIndicator progress,
                                    @NotNull File targetDir,
                                    boolean unwrapSingleTopLevelFolder) throws IOException {
    if (progress != null) {
      progress.setText(LangBundle.message("progress.text.extracting"));
    }
    if (unwrapSingleTopLevelFolder) {
      return FileUtil.createTempDirectory("unzip-dir-", null);
    }
    return targetDir;
  }

  // This method will throw IOException, if a zipArchive file isn't a valid zip archive.
  public static void unzip(@Nullable ProgressIndicator progress,
                           @NotNull File targetDir,
                           @NotNull File zipArchive,
                           @Nullable NullableFunction<? super String, String> pathConvertor,
                           @Nullable ContentProcessor contentProcessor,
                           boolean unwrapSingleTopLevelFolder) throws IOException {
    File unzipToDir = getUnzipToDir(progress, targetDir, unwrapSingleTopLevelFolder);
    try (ZipFile zipFile = new ZipFile(zipArchive, ZipFile.OPEN_READ)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        try (InputStream entryContentStream = zipFile.getInputStream(entry)) {
          unzipEntryToDir(progress, entry, entryContentStream, unzipToDir, pathConvertor, contentProcessor);
        }
      }
    }
    doUnwrapSingleTopLevelFolder(unwrapSingleTopLevelFolder, unzipToDir, targetDir);
  }

  public static void unzip(@Nullable ProgressIndicator progress,
                           @NotNull File targetDir,
                           @NotNull ZipInputStream stream,
                           @Nullable NullableFunction<? super String, String> pathConvertor,
                           @Nullable ContentProcessor contentProcessor,
                           boolean unwrapSingleTopLevelFolder) throws IOException {
    File unzipToDir = getUnzipToDir(progress, targetDir, unwrapSingleTopLevelFolder);
    ZipEntry entry;
    while ((entry = stream.getNextEntry()) != null) {
      unzipEntryToDir(progress, entry, stream, unzipToDir,  pathConvertor, contentProcessor);
    }
    doUnwrapSingleTopLevelFolder(unwrapSingleTopLevelFolder, unzipToDir, targetDir);
  }

  private static void doUnwrapSingleTopLevelFolder(boolean unwrapSingleTopLevelFolder,
                                                   @NotNull File unzipToDir,
                                                   @NotNull File targetDir) throws IOException {
    if (unwrapSingleTopLevelFolder) {
      File[] topLevelFiles = unzipToDir.listFiles();
      File dirToMove;
      if (topLevelFiles != null && topLevelFiles.length == 1 && topLevelFiles[0].isDirectory()) {
        dirToMove = topLevelFiles[0];
      }
      else {
        dirToMove = unzipToDir;
      }
      // Don't "FileUtil.moveDirWithContent(dirToMove, targetDir)"
      // because a file moved with "java.io.File.renameTo" won't inherit its new parent's permissions
      FileUtil.copyDirContent(dirToMove, targetDir);
      FileUtil.delete(unzipToDir);
    }
  }

  private static void unzipEntryToDir(@Nullable ProgressIndicator progress,
                                      @NotNull final ZipEntry zipEntry,
                                      @NotNull final InputStream entryContentStream,
                                      @NotNull final File extractToDir,
                                      @Nullable NullableFunction<? super String, String> pathConvertor,
                                      @Nullable ContentProcessor contentProcessor) throws IOException {
    String relativeExtractPath = createRelativeExtractPath(zipEntry);
    if (pathConvertor != null) {
      relativeExtractPath = pathConvertor.fun(relativeExtractPath);
      if (relativeExtractPath == null) {
        // should be skipped
        return;
      }
    }
    File child = Decompressor.entryFile(extractToDir, relativeExtractPath);
    File dir = zipEntry.isDirectory() ? child : child.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Unable to create dir: '" + dir + "'!");
    }
    if (zipEntry.isDirectory()) {
      return;
    }
    if (progress != null) {
      progress.setText(LangBundle.message("progress.text.extracting.path", relativeExtractPath));
    }
    if (contentProcessor == null) {
      try (FileOutputStream fileOutputStream = new FileOutputStream(child)) {
        FileUtil.copy(entryContentStream, fileOutputStream);
      }
    }
    else {
      byte[] content = contentProcessor.processContent(FileUtil.loadBytes(entryContentStream), child);
      if (content != null) {
        FileUtil.writeToFile(child, content);
      }
    }
    LOG.info("Extract: " + relativeExtractPath);
  }

  @NotNull
  private static String createRelativeExtractPath(@NotNull ZipEntry zipEntry) {
    String name = StringUtil.trimStart(zipEntry.getName(), "/");
    return StringUtil.trimEnd(name, "/");
  }
}
