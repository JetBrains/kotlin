// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class JavaHomeFinderMac extends JavaHomeFinderSimple {
  public static final String JAVA_HOME_FIND_UTIL = "/usr/libexec/java_home";

  JavaHomeFinderMac(boolean forceEmbeddedJava) {
    super(forceEmbeddedJava, "/Library/Java/JavaVirtualMachines", "/System/Library/Java/JavaVirtualMachines");
  }

  @NotNull
  @Override
  public List<String> findExistingJdks() {
    List<String> list = super.findExistingJdks();
    String defaultJavaHome = getSystemDefaultJavaHome();
    if (defaultJavaHome != null) {
      ArrayList<String> list2 = new ArrayList<>(list.size() + 1);
      list2.add(defaultJavaHome);
      list2.addAll(list);
      list = list2;
    }
    return list;
  }

  @Nullable
  private static String getSystemDefaultJavaHome() {
    String homePath = null;
    if (SystemInfo.isMacOSLeopard) {
      // since version 10.5
      if (canExecute(JAVA_HOME_FIND_UTIL)) homePath = ExecUtil.execAndReadLine(new GeneralCommandLine(JAVA_HOME_FIND_UTIL));
    }
    else {
      // before version 10.5
      homePath = "/Library/Java/Home";
    }

    return isDirectory(homePath) ? homePath : null;
  }

  private static boolean canExecute(@Nullable String filePath) {
    if (filePath == null) return false;
    File file = new File(filePath);
    return file.canExecute();
  }

  private static boolean isDirectory(@Nullable String path) {
    if (path == null) return false;
    File dir = new File(path);
    return dir.isDirectory();
  }

  @NotNull
  @Override
  protected List<File> adjustPath(@NotNull File file) {
    return Arrays.asList(file, new File(file, "/Home"), new File(file, "Contents/Home"));
  }
}
