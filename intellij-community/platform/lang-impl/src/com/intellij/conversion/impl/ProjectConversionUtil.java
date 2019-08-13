/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.conversion.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author nik
 */
public class ProjectConversionUtil {
  @NonNls public static final String PROJECT_FILES_BACKUP = "projectFilesBackup";
  @NonNls private static final String BACKUP_EXTENSION = "backup";

  private ProjectConversionUtil() {
  }

  public static File backupFile(File file) throws IOException {
    final String fileName = FileUtil.createSequentFileName(file.getParentFile(), file.getName(), BACKUP_EXTENSION);
    final File backup = new File(file.getParentFile(), fileName);
    FileUtil.copy(file, backup);
    return backup; 
  }

  @NotNull
  public static File backupFiles(final Collection<? extends File> files, final File parentDir) throws IOException {
    File backupDir = getBackupDir(parentDir);
    backupFiles(files, parentDir, backupDir);
    return backupDir;
  }

  public static void backupFiles(Collection<? extends File> files, File parentDir, File backupDir) throws IOException {
    backupDir.mkdirs();
    for (File file : files) {
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
