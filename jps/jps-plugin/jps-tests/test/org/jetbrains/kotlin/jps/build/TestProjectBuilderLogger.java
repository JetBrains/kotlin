/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.jps.build;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.containers.FileCollectionFactory;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.jps.builders.impl.logging.ProjectBuilderLoggerBase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class TestProjectBuilderLogger extends ProjectBuilderLoggerBase {
  private final MultiMap<String, File> myCompiledFiles = new MultiMap<>();
  private final Set<File> myDeletedFiles = FileCollectionFactory.createCanonicalFileSet();
  private final List<String> myLogLines = new ArrayList<>();

  @Override
  public void logDeletedFiles(Collection<String> paths) {
    super.logDeletedFiles(paths);
    for (String path : paths) {
      myDeletedFiles.add(new File(path));
    }
  }

  @Override
  public void logCompiledFiles(Collection<File> files, String builderId, String description) throws IOException {
    super.logCompiledFiles(files, builderId, description);
    myCompiledFiles.putValues(builderId, files);
  }

  public void clearFilesData() {
    myCompiledFiles.clear();
    myDeletedFiles.clear();
  }

  public void clearLog() {
    myLogLines.clear();
  }

  public void assertCompiled(String builderName, File[] baseDirs, String... paths) {
    assertRelativePaths(baseDirs, myCompiledFiles.get(builderName), paths);
  }

  public void assertDeleted(File[] baseDirs, String... paths) {
    assertRelativePaths(baseDirs, myDeletedFiles, paths);
  }

  private static void assertRelativePaths(File[] baseDirs, Collection<File> files, String[] expected) {
    List<String> relativePaths = new ArrayList<>();
    for (File file : files) {
      String path = file.getAbsolutePath();
      for (File baseDir : baseDirs) {
        if (baseDir != null && FileUtil.isAncestor(baseDir, file, false)) {
          path = FileUtil.getRelativePath(baseDir, file);
          break;
        }
      }
      relativePaths.add(FileUtil.toSystemIndependentName(path));
    }
    UsefulTestCase.assertSameElements(relativePaths, expected);
  }

  @Override
  protected void logLine(String message) {
    myLogLines.add(message);
  }

  public String getFullLog(final File... baseDirs) {
    return StringUtil.join(myLogLines, s -> {
      for (File dir : baseDirs) {
        if (dir != null) {
          String path = FileUtil.toSystemIndependentName(dir.getAbsolutePath()) + "/";
          if (s.startsWith(path)) {
            return s.substring(path.length());
          }
        }
      }
      return s;
    }, "\n");
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
