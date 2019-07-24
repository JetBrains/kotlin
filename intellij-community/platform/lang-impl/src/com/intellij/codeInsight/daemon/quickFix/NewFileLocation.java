// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Describes possible locations for new file / directory that can be created by a quick fix.
 *
 * @see CreateFilePathFix
 * @see CreateDirectoryPathFix
 */
public class NewFileLocation {
  private final List<TargetDirectory> myDirectories;
  private final String[] mySubPath;
  private final String myNewFileName;

  public NewFileLocation(List<TargetDirectory> directories, String newFileName) {
    this(directories, ArrayUtil.EMPTY_STRING_ARRAY, newFileName);
  }

  public NewFileLocation(@NotNull List<TargetDirectory> targetDirectories,
                         @NotNull String[] subPath,
                         @NotNull String newFileName) {
    myDirectories = targetDirectories;
    mySubPath = subPath;
    myNewFileName = newFileName;
  }

  /**
   * @return target directories where sub path and new file can be created
   */
  @NotNull
  public List<TargetDirectory> getDirectories() {
    return myDirectories;
  }

  /**
   * @return intermediate path to new file that may not exist but should be created when a quick fix applied
   */
  @NotNull
  public String[] getSubPath() {
    return mySubPath;
  }

  /**
   * @return new file name
   */
  @NotNull
  public String getNewFileName() {
    return myNewFileName;
  }
}
