// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.commandLine;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class FileSetProcessor {
  private static final Logger LOG = Logger.getInstance(FileSetProcessor.class);

  private final Set<File> myTopEntries = new HashSet<>();
  private final Set<String> myFileMasks = new HashSet<>();
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
