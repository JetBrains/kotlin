// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public final class ProjectConversionUtil {
  @NonNls public static final String PROJECT_FILES_BACKUP = "projectFilesBackup";
  @NonNls private static final String BACKUP_EXTENSION = "backup";

  private ProjectConversionUtil() {
  }

  public static File backupFile(Path file) throws IOException {
    final String fileName = FileUtil.createSequentFileName(file.getParent().toFile(), file.getFileName().toString(), BACKUP_EXTENSION);
    final File backup = file.getParent().resolve(fileName).toFile();
    FileUtil.copy(file.toFile(), backup);
    return backup;
  }

  @NotNull
  public static File backupFiles(final Collection<? extends Path> files, final File parentDir) throws IOException {
    File backupDir = getBackupDir(parentDir);
    backupFiles(files, parentDir, backupDir);
    return backupDir;
  }

  public static void backupFiles(Collection<? extends Path> files, File parentDir, File backupDir) throws IOException {
    backupDir.mkdirs();
    for (Path path : files) {
      File file = path.toFile();
      final File target;
      if (FileUtil.isAncestor(parentDir, file, true)) {
        final String relativePath = FileUtil.getRelativePath(parentDir, file);
        target = new File(backupDir.getAbsolutePath() + File.separator + relativePath);
        FileUtil.createParentDirs(target);
      }
      else {
        target = new File(backupDir, file.getName());
      }
      FileUtil.copy(file, target);
    }
  }

  @NotNull
  public static File getBackupDir(File parentDir) {
    final String dirName = FileUtil.createSequentFileName(parentDir, PROJECT_FILES_BACKUP, "");
    return new File(parentDir, dirName);
  }

}
