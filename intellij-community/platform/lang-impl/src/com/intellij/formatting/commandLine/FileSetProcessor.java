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
package com.intellij.formatting.commandLine;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public abstract class FileSetProcessor {
  private static final Logger LOG = Logger.getInstance(FileSetProcessor.class);

  private final Set<File> myTopEntries = ContainerUtil.newHashSet();
  private final Set<String> myFileMasks = ContainerUtil.newHashSet();
  private int myProcessedFiles;
  private boolean isRecursive;

  public void processFiles() throws IOException {
    for (File topEntry : myTopEntries) {
      processEntry(topEntry);
    }
  }

  public void setRecursive() {
    isRecursive = true;
  }

  public void addFileMask(@NotNull String fileMask) {
    String fileMaskRegexp = fileMaskToRegexp(fileMask);
    LOG.info("File mask regexp: " + fileMaskRegexp);
    myFileMasks.add(fileMaskRegexp);
  }

  private static String fileMaskToRegexp(@NotNull String fileMask) {
    return
      fileMask
        .replace(".", "\\.")
        .replace("*", ".*")
        .replace("?", ".")
        .replace("+", "\\+");
  }

  public void addEntry(@NotNull String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
      throw new IOException("File " + filePath + " not found.");
    }
    myTopEntries.add(file);
  }

  private void processEntry(@NotNull File entry) throws IOException {
    if (entry.exists()) {
      if (entry.isDirectory()) {
        LOG.info("Scanning directory " + entry.getPath());
        File[] subEntries = entry.listFiles();
        if (subEntries != null) {
          for (File subEntry : subEntries) {
            if (!subEntry.isDirectory() || isRecursive) {
              processEntry(subEntry);
            }
          }
        }
      }
      else {
        if (matchesFileMask(entry.getName())) {
          VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(entry);
          if (virtualFile == null) {
            throw new IOException("Can not find " + entry.getPath());
          }
          LOG.info("Processing " + virtualFile.getPath());
          if (processFile(virtualFile)) myProcessedFiles++;
        }
      }
    }
  }

  private boolean matchesFileMask(@NotNull String name) {
    if (myFileMasks.isEmpty()) return true;
    for (String fileMask : myFileMasks) {
      if (name.matches(fileMask)) {
        return true;
      }
    }
    return false;
  }

  protected abstract boolean processFile(@NotNull VirtualFile virtualFile);

  public int getProcessedFiles() {
    return myProcessedFiles;
  }
}
