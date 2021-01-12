// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.javascript.boilerplate;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.platform.templates.github.GeneratorException;
import com.intellij.platform.templates.github.Outcome;
import com.intellij.util.net.IOExceptionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Simonchik
 */
public final class GithubDownloadUtil {
  private static final String PROJECT_GENERATORS = "projectGenerators";

  private GithubDownloadUtil() {}

  @NotNull
  private static String formatGithubRepositoryName(@NotNull String userName, @NotNull String repositoryName) {
    return "github-" + userName + "-" + repositoryName;
  }

  @NotNull
  private static String formatGithubUserName(@NotNull String userName) {
    return "github-" + userName;
  }

  @NotNull
  public static File getCacheDir(@NotNull String userName, @NotNull String repositoryName) {
    File generatorsDir = new File(PathManager.getSystemPath(), PROJECT_GENERATORS);
    String dirName = formatGithubRepositoryName(userName, repositoryName);
    File dir = new File(generatorsDir, dirName);
    try {
      return dir.getCanonicalFile();
    } catch (IOException e) {
      return dir;
    }
  }

  @NotNull
  public static File getUserCacheDir(@NotNull String userName) {
    File generatorsDir = new File(PathManager.getSystemPath(), PROJECT_GENERATORS);
    String dirName = formatGithubUserName(userName);
    File dir = new File(generatorsDir, dirName);
    try {
      return dir.getCanonicalFile();
    } catch (IOException e) {
      return dir;
    }
  }

  public static File findCacheFile(@NotNull String userName, @NotNull String repositoryName, @NotNull String cacheFileName) {
    File dir = getCacheDir(userName, repositoryName);
    return new File(dir, cacheFileName);
  }

  public static void downloadContentToFileWithProgressSynchronously(
    @Nullable Project project,
    @NotNull final String url,
    @NotNull String progressTitle,
    @NotNull final File outputFile,
    @NotNull final String userName,
    @NotNull final String repositoryName,
    final boolean retryOnError) throws GeneratorException
  {
    Outcome<File> outcome = DownloadUtil.provideDataWithProgressSynchronously(
      project,
      progressTitle,
      "Downloading zip archive" + DownloadUtil.CONTENT_LENGTH_TEMPLATE + " ...",
      () -> {
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        downloadAtomically(progress, url, outputFile, userName, repositoryName);
        return outputFile;
      }, () -> {
        if (!retryOnError) {
          return false;
        }
        return IOExceptionDialog.showErrorDialog(LangBundle.message("dialog.title.download.error"),
                                                 LangBundle.message("text.can.not.download", url));
      }
    );
    File out = outcome.get();
    if (out != null) {
      return;
    }
    Exception e = outcome.getException();
    if (e != null) {
      throw new GeneratorException(LangBundle.message("dialog.message.can.fetch.content.from", url), e);
    }
    throw new GeneratorException(LangBundle.message("dialog.message.download.was.cancelled"));
  }

  /**
   * Downloads content of {@code url} to {@code outputFile}.
   * {@code outputFile} won't be modified in case of any I/O download errors.
   *
   * @param indicator   progress indicator
   * @param url         url to download
   * @param outputFile  output file
   */
  public static void downloadAtomically(@Nullable ProgressIndicator indicator,
                                        @NotNull String url,
                                        @NotNull File outputFile,
                                        @NotNull String userName,
                                        @NotNull String repositoryName) throws IOException
  {
    String tempFileName = String.format("github-%s-%s-%s", userName, repositoryName, outputFile.getName());
    File tempFile = FileUtil.createTempFile(tempFileName + "-", ".tmp");
    DownloadUtil.downloadAtomically(indicator, url, outputFile, tempFile);
  }

}
